package org.compasszero.android

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Die Sprachwahl des Uebersetzers.
 *
 * Anlass: core/content verlangt fuer einen Phrasenkatalog nur EINE Sprache,
 * keine zwei ("phrase catalog needs at least one language" in Phrases.kt).
 * Ein Katalog mit genau einer Sprache ist damit ein gueltiges, signierbares
 * Paket -- und `fremde` wurde in diesem Fall zur leeren Liste. `baue()` rief
 * darauf ungeprueft `fremde.first()` auf und stuerzte beim Oeffnen des
 * Uebersetzer-Bereichs ab (NoSuchElementException). Diese Tests halten die
 * Sprachrechnung selbst fest, ohne dafuer eine Activity zu brauchen.
 */
class UebersetzerTest {

    @Test
    fun eigeneSpracheIstDiePaketspracheWennSieImKatalogSteht() {
        assertEquals("de", Uebersetzer.eigeneSprache("de", listOf("de", "en")))
    }

    @Test
    fun eigeneSpracheFaelltAufDieErsteKatalogspracheZurueck() {
        // Die Paketsprache "de" fehlt im Katalog -- etwa bei einer
        // uebersetzten Paketvariante, die den Katalog nicht mitgebracht hat.
        assertEquals("en", Uebersetzer.eigeneSprache("de", listOf("en", "fr")))
    }

    @Test
    fun fremdeSprachenSindAlleAusserDerEigenen() {
        assertEquals(listOf("en", "fr"), Uebersetzer.fremdeSprachen("de", listOf("de", "en", "fr")))
    }

    // Genau der Fall, der abgestuerzt ist: ein gueltiger Katalog mit nur
    // einer Sprache. `fremde` muss leer herauskommen, statt dass irgendwo
    // `.first()` auf einer leeren Liste aufgerufen wird.
    @Test
    fun katalogMitNurEinerSpracheHatKeineFremdsprache() {
        val fremde = Uebersetzer.fremdeSprachen(
            Uebersetzer.eigeneSprache("de", listOf("de")),
            listOf("de"),
        )
        assertTrue(fremde.isEmpty(), "ein einsprachiger Katalog darf keine Fremdsprache liefern")
    }
}
