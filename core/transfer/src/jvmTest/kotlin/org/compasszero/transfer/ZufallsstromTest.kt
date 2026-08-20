package org.compasszero.transfer

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

// Feindliche Rahmenfolgen statt gedachter Faelle. Der Zufallstest der
// Inhaltskette hat einen Fehler gefunden, den kein Pruefdurchgang gemeldet
// hatte; derselbe Griff gehoert an den Empfangsweg, weil dort die Rahmen von
// einem fremden Geraet kommen.
class ZufallsstromTest {

    private class ZaehlSenke : Datensenke {
        var geschrieben = 0L
        var verworfen = false
        var schriebNachEnde = false
        var gesperrt = false

        override fun schreibe(bytes: ByteArray, offset: Int, laenge: Int) {
            if (gesperrt) schriebNachEnde = true
            geschrieben += laenge
        }

        override fun abschliessen() = Unit

        override fun verwirf() {
            verworfen = true
            geschrieben = 0
        }
    }

    private class NullPruefsumme : Pruefsumme {
        var gefuettert = 0L
        override fun fuettere(bytes: ByteArray, offset: Int, laenge: Int) {
            gefuettert += laenge
        }
        override fun abschluss(): ByteArray = ByteArray(TransferFormat.HASH_SIZE)
    }

    private fun zufallsRahmen(zufall: Random, angesagt: Long): Rahmen = when (zufall.nextInt(7)) {
        0 -> Rahmen.Angebot(
            (zufall.nextLong(1, 5_000)),
            ByteArray(TransferFormat.HASH_SIZE) { zufall.nextInt(256).toByte() },
            "paket-${zufall.nextInt(1000)}.czp",
        )
        1 -> Rahmen.Annahme
        2 -> Rahmen.Ablehnung(Ablehnungsgrund.entries[zufall.nextInt(Ablehnungsgrund.entries.size)])
        3 -> Rahmen.Fertig
        4 -> Rahmen.Abbruch(Abbruchgrund.entries[zufall.nextInt(Abbruchgrund.entries.size)])
        // Datenrahmen mal weit unter, mal weit ueber der Ansage.
        else -> {
            val obergrenze = maxOf(1, minOf(TransferFormat.MAX_RAHMEN_NUTZLAST, (angesagt * 2).toInt()))
            Rahmen.Daten(ByteArray(zufall.nextInt(1, obergrenze + 1)))
        }
    }

    @Test
    fun feindlicheRahmenfolgenBringenDenEmpfaengerNichtAusDemTritt() {
        for (durchgang in 0 until 12_000) {
            val zufall = Random(durchgang)
            val senke = ZaehlSenke()
            val summe = NullPruefsumme()
            val empfaenger = Empfaenger(senke, summe)
            var angesagt = 1L

            repeat(zufall.nextInt(1, 12)) {
                if (zufall.nextInt(5) == 0) {
                    empfaenger.entscheide(zufall.nextBoolean())
                } else {
                    val antwort = empfaenger.rahmenEmpfangen(zufallsRahmen(zufall, angesagt))
                    if (antwort is Antwort.Frage) angesagt = antwort.angebot.groesse
                }

                val beendet = empfaenger.zustand == EmpfangsZustand.Beendet ||
                    empfaenger.zustand == EmpfangsZustand.Abgeschlossen
                if (beendet) senke.gesperrt = true

                // Kernzusage: nie mehr Bytes speichern als angesagt, und nach dem
                // Ende ueberhaupt keine mehr.
                // Gemessen wird an der Senke, nicht am Zaehler der Maschine:
                // Der Zaehler wird von genau dem Pfad hochgezaehlt, der auch
                // schreibt -- eine Zusicherung darauf waere fast selbsterfuellend.
                assertTrue(
                    senke.geschrieben <= empfaenger.angesagteBytes,
                    "Durchgang $durchgang: ${senke.geschrieben} Bytes geschrieben, angesagt ${empfaenger.angesagteBytes}",
                )
                if (!senke.verworfen) {
                    assertEquals(
                        empfaenger.empfangeneBytes, senke.geschrieben,
                        "Durchgang $durchgang: Zaehler und Senke gehen auseinander",
                    )
                }
                assertTrue(!senke.schriebNachEnde, "Durchgang $durchgang: Schreibzugriff nach dem Ende")
            }

            // Die Pruefsumme muss genau das gesehen haben, was gespeichert wurde.
            if (!senke.verworfen) {
                assertEquals(senke.geschrieben, summe.gefuettert, "Durchgang $durchgang")
            }
        }
    }

    // Der Test oben kommt nie ans Ziel: zufaellige Rahmen treffen weder die
    // angesagte Bytezahl noch die Pruefsumme. Der Erfolgsfall braucht deshalb
    // einen eigenen Lauf — hier mit zufaelliger Stueckelung, weil ein Paket
    // ueber Funk in beliebig geschnittenen Haeppchen ankommt.
    @Test
    fun gueltigeUebertragungHaeltJedeStueckelungAus() {
        for (durchgang in 0 until 6_000) {
            val zufall = Random(durchgang)
            val senke = ZaehlSenke()
            val summe = NullPruefsumme()
            val empfaenger = Empfaenger(senke, summe)
            val groesse = zufall.nextInt(1, 4_000)

            empfaenger.rahmenEmpfangen(
                Rahmen.Angebot(groesse.toLong(), ByteArray(TransferFormat.HASH_SIZE), "paket.czp"),
            )
            empfaenger.entscheide(true)

            var offen = groesse
            var letzte: Antwort = Antwort.Nichts
            while (offen > 0) {
                val stueck = zufall.nextInt(1, offen + 1)
                letzte = empfaenger.rahmenEmpfangen(Rahmen.Daten(ByteArray(stueck)))
                assertEquals(Antwort.Nichts, letzte, "Durchgang $durchgang: Stueck abgelehnt")
                offen -= stueck
            }
            letzte = empfaenger.rahmenEmpfangen(Rahmen.Fertig)

            assertTrue(letzte is Antwort.Fertig, "Durchgang $durchgang: nicht fertig geworden, sondern $letzte")
            assertEquals(groesse.toLong(), letzte.bytes, "Durchgang $durchgang")
            assertEquals(groesse.toLong(), senke.geschrieben, "Durchgang $durchgang")
            assertEquals(groesse.toLong(), summe.gefuettert, "Durchgang $durchgang")
            assertTrue(!senke.verworfen, "Durchgang $durchgang: gueltige Uebertragung wurde verworfen")
        }
    }

    // Der echte Rand: bis auf das letzte Byte auffuellen, dann eines zu viel.
    // Ein Test, der nur ein Promille der Ansage schickt, kann gar nicht rot
    // werden und sichert nichts zu.
    @Test
    fun genauBisZurAnsageGehtDurchUndEinByteMehrNicht() {
        val ansage = 3 * TransferFormat.MAX_RAHMEN_NUTZLAST + 137
        val senke = ZaehlSenke()
        val empfaenger = Empfaenger(senke, NullPruefsumme())
        empfaenger.rahmenEmpfangen(
            Rahmen.Angebot(ansage.toLong(), ByteArray(TransferFormat.HASH_SIZE), "gross.czp"),
        )
        empfaenger.entscheide(true)

        var offen = ansage
        while (offen > 0) {
            val stueck = minOf(offen, TransferFormat.MAX_RAHMEN_NUTZLAST)
            assertEquals(Antwort.Nichts, empfaenger.rahmenEmpfangen(Rahmen.Daten(ByteArray(stueck))))
            offen -= stueck
        }
        assertEquals(ansage.toLong(), senke.geschrieben)

        // Jetzt ist die Ansage genau erfuellt -- ein einziges Byte mehr muss
        // abbrechen, und die Senke darf es nicht zu sehen bekommen.
        val vorher = senke.geschrieben
        val antwort = empfaenger.rahmenEmpfangen(Rahmen.Daten(ByteArray(1)))
        val fehler = assertIs<Antwort.Fehlgeschlagen>(antwort)
        assertEquals(TransferFehler.ZuVieleDaten, fehler.fehler)
        assertTrue(senke.verworfen, "die Senke wurde nicht verworfen")
        assertTrue(senke.geschrieben <= vorher, "nach dem Abbruch wurde noch geschrieben")
    }
}
