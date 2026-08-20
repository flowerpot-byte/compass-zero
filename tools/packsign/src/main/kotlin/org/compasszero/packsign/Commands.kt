package org.compasszero.packsign

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import org.compasszero.content.ContentLimits
import org.compasszero.content.LoadResult
import org.compasszero.content.PackParser
import org.compasszero.content.PackReader
import org.compasszero.content.Severity
import org.compasszero.karte.Bilddatei
import org.compasszero.karte.Hoehendatei
import org.compasszero.karte.Hoehenformat
import org.compasszero.karte.Kartendatei
import org.compasszero.karte.Kartenfehler
import org.compasszero.karte.Kartenumschlag
import org.compasszero.karte.Namensdatei
import org.compasszero.karte.Wegenetz
import org.compasszero.karte.Zusatzumschlag
import org.compasszero.security.AssetRead
import org.compasszero.security.Digests
import org.compasszero.security.Ed25519
import org.compasszero.security.Hex
import org.compasszero.security.PackFormat
import org.compasszero.security.PackVerdict
import org.compasszero.security.PackVerifier
import org.compasszero.security.PackWriter
import org.compasszero.security.TrustStore
import org.compasszero.security.TrustedKey

object Commands {

    private val KEY_NAME = Regex("[a-z0-9-]+")

    // Der Aufbau wird im Ganzen geprueft, dafuer muss der Payload in den Speicher
    // passen. Groessere Pakete gibt es im Projekt nicht.
    private const val PAYLOAD_PRUEFGRENZE = 256L * 1024 * 1024

    // Karten sind groesser als Inhaltspakete -- der Europa-Ueberblick liegt bei
    // rund 300 MB. Auch sie werden im Ganzen in den Speicher gelesen, damit
    // geprueft und unterschrieben genau dieselben Bytes sind; das Werkzeug
    // laeuft auf dem Rechner des Maintainers, nicht auf einem Telefon.
    private const val KARTE_PRUEFGRENZE = 1500L * 1024 * 1024

    // Wieviel eine Kante des Wegenetzes von ihren eigenen Angaben abweichen
    // darf. Beide Zahlen sind an der gebauten Salzburg-Datei gemessen
    // (75 970 Knoten, 93 095 Kanten, 18.08.2026): Die Linien fangen auf
    // 0,0 Meter genau an ihren Knoten an, und die groesste Abweichung zwischen
    // angegebener und nachgemessener Laenge liegt bei 0,50 Metern -- das ist
    // das Runden auf ganze Meter beim Bauen. Wer hier grosszuegiger prueft,
    // prueft nichts mehr; wer strenger prueft, weist gebaute Dateien ab.
    private const val ENDPUNKT_SCHLUPF = 0.5
    private const val LAENGEN_SCHLUPF = 1.0

    private val KNOWN_OPTIONS = mapOf(
        "keygen" to setOf("dir", "name"),
        "pack" to setOf("in", "out"),
        "sign" to setOf("key", "in", "out"),
        "verify" to setOf("in", "keys"),
        "karte-signieren" to setOf("key", "in", "out"),
        "karte-pruefen" to setOf("in", "keys"),
        "bild-signieren" to setOf("key", "in", "out"),
        "bild-pruefen" to setOf("in", "keys"),
        "namen-signieren" to setOf("key", "in", "out"),
        "namen-pruefen" to setOf("in", "keys"),
        "wege-signieren" to setOf("key", "in", "out"),
        "wege-pruefen" to setOf("in", "keys"),
        "hoehen-signieren" to setOf("key", "in", "out"),
        "hoehen-pruefen" to setOf("in", "keys"),
    )

    fun run(args: Array<String>, out: Appendable): Int {
        val command = args.firstOrNull() ?: return usage(out)
        val allowed = KNOWN_OPTIONS[command] ?: return usage(out)
        val options = parseOptions(args, allowed, out) ?: return 2
        return when (command) {
            "keygen" -> keygen(options, out)
            "pack" -> pack(options, out)
            "sign" -> sign(options, out)
            "karte-signieren" -> karteSignieren(options, out)
            "karte-pruefen" -> kartePruefen(options, out)
            "bild-signieren" -> zusatzSignieren(BILD, options, out)
            "bild-pruefen" -> zusatzPruefen(BILD, options, out)
            "namen-signieren" -> zusatzSignieren(NAMEN, options, out)
            "namen-pruefen" -> zusatzPruefen(NAMEN, options, out)
            "wege-signieren" -> zusatzSignieren(WEGE, options, out)
            "wege-pruefen" -> zusatzPruefen(WEGE, options, out)
            "hoehen-signieren" -> zusatzSignieren(HOEHEN, options, out)
            "hoehen-pruefen" -> zusatzPruefen(HOEHEN, options, out)
            else -> verify(options, out)
        }
    }

    // Ein Tippfehler in einer Option darf nicht still ignoriert werden: wer sich
    // bei --keys vertippt, prueft sonst gegen einen leeren Vertrauensspeicher.
    private fun parseOptions(args: Array<String>, allowed: Set<String>, out: Appendable): Map<String, String>? {
        val map = HashMap<String, String>()
        var i = 1
        while (i < args.size) {
            val raw = args[i]
            if (!raw.startsWith("--") || i + 1 >= args.size) {
                usage(out)
                return null
            }
            val key = raw.removePrefix("--")
            if (key !in allowed) {
                out.appendLine("Unbekannte Option: --$key")
                usage(out)
                return null
            }
            if (map.put(key, args[i + 1]) != null) {
                out.appendLine("Option doppelt angegeben: --$key")
                return null
            }
            i += 2
        }
        return map
    }

    private fun usage(out: Appendable): Int {
        out.appendLine("packsign — Compass-Zero-Pakete erzeugen, signieren, pruefen")
        out.appendLine()
        out.appendLine("  keygen --dir VERZEICHNIS --name NAME")
        out.appendLine("  pack   --in INHALTSVERZEICHNIS --out DATEI.zip")
        out.appendLine("  sign   --key NAME.secret --in DATEI.zip --out DATEI.czp")
        out.appendLine("  verify --in DATEI.czp [--keys LISTE.txt]   (Zeilen: Name=PublicKeyHex)")
        out.appendLine()
        out.appendLine("  karte-signieren --key NAME.secret --in KARTE.czk --out KARTE-signiert.czk")
        out.appendLine("  karte-pruefen   --in KARTE.czk [--keys LISTE.txt]")
        out.appendLine()
        out.appendLine("  bild-signieren  --key NAME.secret --in BILDER.czb --out BILDER-signiert.czb")
        out.appendLine("  bild-pruefen    --in BILDER.czb [--keys LISTE.txt]")
        out.appendLine("  namen-signieren --key NAME.secret --in NAMEN.czn --out NAMEN-signiert.czn")
        out.appendLine("  namen-pruefen   --in NAMEN.czn [--keys LISTE.txt]")
        out.appendLine("  wege-signieren  --key NAME.secret --in WEGE.czw --out WEGE-signiert.czw")
        out.appendLine("  wege-pruefen    --in WEGE.czw [--keys LISTE.txt]")
        out.appendLine("  hoehen-signieren --key NAME.secret --in HOEHEN.czh --out HOEHEN-signiert.czh")
        out.appendLine("  hoehen-pruefen   --in HOEHEN.czh [--keys LISTE.txt]")
        return 2
    }

    // Eine Karte ist so gross, dass sie nicht mehr ins APK passt -- also reist
    // sie einzeln, und dann braucht sie eine eigene Unterschrift. Der Umschlag
    // ist derselbe wie beim Inhaltspaket, aber mit eigener Kennung: Sonst
    // deckte eine Unterschrift ueber ein Paket eine Karte gleicher Groesse.
    private fun karteSignieren(options: Map<String, String>, out: Appendable): Int {
        val keyFile = options["key"]?.let(::File) ?: return usage(out)
        val karte = options["in"]?.let(::File) ?: return usage(out)
        val ziel = options["out"]?.let(::File) ?: return usage(out)
        val seed = seedLesen(keyFile, out) ?: return 2
        if (!karte.isFile) {
            out.appendLine("Kartendatei fehlt: $karte")
            return 2
        }
        if (karte.absoluteFile == ziel.absoluteFile) {
            out.appendLine("Quelle und Ziel sind dieselbe Datei.")
            return 2
        }
        if (ziel.exists()) {
            out.appendLine("Zieldatei existiert bereits, nichts ueberschrieben: $ziel")
            return 2
        }
        if (karte.length() > KARTE_PRUEFGRENZE) {
            out.appendLine("Karte groesser als $KARTE_PRUEFGRENZE Bytes.")
            return 2
        }
        // Dieselbe Regel wie beim Inhaltspaket: Was das Werkzeug nicht
        // verstanden hat, unterschreibt es nicht. Beim Paket ist das der
        // ZIP-Aufbau, bei der Karte jede einzelne Kachel -- gelesen mit
        // demselben Leser, den auch die App benutzt.
        val beanstandung = karteDurchsehen(karte)
        if (beanstandung != null) {
            out.appendLine("Karte nicht in Ordnung: ${clean(beanstandung)}")
            out.appendLine("Was hier nicht aufgeht, wuerde ungeprueft mitsigniert.")
            return 2
        }

        val bytes = try {
            karte.readBytes()
        } catch (e: IOException) {
            out.appendLine("Karte nicht lesbar: ${clean(e.message)}")
            return 2
        }
        val entwurf = File(ziel.absoluteFile.parentFile, ziel.name + ".pruefung")
        return try {
            PackWriter.writeMitPruefsumme(bytes, seed, entwurf, PackFormat.KARTE_MAGIC)
            val publicKey = Ed25519.publicKeyFromSeed(seed)
            val speicher = TrustStore(listOf(TrustedKey("selbst", publicKey)))
            val geprueft = try {
                Kartenumschlag.oeffne(entwurf, speicher).use { it.geprueft }
            } catch (e: Kartenfehler) {
                out.appendLine("Die geschriebene Karte prueft sich nicht selbst: ${clean(e.message)}")
                false
            }
            if (!geprueft) return 2
            Files.move(entwurf.toPath(), ziel.toPath(), StandardCopyOption.REPLACE_EXISTING)
            out.appendLine("Karte signiert: $ziel (${ziel.length()} Bytes)")
            out.appendLine("Fingerabdruck: ${Digests.fingerprint(publicKey)}")
            0
        } catch (e: IOException) {
            out.appendLine("Schreiben fehlgeschlagen: ${clean(e.message)}")
            2
        } finally {
            entwurf.delete()
        }
    }

    // Liest jede Kachel wirklich. Ein Verzeichnis, das aufgeht, sagt noch
    // nichts ueber die Kacheln, auf die es zeigt.
    private fun karteDurchsehen(datei: File): String? = try {
        Kartendatei.oeffne(datei).use { karte ->
            var kacheln = 0
            for (zoom in karte.zoomKleinste..karte.zoomGroesste) {
                for ((x, y) in karte.kachelliste(zoom)) {
                    karte.kachel(zoom, x, y) ?: return "Kachel $zoom/$x/$y steht im Verzeichnis, fehlt aber"
                    kacheln++
                }
            }
            if (kacheln == 0) "die Karte enthaelt keine einzige Kachel" else null
        }
    } catch (e: Kartenfehler) {
        e.message ?: "unlesbar"
    }

    private fun kartePruefen(options: Map<String, String>, out: Appendable): Int {
        val datei = options["in"]?.let(::File) ?: return usage(out)
        val trust = loadKeys(options["keys"], out) ?: return 2
        return try {
            Kartenumschlag.oeffne(datei, trust).use { offen ->
                when (val urteil = offen.urteil) {
                    null -> {
                        out.appendLine("Diese Karte traegt KEINE Unterschrift.")
                        out.appendLine("Zoom ${offen.datei.zoomKleinste}..${offen.datei.zoomGroesste}, " +
                            "${offen.datei.kachelzahl} Kacheln")
                        1
                    }
                    is PackVerdict.Trusted -> {
                        out.appendLine("Signatur GUELTIG, Signierer: ${urteil.signer.name}")
                        out.appendLine("Zoom ${offen.datei.zoomKleinste}..${offen.datei.zoomGroesste}, " +
                            "${offen.datei.kachelzahl} Kacheln")
                        out.appendLine("SHA-256 der Kartendatei: ${sha256Of(datei)}")
                        0
                    }
                    is PackVerdict.UnknownSigner -> {
                        out.appendLine("Signatur in sich gueltig, aber Signierer UNBEKANNT.")
                        out.appendLine("Fingerabdruck: ${urteil.fingerprint}")
                        1
                    }
                    else -> {
                        out.appendLine("Karte unbrauchbar: $urteil")
                        1
                    }
                }
            }
        } catch (e: Kartenfehler) {
            out.appendLine("Karte unbrauchbar: ${clean(e.message)}")
            1
        }
    }

    // Die drei Zusatzdateien neben der Karte: Satellitenbild, Namensverzeichnis,
    // Wegenetz. Fuer das Werkzeug unterscheiden sie sich in vier Dingen --
    // Name, Kennung, Durchsicht, Kurzbericht. Alles andere ist bei allen dreien
    // dasselbe und steht deshalb genau einmal da: Wer den Umschlag spaeter
    // aendert, aendert ihn fuer alle drei zugleich und kann keinen vergessen.
    private class Zusatzart(
        val name: String,
        val umschlag: ByteArray,
        val blank: ByteArray,
        /** Liest den Inhalt vollstaendig. Gibt die Beanstandung zurueck, sonst null. */
        val durchsehen: (File, Long) -> String?,
        /** Was drinsteht, in einer Zeile. */
        val bericht: (File, Long) -> String,
    )

    private val BILD = Zusatzart(
        "Satellitenbild", PackFormat.BILD_MAGIC, Bilddatei.KENNUNG,
        ::bilderDurchsehen, ::bilderBericht,
    )

    private val NAMEN = Zusatzart(
        "Namensverzeichnis", PackFormat.NAME_MAGIC, Namensdatei.KENNUNG,
        ::namenDurchsehen, ::namenBericht,
    )

    private val WEGE = Zusatzart(
        "Wegenetz", PackFormat.WEGE_MAGIC, Wegenetz.KENNUNG,
        ::wegeDurchsehen, ::wegeBericht,
    )

    private val HOEHEN = Zusatzart(
        "Gelaendeform", PackFormat.HOEHEN_MAGIC, Hoehenformat.KENNUNG,
        ::hoehenDurchsehen, ::hoehenBericht,
    )

    private fun zusatzSignieren(art: Zusatzart, options: Map<String, String>, out: Appendable): Int {
        val keyFile = options["key"]?.let(::File) ?: return usage(out)
        val quelle = options["in"]?.let(::File) ?: return usage(out)
        val ziel = options["out"]?.let(::File) ?: return usage(out)
        val seed = seedLesen(keyFile, out) ?: return 2
        if (!quelle.isFile) {
            out.appendLine("Datei fehlt: $quelle")
            return 2
        }
        if (quelle.absoluteFile == ziel.absoluteFile) {
            out.appendLine("Quelle und Ziel sind dieselbe Datei.")
            return 2
        }
        if (ziel.exists()) {
            out.appendLine("Zieldatei existiert bereits, nichts ueberschrieben: $ziel")
            return 2
        }
        if (quelle.length() > KARTE_PRUEFGRENZE) {
            out.appendLine("Datei groesser als $KARTE_PRUEFGRENZE Bytes.")
            return 2
        }

        // Dieselbe Regel wie beim Paket und bei der Karte: Was das Werkzeug
        // nicht verstanden hat, unterschreibt es nicht. Bei diesen dreien
        // wiegt das schwerer als bei einer Karte -- ein erfundener Eintrag
        // "Krankenhaus" schickt jemanden an eine Stelle, wo keines ist.
        val beanstandung = art.durchsehen(quelle, 0L)
        if (beanstandung != null) {
            out.appendLine("${art.name} nicht in Ordnung: ${clean(beanstandung)}")
            out.appendLine("Was hier nicht aufgeht, wuerde ungeprueft mitsigniert.")
            return 2
        }

        val bytes = try {
            quelle.readBytes()
        } catch (e: IOException) {
            out.appendLine("Datei nicht lesbar: ${clean(e.message)}")
            return 2
        }
        val entwurf = File(ziel.absoluteFile.parentFile, ziel.name + ".pruefung")
        return try {
            PackWriter.writeMitPruefsumme(bytes, seed, entwurf, art.umschlag)
            val publicKey = Ed25519.publicKeyFromSeed(seed)
            val speicher = TrustStore(listOf(TrustedKey("selbst", publicKey)))
            val befund = try {
                Zusatzumschlag.pruefe(entwurf, art.umschlag, art.blank, speicher)
            } catch (e: Kartenfehler) {
                out.appendLine("Die geschriebene Datei prueft sich nicht selbst: ${clean(e.message)}")
                return 2
            }
            if (befund.urteil !is PackVerdict.Trusted) {
                out.appendLine("Die geschriebene Datei traegt nicht die erwartete Unterschrift.")
                return 2
            }
            // UND NOCH EINMAL DURCHSEHEN, jetzt hinter dem Umschlag. Die
            // Versaetze im Verzeichnis zaehlen ab dem Inhalt und nicht ab dem
            // Dateianfang; wer den Umschlag nicht dazurechnet, liest um die
            // Laenge des Umschlags verschoben. Ohne diese zweite Durchsicht
            // faellt so ein Fehler erst auf dem Telefon auf, und dort sieht er
            // aus wie eine kaputte Datei.
            val nachher = art.durchsehen(entwurf, befund.versatz)
            if (nachher != null) {
                out.appendLine("Hinter dem Umschlag geht die Datei nicht mehr auf: ${clean(nachher)}")
                return 2
            }
            Files.move(entwurf.toPath(), ziel.toPath(), StandardCopyOption.REPLACE_EXISTING)
            out.appendLine("${art.name} signiert: $ziel (${ziel.length()} Bytes)")
            out.appendLine(art.bericht(ziel, befund.versatz))
            out.appendLine("Fingerabdruck: ${Digests.fingerprint(publicKey)}")
            0
        } catch (e: IOException) {
            out.appendLine("Schreiben fehlgeschlagen: ${clean(e.message)}")
            2
        } finally {
            entwurf.delete()
        }
    }

    private fun zusatzPruefen(art: Zusatzart, options: Map<String, String>, out: Appendable): Int {
        val datei = options["in"]?.let(::File) ?: return usage(out)
        val trust = loadKeys(options["keys"], out) ?: return 2
        val befund = try {
            Zusatzumschlag.pruefe(datei, art.umschlag, art.blank, trust)
        } catch (e: Kartenfehler) {
            out.appendLine("${art.name} unbrauchbar: ${clean(e.message)}")
            return 1
        }
        // Auch beim Pruefen wird der Inhalt ganz gelesen. Eine gueltige
        // Unterschrift sagt nur, dass die Datei die ist, die jemand
        // unterschrieben hat -- nicht, dass sie aufgeht.
        val beanstandung = try {
            art.durchsehen(datei, befund.versatz)
        } catch (e: IOException) {
            clean(e.message)
        }
        if (beanstandung != null) {
            out.appendLine("${art.name} nicht in Ordnung: ${clean(beanstandung)}")
            return 1
        }
        val code = when (val urteil = befund.urteil) {
            null -> {
                out.appendLine("Diese Datei traegt KEINE Unterschrift.")
                1
            }
            is PackVerdict.Trusted -> {
                out.appendLine("Signatur GUELTIG, Signierer: ${urteil.signer.name}")
                0
            }
            is PackVerdict.UnknownSigner -> {
                out.appendLine("Signatur in sich gueltig, aber Signierer UNBEKANNT.")
                out.appendLine("Fingerabdruck: ${urteil.fingerprint}")
                1
            }
            else -> {
                out.appendLine("${art.name} unbrauchbar: $urteil")
                1
            }
        }
        out.appendLine(art.bericht(datei, befund.versatz))
        if (code == 0) out.appendLine("SHA-256 der Datei: ${sha256Of(datei)}")
        return code
    }

    // Liest jede Kachel wirklich und laesst sie durch dieselbe Bildpruefung
    // laufen wie die Bilder in einem Inhaltspaket. Ein Verzeichnis, das aufgeht,
    // sagt nichts ueber das, worauf es zeigt.
    private fun bilderDurchsehen(datei: File, versatz: Long): String? = try {
        Bilddatei.oeffne(datei, versatz).use { bild ->
            var gesehen = 0
            for (zoom in bild.zoomKleinste..bild.zoomGroesste) {
                for ((x, y) in bild.kachelliste(zoom)) {
                    val kachel = bild.kachel(zoom, x, y)
                        ?: return "Kachel $zoom/$x/$y steht im Verzeichnis, fehlt aber"
                    // kachel() darf eine groebere Stufe liefern, wenn die
                    // verlangte fehlt. Was im Verzeichnis steht, muss es aber
                    // genau geben -- sonst zeigt der Eintrag ins Leere.
                    if (kachel.zoom != zoom || kachel.x != x || kachel.y != y) {
                        return "Kachel $zoom/$x/$y liefert ${kachel.zoom}/${kachel.x}/${kachel.y}"
                    }
                    bildKennungVonBytes(kachel.roh)?.let { return "Kachel $zoom/$x/$y: $it" }
                    gesehen++
                }
            }
            if (gesehen == 0) "die Datei enthaelt keine einzige Kachel" else null
        }
    } catch (e: IOException) {
        e.message ?: "unlesbar"
    }

    private fun bilderBericht(datei: File, versatz: Long): String =
        Bilddatei.oeffne(datei, versatz).use { bild ->
            val aufnahme = if (bild.aufnahmeVon == 0 && bild.aufnahmeBis == 0) {
                "Aufnahmedatum unbekannt"
            } else {
                "Aufnahmen ${bild.aufnahmeVon} bis ${bild.aufnahmeBis}"
            }
            "Zoom ${bild.zoomKleinste}..${bild.zoomGroesste}, ${bild.kachelzahl} Kacheln, $aufnahme"
        }

    // Die Suche ueber dieses Verzeichnis ist binaer. Auf einer unsortierten
    // Datei findet sie nicht etwa nichts, sondern den falschen Ort -- und wer
    // danach losgeht, geht in die falsche Richtung. Der Leser prueft die
    // Versatztabelle, weil ihn eine kaputte in einen wilden Sprung schickt;
    // die Reihenfolge der SCHLUESSEL kann nur hier geprueft werden, wo die
    // Datei ohnehin einmal ganz gelesen wird.
    private fun namenDurchsehen(datei: File, versatz: Long): String? = try {
        Namensdatei.oeffne(datei, versatz).use { verzeichnis ->
            if (verzeichnis.anzahl == 0) return "das Verzeichnis enthaelt keinen einzigen Namen"
            var vorher: ByteArray? = null
            for (i in 0 until verzeichnis.anzahl) {
                val schluessel = verzeichnis.schluessel(i)
                val letzter = vorher
                if (letzter != null && Namensdatei.vergleicheSchluessel(letzter, schluessel) > 0) {
                    return "Eintrag $i steht vor seinem Vorgaenger -- die Suche faende ihn nie"
                }
                vorher = schluessel
                val text = String(schluessel, Charsets.UTF_8)
                // Der Schluessel muss so gefaltet sein, wie die Suche eine
                // Anfrage faltet. Sonst steht der Name in der Datei und ist
                // trotzdem nicht zu tippen.
                if (Namensdatei.falte(text) != text) {
                    return "Eintrag $i hat den Suchschluessel \"${clean(text)}\", so faltet die Suche nicht"
                }
                val eintrag = verzeichnis.eintrag(i)
                if (eintrag.name.isBlank()) return "Eintrag $i hat keinen Namen"
                if (eintrag.breite < verzeichnis.sued || eintrag.breite > verzeichnis.nord ||
                    eintrag.laenge < verzeichnis.west || eintrag.laenge > verzeichnis.ost
                ) {
                    return "\"${clean(eintrag.name)}\" liegt ausserhalb des angegebenen Rahmens"
                }
            }
            null
        }
    } catch (e: IOException) {
        e.message ?: "unlesbar"
    }

    private fun namenBericht(datei: File, versatz: Long): String =
        Namensdatei.oeffne(datei, versatz).use { verzeichnis ->
            "${verzeichnis.anzahl} Namen, Rahmen " + rahmen(
                verzeichnis.west, verzeichnis.sued, verzeichnis.ost, verzeichnis.nord,
            )
        }

    // Der Leser prueft beim Oeffnen die Kantenkoepfe -- Knotennummern,
    // Aufschlaege, Laengen. Die GEOMETRIE bleibt dabei auf der Platte und wird
    // erst gelesen, wenn eine Route gezeichnet wird. Genau die wird hier einmal
    // ganz durchgegangen: Eine Kante, deren Linie woanders anfaengt als ihr
    // Knoten, gibt eine Route, die im Gelaende nicht zusammenhaengt.
    private fun wegeDurchsehen(datei: File, versatz: Long): String? = try {
        Wegenetz.oeffne(datei, versatz).use { netz ->
            for (kante in 0 until netz.kantenzahl) {
                val punkte = netz.geometrie(kante)
                val a = netz.knotenA(kante)
                val b = netz.knotenB(kante)
                val vorne = Wegenetz.entfernung(
                    punkte.first()[0], punkte.first()[1], netz.breiteVon(a), netz.laengeVon(a),
                )
                val hinten = Wegenetz.entfernung(
                    punkte.last()[0], punkte.last()[1], netz.breiteVon(b), netz.laengeVon(b),
                )
                if (vorne > ENDPUNKT_SCHLUPF || hinten > ENDPUNKT_SCHLUPF) {
                    return "Kante $kante liegt nicht an ihren Knoten " +
                        "(${vorne.toInt()} m / ${hinten.toInt()} m daneben)"
                }
                var gemessen = 0.0
                for (i in 0 until punkte.size - 1) {
                    gemessen += Wegenetz.entfernung(
                        punkte[i][0], punkte[i][1], punkte[i + 1][0], punkte[i + 1][1],
                    )
                }
                // Die angegebene Laenge ist es, mit der die Wegesuche rechnet
                // und die hinterher als Streckenlaenge dasteht. Weicht sie von
                // der Linie ab, plant jemand nach einer Zahl, die nicht stimmt.
                val abweichung = kotlin.math.abs(gemessen - netz.meterVon(kante))
                if (abweichung > LAENGEN_SCHLUPF && abweichung > 0.005 * gemessen) {
                    return "Kante $kante ist mit ${netz.meterVon(kante)} m angegeben, " +
                        "gemessen sind ${gemessen.toInt()} m"
                }
            }
            null
        }
    } catch (e: IOException) {
        e.message ?: "unlesbar"
    }

    private fun wegeBericht(datei: File, versatz: Long): String =
        Wegenetz.oeffne(datei, versatz).use { netz ->
            "${netz.knotenzahl} Knoten, ${netz.kantenzahl} Kanten, Rahmen " +
                rahmen(netz.west, netz.sued, netz.ost, netz.nord)
        }

    // Jede Kachel wird entpackt und gelesen. Das Verzeichnis prueft der Leser
    // schon beim Oeffnen; was in den Kacheln steht, sieht er nie -- und eine
    // Kachel, die sich nicht entpacken laesst, faellt in der App als fehlende
    // Schummerung auf, also als flaches Land.
    private fun hoehenDurchsehen(datei: File, versatz: Long): String? = try {
        Hoehendatei.oeffne(datei, versatz).use { hoehen ->
            var gesehen = 0
            for (zoom in hoehen.zoomKleinste..hoehen.zoomGroesste) {
                for ((x, y) in hoehen.kachelliste(zoom)) {
                    val kachel = hoehen.kachel(zoom, x, y)
                        ?: return "Kachel $zoom/$x/$y steht im Verzeichnis, fehlt aber"
                    if (kachel.zoom != zoom || kachel.kachelX != x || kachel.kachelY != y) {
                        return "Kachel $zoom/$x/$y liefert ${kachel.zoom}/${kachel.kachelX}/${kachel.kachelY}"
                    }
                    gesehen++
                }
            }
            if (gesehen == 0) "die Datei enthaelt keine einzige Kachel" else null
        }
    } catch (e: Kartenfehler) {
        e.message ?: "unlesbar"
    }

    private fun hoehenBericht(datei: File, versatz: Long): String =
        Hoehendatei.oeffne(datei, versatz).use { hoehen ->
            "Zoom ${hoehen.zoomKleinste}..${hoehen.zoomGroesste}, ${hoehen.kachelzahl} Kacheln, " +
                "${hoehen.kante} x ${hoehen.kante} Stuetzstellen je Kachel"
        }

    private fun rahmen(west: Double, sued: Double, ost: Double, nord: Double): String =
        String.format(java.util.Locale.ROOT, "%.3f %.3f bis %.3f %.3f", west, sued, ost, nord)

    // Der Schluessel wird an drei Stellen gelesen. Zweimal dieselbe Pruefung zu
    // schreiben hiesse, dass eine spaetere Berichtigung eine davon vergisst.
    private fun seedLesen(keyFile: File, out: Appendable): ByteArray? {
        if (!keyFile.isFile) {
            out.appendLine("Schluesseldatei fehlt: $keyFile")
            return null
        }
        if (keyFile.name.endsWith(".public")) {
            out.appendLine("Zum Signieren wird die .secret-Datei gebraucht, nicht die .public-Datei.")
            return null
        }
        val seed = try {
            Hex.decode(keyFile.readText().trim())
        } catch (e: IllegalArgumentException) {
            out.appendLine("Schluesseldatei unlesbar: ${clean(e.message)}")
            return null
        }
        if (seed.size != Ed25519.SEED_SIZE) {
            out.appendLine("Schluesseldatei hat ${seed.size} statt ${Ed25519.SEED_SIZE} Bytes.")
            return null
        }
        return seed
    }

    private fun keygen(options: Map<String, String>, out: Appendable): Int {
        val dir = options["dir"]?.let(::File) ?: return usage(out)
        val name = options["name"] ?: return usage(out)
        if (!KEY_NAME.matches(name)) {
            out.appendLine("Name muss zu ${KEY_NAME.pattern} passen.")
            return 2
        }
        if (!dir.isDirectory && !dir.mkdirs()) {
            out.appendLine("Verzeichnis nicht anlegbar: $dir")
            return 2
        }
        val secretFile = File(dir, "$name.secret")
        val publicFile = File(dir, "$name.public")
        if (secretFile.exists() || publicFile.exists()) {
            out.appendLine("Dateien existieren bereits, nichts ueberschrieben: $secretFile / $publicFile")
            return 2
        }
        val seed = Ed25519.generateSeed()
        val publicKey = Ed25519.publicKeyFromSeed(seed)
        secretFile.writeText(Hex.encode(seed) + "\n")
        val rechteGesetzt = restrictToOwner(secretFile)
        publicFile.writeText(Hex.encode(publicKey) + "\n")
        out.appendLine("Schluessel erzeugt: $publicFile")
        out.appendLine("Fingerabdruck: ${Digests.fingerprint(publicKey)}")
        out.appendLine("WICHTIG: $secretFile offline sichern und NIEMALS weitergeben oder einchecken.")
        if (!rechteGesetzt) {
            out.appendLine("ACHTUNG: Die Dateirechte konnten nicht eingeschraenkt werden.")
            out.appendLine("Die Schluesseldatei ist moeglicherweise fuer andere lesbar — Zugriff von Hand pruefen.")
        }
        return 0
    }

    // Der Geheimschluessel darf nicht mit Standardrechten fuer alle lesbar liegen.
    // Gelingt das nicht, muss der Maintainer es erfahren — der Signierschluessel
    // ist die Wurzel des gesamten Vertrauensmodells.
    private fun restrictToOwner(file: File): Boolean {
        val entzogen = file.setReadable(false, false) && file.setWritable(false, false)
        val zurueck = file.setReadable(true, true) && file.setWritable(true, true)
        return entzogen && zurueck
    }

    private fun pack(options: Map<String, String>, out: Appendable): Int {
        val dir = options["in"]?.let(::File) ?: return usage(out)
        val target = options["out"]?.let(::File) ?: return usage(out)
        if (target.exists()) {
            out.appendLine("Zieldatei existiert bereits, nichts ueberschrieben: $target")
            return 2
        }
        val problems = checkContent(dir)
        if (problems.isNotEmpty()) {
            out.appendLine("Inhalt wuerde in der App abgelehnt, deshalb kein Paket gebaut:")
            for (problem in problems.take(20)) out.appendLine("  ${clean(problem)}")
            if (problems.size > 20) out.appendLine("  ... und ${problems.size - 20} weitere")
            return 2
        }
        return try {
            val names = DeterministicZip.write(dir, target)
            out.appendLine("Payload geschrieben: $target (${names.size} Dateien)")
            0
        } catch (e: IllegalArgumentException) {
            out.appendLine("Fehler: ${clean(e.message)}")
            2
        } catch (e: IOException) {
            out.appendLine("Datei nicht schreibbar: ${clean(e.message)}")
            2
        }
    }

    // Fremder Text (Dateinamen, Inhaltsfehler) darf keine Steuerzeichen auf das
    // Terminal bringen.
    private fun clean(value: String?): String =
        (value ?: "unbekannt").map { if (it.isISOControl()) ' ' else it }.joinToString("").take(300)

    // Ein Werkzeug, das kaputte Ueberlebenshinweise signiert, waere schlimmer als gar
    // keins: die Signatur wuerde dem Fehler Glaubwuerdigkeit geben. Deshalb laeuft
    // der Inhalt vor dem Packen durch dieselbe Pruefung wie in der App.
    private fun checkContent(dir: File): List<String> {
        if (!dir.isDirectory) return listOf("kein Verzeichnis: $dir")
        val files = HashMap<String, ByteArray>()
        val assets = HashSet<String>()
        // Dieselbe Sammlung wie beim Packen: keine Verknuepfungen, Tiefengrenze,
        // Groessengrenze. Sonst prueft das Werkzeug etwas anderes, als es packt.
        val gefunden = try {
            DeterministicZip.sammle(dir)
        } catch (e: IllegalArgumentException) {
            return listOf(clean(e.message))
        }
        for ((file, name) in gefunden) {
            if (name.startsWith("assets/")) {
                // Die Spezifikation sagt Bilder zu. Ohne Pruefung koennte unter
                // assets/ beliebiger Inhalt liegen und die Unterschrift des
                // Maintainers tragen, nur weil ihn irgendein Schritt verwendet.
                bildKennung(file)?.let { return listOf("$name: $it") }
                assets.add(name)
                continue
            }
            if (file.length() > ContentLimits.MAX_JSON_BYTES) {
                return listOf("$name: groesser als ${ContentLimits.MAX_JSON_BYTES} Bytes")
            }
            files[name] = file.readBytes()
        }
        return blockendeProbleme(PackParser.parse(files, assets))
    }

    // Liefert die Beanstandung, oder null wenn die Datei als Bild durchgeht.
    private fun bildKennung(file: File): String? {
        // Die ganze Datei, nicht nur der Kopf: Ob hinter dem Bildende noch etwas
        // mitreist, laesst sich am Kopf nicht sehen. Ein Eintrag ist ohnehin auf
        // 8 MiB begrenzt, und das Werkzeug laeuft auf dem Rechner des Maintainers.
        val bytes = try {
            file.readBytes()
        } catch (e: IOException) {
            return "nicht lesbar: ${clean(e.message)}"
        }
        return bildKennungVonBytes(bytes)
    }

    // Dieselbe Regel fuer beide Wege: der Eingabeordner beim Packen und das
    // fertige Paket beim Signieren. Zwei Fassungen wuerden frueher oder spaeter
    // auseinanderlaufen, und die laxere gaebe den Ausschlag.
    private fun bildKennungVonBytes(bytes: ByteArray, laenge: Int = bytes.size): String? {
        if (laenge < 3) return "zu klein fuer ein Bild"
        val png = byteArrayOf(-119, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        if (laenge >= 8 && bytes.copyOf(8).contentEquals(png)) return pngAufbau(bytes, laenge)
        if (bytes[0] == (0xFF).toByte() && bytes[1] == (0xD8).toByte() && bytes[2] == (0xFF).toByte()) {
            return jpegAufbau(bytes, laenge)
        }
        return "ist kein PNG und kein JPEG"
    }

    // Die ersten acht Byte zu pruefen genuegt nicht: dahinter passt beliebig viel
    // Fremdes, und es traegt die Unterschrift des Maintainers mit. Deshalb wird
    // die Kettenstruktur nachgerechnet, bis sie genau am Dateiende aufgeht.
    //
    // Was das NICHT leistet: Ein wohlgeformtes Bild kann in seinen erlaubten
    // Zusatzabschnitten immer noch Daten tragen. Die Pruefung macht das Verstecken
    // aufwaendig, nicht unmoeglich -- sie faengt das versehentliche und das
    // naheliegende Mitschleppen.
    private fun pngAufbau(bytes: ByteArray, laenge: Int): String? {
        var stelle = 8
        var sahEnde = false
        while (stelle + 8 <= laenge) {
            val datenLaenge = lies32(bytes, stelle)
            if (datenLaenge < 0) return "Abschnittslaenge ausserhalb des Bereichs"
            val typ = String(bytes, stelle + 4, 4, Charsets.US_ASCII)
            // Laenge + Typ + Daten + Pruefzahl
            val naechste = stelle.toLong() + 12L + datenLaenge
            if (naechste > laenge) return "Abschnitt $typ ragt ueber das Dateiende hinaus"
            stelle = naechste.toInt()
            if (typ == "IEND") {
                sahEnde = true
                break
            }
        }
        if (!sahEnde) return "kein vollstaendiges PNG (Endabschnitt fehlt)"
        if (stelle != laenge) return "${laenge - stelle} Byte hinter dem Bildende"
        return null
    }

    // Beim JPEG wird der Anfang und das Ende festgenagelt. Ein vollstaendiger
    // Markenlauf durch die Bilddaten waere hier unverhaeltnismaessig; entscheidend
    // ist, dass nichts hinter dem Endezeichen mitreist.
    private fun jpegAufbau(bytes: ByteArray, laenge: Int): String? {
        if (laenge < 4) return "zu klein fuer ein JPEG"
        val endeRichtig = bytes[laenge - 2] == (0xFF).toByte() && bytes[laenge - 1] == (0xD9).toByte()
        if (!endeRichtig) return "kein vollstaendiges JPEG oder Bytes hinter dem Bildende"
        return null
    }

    private fun lies32(bytes: ByteArray, offset: Int): Int {
        var wert = 0
        for (i in 0 until 4) wert = (wert shl 8) or (bytes[offset + i].toInt() and 0xFF)
        return wert
    }

    // Was den Paketbau verhindert: alles Fatale, und jede Inhaltsdatei, die nicht
    // im Manifest angemeldet ist. Letztere wuerde die App gar nicht erst parsen,
    // also ungeprueft mitgeliefert werden.
    private fun blockendeProbleme(result: LoadResult): List<String> {
        val fehler = result.problems
            .filter { it.severity == Severity.Fatal }
            .map { "${it.where}: ${it.code} (${it.detail})" }
            .toMutableList()
        for (problem in result.problems) {
            when (problem.code) {
                "file-ignored" ->
                    fehler.add("${problem.where}: nicht im Manifest angemeldet, wuerde ungeprueft mitgeliefert")
                "asset-unused" ->
                    fehler.add("${problem.where}: von keinem Inhalt verwendet, wuerde ungeprueft mitgeliefert")
                // Eine Bauanleitung mit fehlendem Schrittbild ist ein halber Hinweis.
                "asset-missing" ->
                    fehler.add("${problem.where}: von einem Inhalt verwendet, aber nicht im Paket")
                // Wenn die Meldungsliste ueberlaeuft, kann das Werkzeug nicht mehr
                // sehen, was es signieren wuerde. Dann wird nicht signiert.
                "too-many-problems" ->
                    fehler.add("zu viele Meldungen, Inhalt nicht vollstaendig pruefbar: ${problem.detail}")
            }
        }
        if (fehler.isEmpty() && result.pack == null) fehler.add("Inhalt nicht ladbar")
        return fehler
    }

    private fun sign(options: Map<String, String>, out: Appendable): Int {
        val keyFile = options["key"]?.let(::File) ?: return usage(out)
        val payload = options["in"]?.let(::File) ?: return usage(out)
        val target = options["out"]?.let(::File) ?: return usage(out)
        if (!keyFile.isFile) {
            out.appendLine("Schluesseldatei fehlt: $keyFile")
            return 2
        }
        if (keyFile.name.endsWith(".public")) {
            out.appendLine("Zum Signieren wird die .secret-Datei gebraucht, nicht die .public-Datei.")
            return 2
        }
        if (!payload.isFile) {
            out.appendLine("Payload fehlt: $payload")
            return 2
        }
        val seed = try {
            Hex.decode(keyFile.readText().trim())
        } catch (e: IllegalArgumentException) {
            out.appendLine("Schluesseldatei unlesbar: ${clean(e.message)}")
            return 2
        }
        if (seed.size != Ed25519.SEED_SIZE) {
            out.appendLine("Schluesseldatei hat ${seed.size} statt ${Ed25519.SEED_SIZE} Bytes.")
            return 2
        }
        if (payload.length() > PAYLOAD_PRUEFGRENZE) {
            out.appendLine("Payload groesser als $PAYLOAD_PRUEFGRENZE Bytes, so gross ist kein Inhaltspaket.")
            return 2
        }
        // EINMAL lesen, und aus genau diesen Bytes pruefen und spaeter
        // unterschreiben. Zwei Lesevorgaenge waeren eine Luecke: zwischen ihnen
        // liesse sich die Datei austauschen, und unterschrieben waere etwas, das
        // nie jemand angesehen hat.
        val payloadBytes = try {
            payload.readBytes()
        } catch (e: IOException) {
            out.appendLine("Payload nicht lesbar: ${clean(e.message)}")
            return 2
        }
        val aufbau = try {
            ZipAufbau.problem(payloadBytes)
        } catch (e: RuntimeException) {
            "Aufbau nicht lesbar (${e::class.simpleName})"
        }
        if (aufbau != null) {
            out.appendLine("Payload-Aufbau nicht in Ordnung: ${clean(aufbau)}")
            out.appendLine("Was hier nicht aufgeht, wuerde ungeprueft mitsigniert.")
            return 2
        }
        val publicKey = Ed25519.publicKeyFromSeed(seed)

        if (payload.absoluteFile == target.absoluteFile) {
            out.appendLine("Quelle und Ziel sind dieselbe Datei.")
            return 2
        }
        if (target.exists()) {
            // Wie beim Schluesselerzeugen: ein vertippter Zielpfad darf kein
            // fertiges Paket ueberschreiben.
            out.appendLine("Zieldatei existiert bereits, nichts ueberschrieben: $target")
            return 2
        }

        // Erst daneben schreiben, pruefen, und nur bei fehlerfreiem Ergebnis an den
        // Zielpfad verschieben. Andernfalls koennte ein abgelehntes, aber bereits
        // signiertes Paket liegen bleiben — etwa wenn das Loeschen scheitert.
        val entwurf = File(target.absoluteFile.parentFile, target.name + ".pruefung")
        return try {
            PackWriter.write(payloadBytes, seed, entwurf)
            val problems = checkSignedPack(entwurf, publicKey)
            if (problems.isNotEmpty()) {
                out.appendLine("Paket verworfen — die App wuerde diesen Inhalt ablehnen:")
                for (problem in problems.take(20)) out.appendLine("  ${clean(problem)}")
                if (problems.size > 20) out.appendLine("  ... und ${problems.size - 20} weitere")
                return 2
            }
            Files.move(entwurf.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            out.appendLine("Paket signiert: $target (${target.length()} Bytes)")
            out.appendLine("Signiert mit Fingerabdruck: ${Digests.fingerprint(publicKey)}")
            out.appendLine("SHA-256 der Paketdatei: ${sha256Of(target)}")
            0
        } catch (e: IllegalArgumentException) {
            out.appendLine("Fehler: ${clean(e.message)}")
            2
        } catch (e: IllegalStateException) {
            out.appendLine("Fehler: ${clean(e.message)}")
            2
        } catch (e: IOException) {
            out.appendLine("Datei nicht schreibbar: ${clean(e.message)}")
            2
        } finally {
            entwurf.delete()
        }
    }

    // Das fertige Paket wird genau so gelesen, wie die App es lesen wuerde —
    // inklusive Container- und Speichergrenzen. Nur was hier durchkommt, darf die
    // Unterschrift des Maintainers tragen; ein Payload aus fremder Hand umgeht
    // die Pruefung sonst einfach, indem er pack ueberspringt.
    private fun checkSignedPack(pack: File, publicKey: ByteArray): List<String> {
        val trust = TrustStore(listOf(TrustedKey("selbst", publicKey)))
        val outcome = PackReader.read(pack, trust)
        val result = outcome.result ?: return listOf(
            when (val verdict = outcome.verdict) {
                is PackVerdict.Damaged -> "Paket unbrauchbar: ${verdict.damage}"
                is PackVerdict.Unsupported -> "Formatversion ${verdict.version} unbekannt"
                is PackVerdict.BadSignature -> "Signatur passt nicht zum Inhalt"
                else -> "Paket nicht lesbar"
            },
        )
        val probleme = blockendeProbleme(result).toMutableList()
        // Die Bildkennung hing bisher nur am pack-Weg. Wer dem Maintainer eine
        // fertige payload.zip schickt ("spart dir Arbeit"), konnte unter assets/
        // beliebige Fremddaten unterbringen: sign las das Paket zwar, sah Bilder
        // aber nur als Name, Groesse und Pruefsumme und unterschrieb sie
        // ungesehen. Geprueft wird jetzt am fertigen Paket, nicht am
        // Eingabeordner -- und ueber readAsset, damit die Bytes gegen die
        // Pruefsumme aus dem signierten Durchlauf haengen.
        val geoeffnet = outcome.pack
        if (geoeffnet != null) {
            for (eintrag in geoeffnet.assets) {
                when (val gelesen = geoeffnet.readAsset(eintrag)) {
                    is AssetRead.Ok -> bildKennungVonBytes(gelesen.bytes)?.let {
                        probleme.add("${eintrag.name}: $it")
                    }
                    is AssetRead.Missing -> probleme.add("${eintrag.name}: im Paket nicht auffindbar")
                    is AssetRead.Damaged -> probleme.add("${eintrag.name}: ${gelesen.damage}")
                }
            }
        }
        return probleme
    }

    private fun sha256Of(file: File): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return Hex.encode(digest.digest())
    }

    private fun verify(options: Map<String, String>, out: Appendable): Int {
        val file = options["in"]?.let(::File) ?: return usage(out)
        val trust = loadKeys(options["keys"], out) ?: return 2
        val opened = PackVerifier(trust).open(file)
        return when (val verdict = opened.verdict) {
            is PackVerdict.Trusted -> {
                out.appendLine("Signatur GUELTIG, Signierer: ${verdict.signer.name}")
                listEntries(opened.entries.map { it.name to it.size }, out)
                out.appendLine("SHA-256 der Paketdatei: ${sha256Of(file)}")
                0
            }
            is PackVerdict.UnknownSigner -> {
                out.appendLine("Signatur in sich gueltig, aber Signierer UNBEKANNT.")
                out.appendLine("Fingerabdruck: ${verdict.fingerprint}")
                1
            }
            is PackVerdict.BadSignature -> {
                out.appendLine("Signatur passt nicht zum Inhalt: Datei ist MANIPULIERT oder beschaedigt.")
                1
            }
            is PackVerdict.Unsupported -> {
                out.appendLine("Formatversion ${verdict.version} kennt dieses Werkzeug nicht.")
                1
            }
            is PackVerdict.Aborted -> {
                out.appendLine("Pruefung abgebrochen.")
                1
            }
            is PackVerdict.Damaged -> {
                if (!file.exists()) {
                    out.appendLine("Datei nicht gefunden: $file")
                } else {
                    out.appendLine("Paket unbrauchbar: ${verdict.damage}")
                }
                1
            }
        }
    }

    private fun listEntries(entries: List<Pair<String, Long>>, out: Appendable) {
        out.appendLine("Inhalt (${entries.size} Dateien):")
        for ((name, size) in entries.sortedBy { it.first }) {
            out.appendLine("  $name ($size Bytes)")
        }
    }

    private fun loadKeys(path: String?, out: Appendable): TrustStore? {
        if (path == null) return TrustStore(emptyList())
        val file = File(path)
        if (!file.isFile) {
            out.appendLine("Schluesselliste fehlt: $file")
            return null
        }
        val keys = ArrayList<TrustedKey>()
        for ((lineNumber, raw) in file.readLines().withIndex()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            val split = line.indexOf('=')
            if (split <= 0) {
                out.appendLine("Zeile ${lineNumber + 1} unlesbar (erwartet Name=Hex): ${clean(line)}")
                return null
            }
            try {
                keys.add(TrustedKey(line.substring(0, split).trim(), Hex.decode(line.substring(split + 1).trim())))
            } catch (e: IllegalArgumentException) {
                out.appendLine("Zeile ${lineNumber + 1} unlesbar: ${e.message}")
                return null
            }
        }
        return try {
            TrustStore(keys)
        } catch (e: IllegalArgumentException) {
            out.appendLine("Schluesselliste fehlerhaft: ${e.message}")
            null
        }
    }
}
