package org.compasszero.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JsonShapeTest {

    private fun problem(text: String, limits: ShapeLimits = ShapeLimits.DEFAULT) =
        JsonShape.problem(text, limits)?.first

    @Test
    fun plainJsonPasses() {
        assertNull(problem("""{"a":1,"b":[1,2,3],"c":{"d":"e"}}"""))
    }

    @Test
    fun hostileNestingIsCaught() {
        assertEquals("json-too-deep", problem("[".repeat(200) + "]".repeat(200)))
    }

    @Test
    fun depthCapIsExact() {
        assertNull(problem("[[[0]]]", ShapeLimits(maxDepth = 3)))
        assertEquals("json-too-deep", problem("[[[0]]]", ShapeLimits(maxDepth = 2)))
    }

    @Test
    fun bracketsInsideStringsDoNotCount() {
        assertNull(problem("""{"a":"{{{[[[","b":"x"}""", ShapeLimits(maxDepth = 5)))
    }

    @Test
    fun escapedQuotesStayInsideString() {
        assertNull(problem("""{"a":"x\"[[[[\"y"}""", ShapeLimits(maxDepth = 5)))
    }

    @Test
    fun escapedBackslashEndsTheString() {
        // Der klassische Bruchfall: der Backslash maskiert sich selbst, das
        // folgende Anfuehrungszeichen schliesst die Zeichenkette wirklich.
        assertEquals("json-too-deep", problem("""{"a":"x\\"[[[}""", ShapeLimits(maxDepth = 2)))
    }

    @Test
    fun tooManyElementsAreCaughtBeforeParsing() {
        val many = "[" + (1..50).joinToString(",") { "\"$it\"" } + "]"
        assertEquals("json-too-many-elements", problem(many, ShapeLimits(maxElements = 20)))
        assertNull(problem(many, ShapeLimits(maxElements = 200)))
    }

    @Test
    fun duplicateKeysInTheSameObjectAreRejected() {
        val json = """{"body":"Zehn Minuten kochen.","body":"Rohes Wasser ist unbedenklich."}"""
        assertEquals("json-duplicate-key", problem(json))
    }

    @Test
    fun versteckteDoppelteSchluesselMitMaskierung() {
        // Der eigentliche Angriff: wer die Datei liest, sieht den ersten Wert,
        // die App verwendet den zweiten. Mit einer Maskierung geschrieben faellt
        // das keinem Menschen und keinem Vergleichswerkzeug auf.
        val angriff = """{"body":"Zehn Minuten kochen.","\u0062ody":"Rohes Wasser ist unbedenklich."}"""
        assertTrue(angriff.contains("\\u0062"), "Testdaten muessen die Maskierung wirklich enthalten")
        assertEquals("json-duplicate-key", problem(angriff))

        assertEquals("json-duplicate-key", problem("""{"a\/b":1,"a/b":2}"""))
        assertEquals("json-duplicate-key", problem("""{"a\tb":1,"a	b":2}"""))
        assertEquals("json-duplicate-key", problem("""{"quelle":1,"quelle":2}"""))
    }

    @Test
    fun kaputteMaskierungImSchluesselIstUngueltig() {
        assertEquals("json-invalid", problem("""{"a\q":1}"""))
        assertEquals("json-invalid", problem("""{"a\u00":1}"""))
    }

    @Test
    fun sameKeyInDifferentObjectsIsFine() {
        assertNull(problem("""{"a":{"name":"x"},"b":{"name":"y"}}"""))
        assertNull(problem("""[{"name":"x"},{"name":"y"}]"""))
    }

    @Test
    fun duplicateKeyIsFoundInNestedObjects() {
        assertEquals("json-duplicate-key", problem("""{"a":{"n":1,"m":2,"n":3}}"""))
    }

    @Test
    fun keysAreOnlyKeysBeforeAColon() {
        // "name" als Wert darf nicht als Schluessel zaehlen.
        assertNull(problem("""{"a":"name","name":"b"}"""))
    }

    @Test
    fun unterminatedStringIsRejected() {
        assertEquals("json-invalid", problem("""{"a":"offen"""))
    }

    @Test
    fun unbalancedBracketsAreRejected() {
        assertEquals("json-invalid", problem("""{"a":1"""))
        assertEquals("json-invalid", problem("""{"a":1}}"""))
    }

    @Test
    fun maskierungsVariantenWerdenWieVomParserGelesen() {
        // Grossbuchstaben im Hex, Ersatzzeichen-Paare, geschweifte Klammer als
        // Maskierung, doppelte Maskierung: der Scanner muss genau das sehen, was
        // der Parser sieht — sonst entsteht wieder eine Luecke oder ein Fehlalarm.
        assertEquals("json-duplicate-key", problem("""{"AB":1,"\u0041B":2}"""))
        assertEquals("json-duplicate-key", problem("""{"\uD83D\uDE00":1,"\ud83d\ude00":2}"""))
        // Eine maskierte Klammer im Schluessel ist keine Klammer.
        assertNull(problem("""{"\u007Ba":1,"b":2}"""))
        // Doppelte Maskierung ergibt Backslash plus Text, nicht das Zeichen.
        assertNull(problem("""{"\\u0062":1,"b":2}"""))
        // Maskierung im Wert darf keinen Schluessel vortaeuschen.
        assertNull(problem("""{"a":"\u0062:1","b":2}"""))
    }

    @Test
    fun ungueltigesHexInDerMaskierungIstUngueltig() {
        assertEquals("json-invalid", problem("""{"a\u+062":1}"""))
        assertEquals("json-invalid", problem("""{"a\u-062":1}"""))
        assertEquals("json-invalid", problem("""{"a\u 062":1}"""))
    }

    @Test
    fun falscherKlammertypIstUngueltig() {
        assertEquals("json-invalid", problem("""{"a":1]"""))
        assertEquals("json-invalid", problem("""["a"}"""))
    }
}
