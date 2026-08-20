package org.compasszero.android

import android.app.Activity
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Die Anleitung: wie Karten hereinkommen, wie eine neuere Ausgabe eingespielt
 * wird und wie sich pruefen laesst, ob an der App etwas veraendert wurde.
 *
 * WARUM ES DAS ALS EIGENE SEITE GIBT (Rueckmeldung vom 17.08.2026): "ich will
 * das ein modul eingebaut wird das eine anleitung gibt wie man weitere karten
 * reinlaedt aus dem internett bzw neuere releases von mir nutzt und wie man
 * checked ob irgendwas an der app veraendert wurde." Bis dahin standen
 * Bruchstuecke davon zwischen den Einstellungen -- an einer Stelle, an der man
 * sie nur findet, wenn man ohnehin scrollt, und ohne den einen Satz, auf den
 * es ankommt: woher die Dateien ueberhaupt kommen.
 *
 * ERST DIE FRAGE, DANN DER TEXT. Fuenf Kapitel als Liste; man tippt die Frage
 * an, die man hat. Eine durchlaufende Seite waere kuerzer zu bauen und im
 * Ernstfall unbrauchbar.
 *
 * Der Satz steht in `Bausteine.artikel` -- dieselbe Gliederung wie bei den
 * Eintraegen: Zeilen in Grossbuchstaben werden zu Zwischentiteln, Zeilen mit
 * Gedankenstrich zu Aufzaehlungen. Deshalb sieht die Anleitung aus wie der
 * Rest des Programms, ohne dass hier ein einziges Mass steht.
 */
class Anleitung(
    private val gastgeber: Activity,
    private val neuAufbauen: () -> Unit,
) {

    private var offen: Int? = null

    /** True, wenn die Zurueck-Taste hier verbraucht wurde. */
    fun zurueck(): Boolean {
        if (offen == null) return false
        offen = null
        neuAufbauen()
        return true
    }

    fun aufAnfang() {
        offen = null
    }

    fun baue(b: Bausteine, verlassen: () -> Unit): View {
        val spalte = b.spalte().apply { setPadding(0, 0, 0, b.stil.abstand) }
        val hier = offen
        if (hier == null) {
            spalte.addView(rueckweg(b, "Einstellungen", verlassen), b.breit())
            spalte.addView(b.ueberschrift("Anleitung"), b.breit())
            spalte.addView(
                b.nebentext(
                    "Alles hier geht ohne Netz. Die App selbst hat keinen Netzzugang — " +
                        "was hereinkommt, bringst du herein.",
                ),
                b.breit(),
            )
            KAPITEL.forEachIndexed { nummer, kapitel ->
                spalte.addView(kachel(b, kapitel, nummer), b.breit().apply {
                    bottomMargin = b.stil.abstand / 2
                })
            }
        } else {
            val kapitel = KAPITEL[hier]
            spalte.addView(rueckweg(b, "Anleitung") { zurueck() }, b.breit())
            spalte.addView(b.ueberschrift(kapitel.titel), b.breit())
            spalte.addView(b.artikel(kapitel.text), b.breit())
        }
        return ScrollView(gastgeber).apply {
            addView(spalte)
            setBackgroundColor(b.stil.hintergrund)
        }
    }

    // Ein sichtbarer Weg zurueck, nicht nur die Systemtaste: Auf einem Geraet
    // mit Wischbedienung findet die niemand unter Druck.
    private fun rueckweg(b: Bausteine, ziel: String, beiTipp: () -> Unit) =
        TextView(gastgeber).apply {
            text = "‹  $ziel"
            textSize = b.stil.textGroesse
            typeface = b.stil.textSchrift
            setTextColor(b.stil.gedaempft)
            setPadding(0, b.stil.abstand / 2, 0, b.stil.abstand / 2)
            isClickable = true
            isFocusable = true
            setOnClickListener { beiTipp() }
            b.antippbewegung(this)
        }

    private fun kachel(b: Bausteine, kapitel: Kapitel, nummer: Int): View {
        val kasten = b.spalte().apply {
            background = b.kachelflaeche(leer = false)
            setPadding(b.stil.abstand, b.stil.abstand * 3 / 4, b.stil.abstand, b.stil.abstand * 3 / 4)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                offen = nummer
                neuAufbauen()
            }
            b.antippbewegung(this)
        }
        kasten.addView(
            TextView(gastgeber).apply {
                text = kapitel.titel
                textSize = b.stil.textGroesse
                typeface = Typeface.create(b.stil.ueberschriftSchrift, Typeface.BOLD)
                setTextColor(b.stil.text)
                gravity = Gravity.START
            },
            LinearLayout.LayoutParams(Bausteine.MATCH, Bausteine.WRAP),
        )
        kasten.addView(
            TextView(gastgeber).apply {
                text = kapitel.zeile
                textSize = b.stil.textGroesse * 0.82f
                typeface = b.stil.textSchrift
                setTextColor(b.stil.gedaempft)
                setPadding(0, b.stil.abstand / 4, 0, 0)
            },
            LinearLayout.LayoutParams(Bausteine.MATCH, Bausteine.WRAP),
        )
        return kasten
    }

    private class Kapitel(val titel: String, val zeile: String, val text: String)

    private companion object {

        /**
         * Wo es die Ausgaben gibt.
         *
         * STEHT ABSICHTLICH LEER, solange die Stelle nicht feststeht. Eine
         * erfundene oder veraltete Adresse in einer Anleitung, die vor
         * untergeschobenen Dateien warnt, waere genau der Fehler, vor dem sie
         * warnt. Ist hier nichts eingetragen, sagt der Text das geradeheraus,
         * statt auf eine Suchmaschine zu verweisen.
         */
        const val BEZUGSSTELLE = ""

        val KAPITEL = listOf(
            Kapitel(
                "Eine Karte ins Gerät holen",
                "Was eine Kartendatei ist, wie sie hereinkommt, was danach dasteht",
                """
                Eine Karte ist EINE EINZIGE DATEI mit der Endung .czk. Darin steckt alles: Küsten, Flüsse, Wälder, Straßen, Wege, Ortsnamen. Die Geländeform liegt getrennt davon in einer Datei mit der Endung .czh und kommt denselben Weg.

                VIER WEGE INS GERÄT, alle ohne Netz:
                — Am Kabel vom Rechner in irgendeinen Ordner des Handys.
                — Von einer Speicherkarte.
                — Von einem anderen Gerät, das die Datei schon hat.
                — Von einem Stick über einen Adapter.

                DANN IN DEN EINSTELLUNGEN: „Kartendatei auswählen" antippen und die Datei suchen. Die App kopiert sie in ihren eigenen Ordner und prüft dabei die Unterschrift.

                DEN ORDNER DER APP MUSST DU NICHT FINDEN: Du wählst die Datei dort aus, wo sie liegt; das Kopieren macht die App.

                EINE BERECHTIGUNG BRAUCHT DAS NICHT: Du wählst selbst aus, und damit ist genau diese eine Datei freigegeben — für das Einlesen von Karten steht deshalb nichts in der Berechtigungsliste.

                ZUM SCHLUSS DIE KARTE EINMAL NEU ÖFFNEN: Die Kartenansicht liest ihren Bestand beim Aufbau.

                WAS DANACH DASTEHT, UND WAS ES HEISST:
                — „Signatur geprüft: …" — die Karte stammt von einem Schlüssel, den diese App kennt.
                — „Signierer UNBEKANNT (…)" — die Datei ist heil und lesbar, kommt aber von jemand anderem. Bei einer selbst gebauten Karte ist das der richtige Befund.
                — „Gelesen, aber OHNE Unterschrift" — es ist keine drin. Die Karte lässt sich benutzen; wer sie gemacht hat, ist damit nicht zu belegen.
                — „Nicht lesbar: …" — die Datei ist unvollständig oder beschädigt. Meistens ist beim Kopieren etwas abgebrochen: noch einmal übertragen.

                SUCHEN KANN DIE KARTE AUCH: nach Koordinaten immer, nach Orten und nach Ländern, wenn die passenden Dateien danebenliegen.
                — KOORDINATEN gehen ohne alles. Getippt werden darf, wie es auf dem Zettel steht: „47.8 13.05", „47,8 13,05", „47.8N 13.05O", „47°48'00"N 13°03'00"O".
                — LÄNDER kennt die App selbst, mit vollem Namen: „Österreich", „Austria", „Frankreich". Gesprungen wird auf die Hauptstadt, und die App schreibt sie dazu.
                — ORTE, QUELLEN, HÜTTEN, KRANKENHÄUSER brauchen ein Namensverzeichnis: eine Datei mit der Endung .czn, die neben der Karte liegt. Sie kommt denselben Weg ins Gerät wie eine Karte.

                DAS VERZEICHNIS WIRD AUS DER KARTE GEBAUT, nicht heruntergeladen. Wer den Quelltext des Projekts und einen Rechner hat, baut es sich selbst:
                — python tools/karte/namen_bauen.py DEINE-KARTE.czk
                Es entsteht eine .czn neben der Karte; aus einer Detailkarte kommen dabei um die fünfzigtausend Namen in gut zwei Megabyte. Es kann nichts enthalten, was nicht auch auf der Karte steht.

                EINE KARTE VERSCHWINDET NICHT von selbst. Sie bleibt auch dann liegen, wenn die App auf eine neuere Ausgabe gebracht wird.
                """.trimIndent(),
            ),
            Kapitel(
                "Woher es Karten gibt",
                "Aus erster Hand, oder selbst gebaut — und warum die Unterschrift zählt",
                """
                KARTEN KOMMEN NICHT AUS DER APP. Sie hat keinen Netzzugang und bekommt keinen; sie kann also weder suchen noch laden. Geladen wird an einem Gerät MIT Netz — an einem Rechner, an einem anderen Handy —, und die fertige Datei bringst du herüber.

                ${bezugsabsatz()}

                DIE ADRESSE MUSS AUS ERSTER HAND KOMMEN: Nicht aus einer Suchmaschine, nicht aus einem Weiterleitungsdienst, nicht aus einer Nachricht von jemandem, den du nicht kennst. Eine untergeschobene Karte ist der einfachste Angriff auf eine App wie diese — dafür gibt es die Unterschrift, und dafür steht dieser Absatz hier.

                SELBST BAUEN GEHT AUCH. Die Karte entsteht aus den offenen Daten von OpenStreetMap; die Werkzeuge dafür liegen im Quelltext des Projekts und laufen auf einem Rechner. So bekommt jeder eine Karte für seine Gegend, auch für eine, für die es keine fertige gibt.

                EINE SELBST GEBAUTE KARTE ZEIGT „Signierer UNBEKANNT". Das ist kein Fehler und kein Vorwurf: Die App kennt nur die Schlüssel, die ihr mitgegeben wurden, und sagt geradeheraus, dass sie diese Herkunft nicht belegen kann. Bei einer Datei, die du selbst gebaut hast, weißt du es besser als sie.

                BEI EINER DATEI VON JEMAND ANDEREM IST DERSELBE SATZ EINE WARNUNG: Für diesen Inhalt steht niemand ein, den diese App kennt.

                KARTENDATEN © OPENSTREETMAP-MITWIRKENDE, ODbL 1.0. Wer eine Karte weitergibt, gibt diesen Hinweis mit.
                """.trimIndent(),
            ),
            Kapitel(
                "Eine neuere Ausgabe einspielen",
                "Wie eine neue Fassung der App auf das Gerät kommt",
                """
                EINE NEUE AUSGABE KOMMT ALS DATEI mit der Endung .apk — über Kabel, von einer Speicherkarte oder von einem anderen Gerät. Die App lädt nichts von selbst nach; sie hat dafür keine Berechtigung und bekommt auch keine.

                SO GEHT ES:
                1. Die Datei aufs Gerät legen.
                2. Im Dateimanager antippen.
                3. Die Installation bestätigen. Beim ersten Mal fragt Android, ob dieser Dateimanager überhaupt Apps installieren darf.
                4. Danach in den Einstellungen nachsehen, ob der Fingerabdruck derselbe geblieben ist.

                WAS DABEI VON SELBST SCHÜTZT: Android lässt eine Aktualisierung nur zu, wenn sie DIESELBE Unterschrift trägt wie das, was schon auf dem Gerät ist. Eine veränderte App kann sich also nicht über eine echte drüberschieben.

                WENN DIE INSTALLATION ABGELEHNT WIRD und Android von einem Widerspruch zu einer vorhandenen App spricht, ist genau das passiert. DANN NICHT DIE ALTE LÖSCHEN, um es doch zu erzwingen — erst klären, woher die neue Datei kam. Löschen und neu installieren macht aus der Sperre ein erledigtes Hindernis, und der Schutz ist weg.

                WAS EINE AKTUALISIERUNG ÜBERSTEHT: Karten, Geländedaten, Inhaltspakete und die eigenen Wegpunkte. Sie liegen neben der App, nicht in ihr.

                WAS EINE DEINSTALLATION NICHT ÜBERSTEHT: dasselbe. Wer die App löscht, löscht den Ordner mit. Vorher wegkopieren.
                """.trimIndent(),
            ),
            Kapitel(
                "Prüfen, ob an der App etwas verändert wurde",
                "Vier Prüfungen, alle ohne Netz, alle selbst nachvollziehbar",
                """
                Alle vier stehen in den Einstellungen und lassen sich ablesen, ohne uns zu glauben.

                1. DIE BERECHTIGUNGEN. Dort steht, was das BETRIEBSSYSTEM eingetragen hat — nicht, was wir behaupten. Es darf GENAU EINE Zeile stehen, und in ihr muss BLUETOOTH vorkommen: auf neueren Geräten BLUETOOTH_CONNECT, auf Geräten mit Android 11 oder älter stattdessen BLUETOOTH und BLUETOOTH_ADMIN. Steht dort irgendetwas anderes — INTERNET, ein Wort mit LOCATION, CAMERA, RECORD_AUDIO —, ist das nicht mehr diese App. Dieselbe Angabe findest du in den Systemeinstellungen unter „Apps".

                WAS ANDROID BEIM ERSTEN MAL FRAGT, UND WARUM ES SCHLIMMER KLINGT, ALS ES IST: Der Dialog heißt sinngemäß „Darf Compass Zero Geräte in der Nähe finden, sich verbinden und ihre Position bestimmen?". Das ist Androids eigener Text für die ganze Rechtegruppe, nicht unserer. Diese App sucht nichts und bestimmt keine Position — sie spricht nur mit Geräten, die du selbst gekoppelt hast. Nachprüfbar ist das an derselben Liste: Wer suchen wollte, bräuchte BLUETOOTH_SCAN, und das steht dort nicht.

                2. DER FINGERABDRUCK DER APP. Eine lange Zahl aus der Unterschrift, mit der genau diese Installation gebaut wurde. Vergleiche sie mit der, die zur Ausgabe veröffentlicht wurde. Stimmt sie überein, ist die App unverändert; stimmt sie nicht, stammt sie von jemand anderem. Es genügt, sie einmal abgeschrieben oder abfotografiert zu haben — der Vergleich braucht kein Netz.

                3. DIE ZEILE ÜBER DEM INHALT. Auf jedem Bildschirm steht oben, ob die Unterschrift des Inhaltspakets hält. Steht dort etwas anderes als „Signatur geprüft", ist das keine Fußnote mehr, sondern eine Warnung: Die Angaben im Paket sind dann ungeprüft.

                4. DIE PRÜFSUMME DES PAKETS. Dieselbe Zahl wie beim Herausgeber heißt: bitgenau dieselbe Datei. Eine einzige veränderte Stelle ändert die ganze Zahl.

                WENN EINE DAVON NICHT STIMMT: Verlass dich auf nichts, was diese Installation über Leben und Gesundheit sagt. App löschen, aus einer Quelle neu einspielen, der du traust, und die vier Prüfungen wiederholen.

                WANN MAN PRÜFT: einmal nach dem ersten Einspielen, einmal nach jeder Aktualisierung. Nicht in dem Moment, in dem man die App wirklich braucht.
                """.trimIndent(),
            ),
            Kapitel(
                "Was diese App niemals tut",
                "Die kurze Liste, an der sich eine Fälschung erkennen lässt",
                """
                Diese Liste ist selbst ein Werkzeug: Tut die App vor dir etwas davon, ist sie nicht die echte.

                — SIE LÄDT NICHTS NACH. Keinen Inhalt, keine Karte, keine Ausgabe — und sie sieht auch nicht nach, „ob es etwas Neues gibt".
                — SIE FRAGT NIE NACH EINEM KONTO, nach einem Passwort, nach einer Mailadresse, nach einer Telefonnummer.
                — SIE VERLANGT NUR BLUETOOTH, und das allein für die Übergabe von Gerät zu Gerät. Keinen Standort, keine Kamera, kein Mikrofon, keinen Speicherzugriff, kein Netz. Sie sucht auch keine Bluetooth-Geräte und macht sich nicht sichtbar: Gekoppelt wird in den Android-Einstellungen, und genau deshalb braucht sie kein Ortungsrecht — bis Android 11 gilt eine Gerätesuche dem System als Ortsangabe.
                — SIE SCHICKT NICHTS WEG. Keine Fehlerberichte, keine Nutzungszahlen, nichts Anonymisiertes, nichts Freiwilliges.
                — SIE ZEIGT KEINE WERBUNG und verlangt kein Geld für einen Teil ihrer Inhalte.

                EIN HINWEIS IN DER APP, ES LIEGE EINE NEUE FASSUNG BEREIT, KANN NICHT ECHT SEIN: Diese App hat keinen Weg zu erfahren, was es draußen gibt. Wer so etwas sieht, sieht eine Fälschung — und sollte alles daran anzweifeln, auch die Ratschläge.

                DER KOMPASS BRAUCHT KEIN NETZ, die Karte braucht kein Netz, die Suche braucht kein Netz. Es gibt in dieser App keine Stelle, an der ein fehlender Netzzugang etwas verhindert. Bleibt etwas aus, liegt es nie daran.
                """.trimIndent(),
            ),
        )

        private fun bezugsabsatz(): String = if (BEZUGSSTELLE.isNotBlank()) {
            "VOM HERAUSGEBER: $BEZUGSSTELLE — dort liegen die fertigen Karten und die " +
                "Ausgaben der App."
        } else {
            "VOM HERAUSGEBER. Wo die fertigen Karten liegen, steht in dieser Ausgabe noch " +
                "nicht — die Stelle wird mit der Veröffentlichung bekanntgegeben und dann " +
                "hier eingetragen. Bis dahin kommen Karten von jemandem, der sie dir " +
                "persönlich gibt, oder du baust sie selbst."
        }
    }
}
