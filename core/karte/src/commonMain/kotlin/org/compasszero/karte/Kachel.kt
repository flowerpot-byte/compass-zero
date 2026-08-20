package org.compasszero.karte

/**
 * Eine entpackte Kachel.
 *
 * Bewusst flache Felder statt eines Objekts je Linie: Eine Kachel traegt
 * mehrere tausend Objekte, und auf einem Geraet von 2014 sind ein paar
 * tausend kurzlebige Objekte je Kachel der Unterschied zwischen einer Karte,
 * die sich schieben laesst, und einer, die alle zwei Sekunden stockt.
 *
 * `x` und `y` stehen im Kachelraster: 0 bis 4095 innerhalb der Kachel, mit
 * einem Ueberstand von [Kartenformat.RAND] nach aussen.
 */
class Kachel(
    val zoom: Int,
    val kachelX: Int,
    val kachelY: Int,
    /** Sorte je Objekt, siehe [Kartenformat.SORTEN]. */
    val sorte: ByteArray,
    /** Art je Objekt, siehe [Kartenformat.Art]. */
    val art: ByteArray,
    /** Punktart je Objekt; nur bei Punkten belegt. */
    val punktart: ByteArray,
    /** Namensnummer je Objekt, -1 wenn ohne Namen. */
    val name: IntArray,
    /** Anfang des Objekts in [x] und [y]. */
    val anfang: IntArray,
    /** Anzahl Stuetzpunkte des Objekts. */
    val laenge: IntArray,
    val x: IntArray,
    val y: IntArray,
    val namen: Array<String>,
) {
    val objekte: Int get() = sorte.size

    fun namenVon(objekt: Int): String? {
        val nummer = name[objekt]
        return if (nummer >= 0 && nummer < namen.size) namen[nummer] else null
    }

    val stuetzpunkte: Int get() = x.size
}

/**
 * Liest eine entpackte Kachel.
 *
 * Jede Laenge aus der Datei wird gegen eine Obergrenze geprueft, bevor
 * irgendetwas belegt wird. Eine Kartendatei kommt von aussen ins Geraet --
 * ueber Bluetooth, ueber eine Speicherkarte, ueber einen fremden Rechner. Wer
 * ihr glaubt, laesst sich mit einer erfundenen Zahl den Speicher fuellen.
 */
object Kachelleser {

    fun lies(roh: ByteArray, zoom: Int, kachelX: Int, kachelY: Int): Kachel {
        val leser = Bytesleser(roh)

        val fassung = leser.byteWert()
        if (fassung != Kartenformat.FASSUNG) {
            throw Kartenfehler("Kachelaufbau $fassung ist unbekannt")
        }

        val namenZahl = leser.varint("Namenszahl", Kartenformat.KACHEL_MAX_NAMEN)
        val namen = Array(namenZahl) {
            val laenge = leser.varint("Namenslaenge", Kartenformat.NAME_MAX_BYTES)
            leser.text(laenge)
        }

        val schichten = leser.varint("Schichtzahl", Kartenformat.SORTEN.size)

        // Erst zaehlen, dann belegen, waere ein zweiter Durchgang. Stattdessen
        // wachsen die Felder -- die Obergrenzen oben halten das im Zaum.
        var sorte = ByteArray(64)
        var art = ByteArray(64)
        var punktart = ByteArray(64)
        var name = IntArray(64)
        var anfang = IntArray(64)
        var laenge = IntArray(64)
        var xs = IntArray(512)
        var ys = IntArray(512)
        var objekte = 0
        var punkte = 0

        for (schicht in 0 until schichten) {
            val dieseSorte = leser.byteWert()
            if (dieseSorte >= Kartenformat.SORTEN.size) {
                throw Kartenfehler("Sorte $dieseSorte ist unbekannt")
            }
            val zahl = leser.varint("Objektzahl", Kartenformat.KACHEL_MAX_OBJEKTE)
            if (objekte + zahl > Kartenformat.KACHEL_MAX_OBJEKTE) {
                throw Kartenfehler("zu viele Objekte in einer Kachel")
            }
            for (i in 0 until zahl) {
                if (objekte == sorte.size) {
                    val neu = objekte * 2
                    sorte = sorte.copyOf(neu)
                    art = art.copyOf(neu)
                    punktart = punktart.copyOf(neu)
                    name = name.copyOf(neu)
                    anfang = anfang.copyOf(neu)
                    laenge = laenge.copyOf(neu)
                }
                val dieseArt = leser.byteWert()
                if (dieseArt > Kartenformat.Art.INNENRING) {
                    throw Kartenfehler("Art $dieseArt ist unbekannt")
                }
                val diesePunktart = leser.varint("Punktart", Kartenformat.PUNKTARTEN.size - 1)
                val nameNummer = leser.varint("Namensnummer", namenZahl)
                val punktZahl = leser.varint("Punktzahl", Kartenformat.KACHEL_MAX_PUNKTE)
                if (punktZahl == 0) throw Kartenfehler("Objekt ohne Stuetzpunkt")
                if (punkte + punktZahl > Kartenformat.KACHEL_MAX_PUNKTE) {
                    throw Kartenfehler("zu viele Stuetzpunkte in einer Kachel")
                }
                while (punkte + punktZahl > xs.size) {
                    xs = xs.copyOf(xs.size * 2)
                    ys = ys.copyOf(ys.size * 2)
                }
                sorte[objekte] = dieseSorte.toByte()
                art[objekte] = dieseArt.toByte()
                punktart[objekte] = diesePunktart.toByte()
                name[objekte] = nameNummer - 1
                anfang[objekte] = punkte
                laenge[objekte] = punktZahl
                var vx = 0
                var vy = 0
                for (k in 0 until punktZahl) {
                    vx += leser.zigzag()
                    vy += leser.zigzag()
                    xs[punkte] = vx
                    ys[punkte] = vy
                    punkte++
                }
                objekte++
            }
        }
        if (!leser.amEnde()) {
            throw Kartenfehler("hinter der Kachel stehen ${leser.rest()} unerklaerte Bytes")
        }

        return Kachel(
            zoom, kachelX, kachelY,
            sorte.copyOf(objekte), art.copyOf(objekte), punktart.copyOf(objekte),
            name.copyOf(objekte), anfang.copyOf(objekte), laenge.copyOf(objekte),
            xs.copyOf(punkte), ys.copyOf(punkte), namen,
        )
    }
}

internal class Bytesleser(private val roh: ByteArray) {

    private var stelle = 0

    fun amEnde() = stelle >= roh.size

    fun rest() = roh.size - stelle

    fun byteWert(): Int {
        if (stelle >= roh.size) throw Kartenfehler("Kachel endet mitten im Satz")
        return roh[stelle++].toInt() and 0xFF
    }

    fun varint(feld: String, hoechstens: Int): Int {
        var wert = 0
        var schub = 0
        while (true) {
            if (stelle >= roh.size) throw Kartenfehler("Kachel endet mitten in einer Zahl")
            // Fuenf Gruppen zu sieben Bit sind 35 Bit -- mehr als ein Int
            // traegt. Ohne diese Schranke laeuft der Wert still ueber und
            // wird negativ.
            if (schub > 28) throw Kartenfehler("$feld ist keine gueltige Zahl")
            val b = roh[stelle++].toInt() and 0xFF
            wert = wert or ((b and 0x7F) shl schub)
            if (b < 0x80) break
            schub += 7
        }
        if (wert < 0 || wert > hoechstens) {
            throw Kartenfehler("$feld ist $wert, erlaubt sind hoechstens $hoechstens")
        }
        return wert
    }

    fun zigzag(): Int {
        var wert = 0
        var schub = 0
        while (true) {
            if (stelle >= roh.size) throw Kartenfehler("Kachel endet mitten in einer Zahl")
            if (schub > 28) throw Kartenfehler("Abstand ist keine gueltige Zahl")
            val b = roh[stelle++].toInt() and 0xFF
            wert = wert or ((b and 0x7F) shl schub)
            if (b < 0x80) break
            schub += 7
        }
        return (wert ushr 1) xor -(wert and 1)
    }

    fun int16(): Int {
        val hoch = byteWert()
        val tief = byteWert()
        val wert = (hoch shl 8) or tief
        return if (wert >= 0x8000) wert - 0x10000 else wert
    }

    fun text(laenge: Int): String {
        if (stelle + laenge > roh.size) throw Kartenfehler("Kachel endet mitten in einem Namen")
        val stueck = roh.copyOfRange(stelle, stelle + laenge)
        stelle += laenge
        return stueck.decodeToString()
    }
}
