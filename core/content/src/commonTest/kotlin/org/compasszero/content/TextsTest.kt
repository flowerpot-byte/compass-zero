package org.compasszero.content

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TextsTest {

    private fun ausCodes(vararg codes: Int) = codes.map { it.toChar() }.joinToString("")

    private fun ausCodepunkt(codepunkt: Int): String {
        val rest = codepunkt - 0x10000
        return ausCodes(0xD800 + (rest shr 10), 0xDC00 + (rest and 0x3FF))
    }

    @Test
    fun normalerTextIstBrauchbar() {
        assertTrue(Texts.isUsable("Wasser abkochen"))
        assertTrue(Texts.isUsable("3 Minuten"))
        assertTrue(Texts.isUsable("Гид по выживанию"))
        assertTrue(Texts.isUsable("生存"))
        assertTrue(Texts.isUsable("OK"))
        assertTrue(Texts.isUsable("Größe: 5 m"))
    }

    @Test
    fun fuellzeichenGeltenNichtAlsSichtbar() {
        // Alle vier Hangul-Fueller zaehlen fuer Unicode als Buchstaben. Genau
        // damit liess sich die Quellenpflicht aushebeln.
        for (code in listOf(0x115F, 0x1160, 0x3164, 0xFFA0, 0x2800, 0x180E, 0xFFFD)) {
            assertFalse(Texts.isUsable(ausCodes(code, code)), "Fuellzeichen U+%04X".format(code))
        }
    }

    @Test
    fun unsichtbareSteuerzeichenSindVerboten() {
        for (code in listOf(0x200B, 0x200C, 0x200D, 0x2060, 0xFEFF, 0x00AD, 0x061C, 0x202E, 0x0007, 0x0000)) {
            assertFalse(Texts.isUsable("Wasser" + ausCodes(code) + "kochen"), "U+%04X".format(code))
        }
    }

    @Test
    fun leerraumUndZeichensetzungAlleinReichenNicht() {
        assertFalse(Texts.isUsable(""))
        assertFalse(Texts.isUsable("   "))
        assertFalse(Texts.isUsable(ausCodes(0x00A0, 0x2007)))
        assertFalse(Texts.isUsable("-"))
        assertFalse(Texts.isUsable("---"))
        assertFalse(Texts.isUsable("..."))
    }

    @Test
    fun zuKurzIstNichtBrauchbar() {
        assertFalse(Texts.isUsable("a"))
        assertFalse(Texts.isUsable("7"))
    }

    @Test
    fun kurzeFelderDuerfenEinZeichenHaben() {
        // Ein einzelnes chinesisches Zeichen ist ein vollstaendiges Wort.
        assertTrue(Texts.isUsable("水", mindestens = 1))
        assertTrue(Texts.isUsable("5", mindestens = 1))
        assertFalse(Texts.isUsable("-", mindestens = 1))
        assertFalse(Texts.isUsable(ausCodes(0x3164), mindestens = 1))
    }

    @Test
    fun zeichenAusserhalbDerGrundebeneSindErlaubt() {
        // Emoji und seltene ostasiatische Zeichen sind sichtbar und muessen durch.
        assertTrue(Texts.isUsable("Feuer 🔥 machen"))
        assertTrue(Texts.isUsable("𠀀𠀁"))
    }

    @Test
    fun markierungszeichenUndHalbeZeichenSindVerboten() {
        assertFalse(Texts.isUsable("Wasser" + ausCodepunkt(0xE0001) + "kochen"))
        assertFalse(Texts.isUsable("halb\uD800"))
        assertFalse(Texts.isUsable("halb\uDC00"))
    }

    // Absaetze im Fliesstext: von Max am 29.07.2026 entschieden, weil ein
    // medizinischer Tipp von zweitausend Zeichen in einem einzigen Absatz unter
    // Stress kaum zu erfassen ist. Die Ausnahme ist eng, und diese Tests halten
    // sie eng.
    @Test
    fun absaetzeSindNurErlaubtWoSieAusdruecklichZugelassenSind() {
        val mitAbsatz = "Erster Absatz.\n\nZweiter Absatz."
        assertFalse(Texts.isUsable(mitAbsatz), "ohne Erlaubnis darf kein Umbruch durchgehen")
        assertTrue(Texts.isUsable(mitAbsatz, absaetzeErlaubt = true))
    }

    // Ein einzelner Umbruch ist ein Zeilenwechsel, zwei sind ein Absatz. Drei
    // sind kein Absatz mehr, sondern Fuellung -- damit liesse sich ein Tipp
    // optisch leeren, ohne dass die Laengengrenze anschlaegt.
    @Test
    fun mehrAlsEineLeerzeileIstFuellungUndVerboten() {
        assertTrue(Texts.isUsable("eins\nzwei", absaetzeErlaubt = true))
        assertTrue(Texts.isUsable("eins\n\nzwei", absaetzeErlaubt = true))
        assertFalse(Texts.isUsable("eins\n\n\nzwei", absaetzeErlaubt = true))
        assertFalse(Texts.isUsable("eins" + "\n".repeat(40) + "zwei", absaetzeErlaubt = true))
    }

    // Erlaubt ist ausschliesslich U+000A. Alles andere, was auch wie ein Umbruch
    // aussieht, bleibt verboten: Mehrere Schreibweisen fuer denselben Umbruch
    // waeren eine Quelle stiller Unterschiede zwischen Paketen, und sie
    // verhalten sich beim Rendern verschieden.
    @Test
    fun andereUmbruchZeichenBleibenVerboten() {
        for (code in listOf(0x0D, 0x0B, 0x0C, 0x85, 0x2028, 0x2029)) {
            assertFalse(
                Texts.isUsable("eins" + code.toChar() + "zwei", absaetzeErlaubt = true),
                "Zeichen U+%04X darf kein Umbruch sein".format(code),
            )
        }
    }

    // Ein Text, der nur aus Umbruechen besteht, hat nichts Sichtbares und faellt
    // weiterhin durch -- die Erlaubnis lockert die Lesbarkeitspruefung nicht.
    @Test
    fun umbruecheAlleinSindKeinText() {
        assertFalse(Texts.isUsable("\n\n", absaetzeErlaubt = true))
        assertFalse(Texts.isUsable("a\nb", absaetzeErlaubt = true, mindestens = 3))
    }
}
