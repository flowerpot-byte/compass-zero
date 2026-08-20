package org.compasszero.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// In ostasiatischen Schriften ist ein einzelnes Zeichen ein vollstaendiges Wort.
// Ein Paket mit den Ueberschriften Wasser, Feuer, Essen -- 水, 火, 食 -- lud
// bisher gar nicht: nicht das Feld wurde beanstandet, sondern das ganze Paket
// abgelehnt. Fuer ein mehrsprachiges Ueberlebenshandbuch ist das ein Ausschluss.
class EinzelzeichenFelderTest {

    private val quelle = """[{"name":"Beispielquelle","detail":"Abschnitt 1"}]"""

    private fun ladeMit(manifestTitel: String, dateien: Map<String, String>): LoadResult {
        val arten = dateien.keys.map { it.removePrefix("content/").removeSuffix(".json") }
        val manifest = """{"schema":1,"id":"org.compasszero.test","version":1,"language":"ja",""" +
            """"title":"$manifestTitel","kinds":[${arten.joinToString(",") { "\"$it\"" }}]}"""
        val roh = HashMap<String, ByteArray>()
        roh["manifest.json"] = manifest.encodeToByteArray()
        for ((name, inhalt) in dateien) roh[name] = inhalt.encodeToByteArray()
        return PackParser.parse(roh, emptySet())
    }

    @Test
    fun einzelzeichenUeberschriftenLadenDasPaket() {
        val kapitel = """{"schema":1,"chapters":[{"id":"k1","title":"水","sections":[""" +
            """{"heading":"水","body":"Beispieltext zum Wasser."},""" +
            """{"heading":"火","body":"Beispieltext zum Feuer."},""" +
            """{"heading":"食","body":"Beispieltext zum Essen."}""" +
            """],"sources":$quelle}]}"""
        val result = ladeMit("水", mapOf("content/agriculture.json" to kapitel))
        assertTrue(
            result.pack != null,
            "Paket mit Einzelzeichen-Ueberschriften laedt nicht: ${result.problems.map { it.code }}",
        )
        assertEquals(3, result.pack.agriculture.single().sections.size)
    }

    @Test
    fun einzelzeichenSchlagwortUndPakettitelLadenDasPaket() {
        val tipps = """{"schema":1,"tips":[{"id":"t1","title":"水","category":"wasser",""" +
            """"body":"Beispieltext zum Wasser.","keywords":["水","火"],"sources":$quelle}]}"""
        val result = ladeMit("水", mapOf("content/tips.json" to tipps))
        assertTrue(result.pack != null, "Paket laedt nicht: ${result.problems.map { it.code }}")
        assertEquals(listOf("水", "火"), result.pack.tips.single().keywords)
    }

    @Test
    fun einzelzeichenMaterialUndWerkzeugLadenDasPaket() {
        val anleitungen = """{"schema":1,"guides":[{"id":"g1","title":"水","category":"wasser",""" +
            """"summary":"Beispielzusammenfassung.","materials":[{"item":"水"}],"tools":["火"],""" +
            """"steps":[{"text":"Beispielschritt."}],"difficulty":1,"sources":$quelle}]}"""
        val result = ladeMit("Testpaket", mapOf("content/guides.json" to anleitungen))
        assertTrue(result.pack != null, "Paket laedt nicht: ${result.problems.map { it.code }}")
    }

    // Fliesstext bleibt streng: dort ist ein einzelnes Zeichen kein Wort, sondern
    // ein leeres Feld -- und ein leerer Ueberlebenshinweis darf nicht durchgehen.
    @Test
    fun einzelnesZeichenAlsFliesstextBleibtEinFehler() {
        val tipps = """{"schema":1,"tips":[{"id":"t1","title":"Titel","category":"wasser",""" +
            """"body":"x","sources":$quelle}]}"""
        val result = ladeMit("Testpaket", mapOf("content/tips.json" to tipps))
        assertTrue(result.pack == null, "ein Fliesstext aus einem Zeichen wurde angenommen")
        assertTrue("body-invalid" in result.problems.map { it.code }, result.problems.map { it.code }.toString())
    }

    // Unsichtbare Zeichen bleiben verboten, auch als Einzelzeichen: sonst liesse
    // sich ein Feld mit einem Fuellzeichen als ausgefuellt ausgeben.
    @Test
    fun unsichtbaresEinzelzeichenBleibtEinFehler() {
        val kapitel = """{"schema":1,"chapters":[{"id":"k1","title":"Titel","sections":[""" +
            """{"heading":"ㅤ","body":"Beispieltext."}],"sources":$quelle}]}"""
        val result = ladeMit("Testpaket", mapOf("content/agriculture.json" to kapitel))
        assertTrue(result.pack == null, "eine unsichtbare Ueberschrift wurde angenommen")
        assertTrue("heading-invalid" in result.problems.map { it.code }, result.problems.map { it.code }.toString())
    }
}
