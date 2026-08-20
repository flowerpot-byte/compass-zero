package org.compasszero.content

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Waechter ueber die Titel.
 *
 * Ein Titelwort wiegt in der Suche 5 Punkte und gewinnt damit jeden
 * Gleichstand gegen ein Schlagwort (3) oder Fliesstext (1). Wer ein Wort in
 * einen Titel schreibt, das schon einem Notfall-Tipp gehoert, verdraengt den —
 * lautlos. Im Merkzettel steht deshalb seit dem 29.07.2026 eine Tabu-Liste.
 *
 * ABER: In der Nacht zum 03.08.2026 habe ich sie DREIMAL gebrochen, obwohl ich
 * sie selbst geschrieben hatte —
 *   "Vorraete und PFLANZEN nach dem Atomschlag",
 *   "PFLANZEN, ohne sie zu essen: ...",
 *   "Saft aus PFLANZEN: ...".
 * Jedes Mal hat erst die Suchprobe es gefunden, und einmal gar nicht die
 * Probe, sondern der Sicherheitstest ("Kochen ohne Topf: heisse Steine ins
 * WASSER"). Eine Regel, die nur in einer Textdatei steht, wird vergessen.
 * Deshalb steht sie jetzt hier.
 *
 * Der Waechter vergleicht WORTANFAENGE, nicht Teilzeichenketten — genauso wie
 * die Suche: "Kleinkind" faengt nicht mit "Kind" an und kapert deshalb keinen
 * Kinder-Notfall, "Fieberkrampf" faengt sehr wohl mit "Fieber" an.
 */
class TitelwaechterTest {

    private fun repoRoot(): File {
        val fromProperty = System.getProperty("compasszero.repoRoot")
        if (fromProperty != null) return File(fromProperty)
        var dir = File(".").absoluteFile
        while (!File(dir, "settings.gradle.kts").exists()) {
            dir = dir.parentFile ?: error("settings.gradle.kts nicht gefunden")
        }
        return dir
    }

    private fun titel(): List<Pair<String, String>> {
        val paket = File(repoRoot(), "content/europe-de/paket")
        val result = PackParser.parse(
            mapOf(
                "manifest.json" to File(paket, "manifest.json").readBytes(),
                "content/tips.json" to File(paket, "content/tips.json").readBytes(),
                "content/guides.json" to File(paket, "content/guides.json").readBytes(),
                "content/agriculture.json" to File(paket, "content/agriculture.json").readBytes(),
                "content/terms.json" to File(paket, "content/terms.json").readBytes(),
            ),
            emptySet(),
        )
        val pack = result.pack ?: error("Paket laedt nicht: ${result.problems}")
        return pack.tips.map { it.id to it.title } +
            pack.guides.map { it.id to it.title } +
            pack.agriculture.map { it.id to it.title }
    }

    // Reserviertes Wort -> die Eintraege, die es im Titel tragen DUERFEN.
    // Die Listen sind der Stand vom 03.08.2026 und bewusst vollstaendig
    // aufgeschrieben: Wer einen Titel aendert, sieht hier sofort, wem das Wort
    // sonst noch gehoert.
    private val reserviert: Map<String, Set<String>> = mapOf(
        "wasser" to setOf(
            "wasser-truebes-wasser-vorbehandeln", "wasser-abkochen", "wasser-abkochen-hoehenlage",
            "wasser-solare-entkeimung-sodis", "wasser-geschmack-belueften",
            "wasser-stelle-beurteilen", "medizin-blasenentzuendung", "medizin-harnverhalt",
        ),
        "fieber" to setOf("erste-hilfe-fieber-gefahr", "erste-hilfe-fieber-versorgen"),
        // Am 19.08.2026 dazugekommen, weil es beinahe schiefgegangen waere:
        // Ein neuer Tipp hiess "Abgedichteter Raum: bei Chemie nach oben, bei
        // Strahlung nach unten" und hat damit bei der Eingabe "strahlung" den
        // Erste-Hilfe-Tipp vom ersten Platz verdraengt. Gefunden hat es die
        // Suchmessung, nicht dieser Waechter -- das Wort stand noch nicht in
        // der Liste. Jetzt steht es drin.
        // strahlung-jodtabletten gehoert dem Wort zu Recht -- der Eintrag
        // handelt von nichts anderem. Der Waechter hat ihn beim Aufnehmen
        // des Wortes gemeldet, weil er den Titel des Erste-Hilfe-Tipps
        // ausschreibt; das ist hier kein Einschleppen, sondern ein Verweis
        // vom Thema aufs Thema.
        "strahlung" to setOf(
            "erste-hilfe-strahlung", "strahlung-jodtabletten",
            "taktisch-wohin-schutz", "wasser-nach-atomschlag",
            "nahrung-atomschlag-tiere", "nahrung-atomschlag-pflanzen",
        ),
        "blutung" to setOf("erste-hilfe-starke-blutung", "erste-hilfe-blutung-kopf-rumpf"),
        "wund" to setOf(
            "erste-hilfe-wunde-verbote", "erste-hilfe-wunde-bedecken",
            "erste-hilfe-fremdkoerper-in-wunde", "erste-hilfe-wunde-tetanus", "medizin-wundrose",
        ),
        "hilfe" to setOf("erste-hilfe-durchfall-hilfe-holen", "erste-hilfe-kein-kursersatz"),
        "schmerzen" to emptySet(),
        "husten" to setOf("erste-hilfe-ersticken-kann-husten", "medizin-tuberkulose"),
        "essen" to setOf(
            "erste-hilfe-durchfall-essen", "hygiene-essen", "nahrung-fisch-nicht-roh",
            "nahrung-atomschlag-tiere",
        ),
        "brennt" to setOf("erste-hilfe-brand-reihenfolge"),
        "rauch" to setOf(
            "erste-hilfe-brandrauch-nicht-hineingehen", "erste-hilfe-rauchvergiftung-erkennen",
            "erste-hilfe-rauchvergiftung-helfen",
        ),
        "feuer" to emptySet(),
        "stich" to emptySet(),
        "notruf" to setOf("erste-hilfe-notruf-112"),
        "kind" to setOf(
            "erste-hilfe-wiederbelebung-kind", "erste-hilfe-herzdruckmassage-kind",
            "erste-hilfe-ersticken-kind", "erste-hilfe-durchfall-zink",
            "medizin-lungenentzuendung-kind", "medizin-stillen-genug", "medizin-kindbettfieber",
            "medizin-kindern-erklaeren",
        ),
        "saeugling" to setOf("erste-hilfe-herzdruckmassage-kind", "erste-hilfe-ersticken-saeugling"),
        "gefahr" to emptySet(),
        "waerme" to setOf("erste-hilfe-waermeerhalt", "erste-hilfe-unterkuehlung-stadium-zwei"),
        "erbrechen" to setOf("erste-hilfe-erbrechen-helfen"),
        "blut" to setOf(
            "erste-hilfe-starke-blutung", "erste-hilfe-blutung-kopf-rumpf",
            "medizin-eileiterschwangerschaft",
        ),
        "pflanzen" to emptySet(),
        // Am 03.08.2026 dazugekommen: "Vorrat fuer Monate: poekeln, raeuchern,
        // doerren" hiess zuerst "FLEISCH fuer Monate haltbar machen" und stand
        // damit bei der Anfrage "fleisch" vor "Fleisch vom Wild durchgaren" —
        // dem Tipp, der sagt, dass Wildfleisch durchgegart gehoert. Gefunden
        // hat das wieder der Sicherheitstest, nicht die Suchprobe.
        "fleisch" to setOf(
            "nahrung-fleisch-faeulnis-erkennen", "nahrung-fleisch-schimmel-und-stickigkeit",
            "nahrung-fleisch-schmutz-abtragen", "nahrung-wildfleisch-durchgaren",
            "nahrung-fuchsbandwurm-eier-nicht-fleisch", "nahrung-kerbtiere-essen",
        ),
        // Am 03.08.2026 dazugekommen, nach dem Querverweis-Fehler weiter unten.
        // Die Wiederbelebung ist die Anleitung, bei der eine Verwechslung am
        // teuersten ist: Wer "beatmen" sucht, muss die Beatmung bekommen und
        // nicht einen Tipp, der das Wort nur nebenbei nennt.
        "beatmen" to setOf("erste-hilfe-nur-druecken", "erste-hilfe-wiederbelebung-kind"),
        "wiederbelebung" to setOf(
            "erste-hilfe-nur-druecken", "erste-hilfe-dreissig-zu-zwei",
            "erste-hilfe-wiederbelebung-kind", "erste-hilfe-wann-aufhoeren",
        ),
        "herzdruckmassage" to setOf(
            "erste-hilfe-herzdruckmassage", "erste-hilfe-herzdruckmassage-kind",
        ),
    )

    // Ein reserviertes Wort kommt nicht nur ueber den eigenen Titel in einen
    // Eintrag. Es kommt auch ueber einen AUSGESCHRIEBENEN QUERVERWEIS.
    //
    // So ist es am 03.08.2026 passiert: Der Tipp "Kontaktgifte auf der Haut"
    // bekam einen Verweis auf "Wiederbelebung Erwachsener: druecken, nicht
    // beatmen". Damit stand "beatmen" in einem Vergiftungs-Tipp, und die Suche
    // danach fuehrte wieder mitten in die Vergiftung -- wogegen der Tipp Monate
    // vorher eigens umbenannt worden war. Kein Schlagwort war angefasst worden,
    // kein Titel geaendert; der Waechter oben konnte nichts sehen.
    //
    // Zwei Ausnahmen sind nachgemessen und in Ordnung: Beide stehen weit hinten
    // und der Eintrag, dem das Wort gehoert, steht davor.
    private val geliehenErlaubt = setOf(
        // Die Behelfstrage nennt "Waerme erhalten" -- Rang 8, harmlos.
        "erste-hilfe-behelfstrage" to "waerme",
        // Der Blitzschlag-Tipp verweist auf die Herzdruckmassage, und genau
        // dorthin soll er verweisen -- Rang 5, beide Herzdruck-Tipps davor.
        "erste-hilfe-blitzschlag" to "herzdruckmassage",
    )

    @Test
    fun keinAusgeschriebenerVerweisSchlepptEinReserviertesWortEin() {
        val paket = File(repoRoot(), "content/europe-de/paket")
        val result = PackParser.parse(
            mapOf(
                "manifest.json" to File(paket, "manifest.json").readBytes(),
                "content/tips.json" to File(paket, "content/tips.json").readBytes(),
                "content/guides.json" to File(paket, "content/guides.json").readBytes(),
                "content/agriculture.json" to File(paket, "content/agriculture.json").readBytes(),
                "content/terms.json" to File(paket, "content/terms.json").readBytes(),
            ),
            emptySet(),
        )
        val pack = result.pack ?: error("Paket laedt nicht: ${result.problems}")
        val index = SearchIndex.build(pack)
        val verstoesse = ArrayList<String>()

        for ((kennung, text) in Verweiszitate.texteJeEintrag(pack)) {
            val zitate = Verweiszitate.finde(text)
            if (zitate.isEmpty()) continue
            val eigene = Verweiszitate.eigeneWoerter(pack, kennung, text)
            for ((_, zitiert) in zitate) {
                for (wort in Verweiszitate.gelieheneWoerter(zitiert, eigene)) {
                    val erlaubt = reserviert[wort] ?: continue
                    if (kennung in erlaubt) continue
                    if ((kennung to wort) in geliehenErlaubt) continue
                    val rang = index.search(wort, limit = 8).map { it.id }.indexOf(kennung)
                    if (rang < 0) continue
                    verstoesse.add(
                        "$kennung schreibt den Verweis \"$zitiert\" aus und traegt damit das " +
                            "reservierte Wort \"$wort\" in seinen Text. Bei der Anfrage \"$wort\" " +
                            "steht $kennung dadurch auf Rang ${rang + 1}, obwohl der Eintrag zu " +
                            "diesem Wort nichts zu sagen hat. Entweder einen Titel zitieren, der " +
                            "das Wort nicht fuehrt, oder den Verweis ohne ausgeschriebenen Titel " +
                            "formulieren — oder, wenn es harmlos ist, hier in geliehenErlaubt " +
                            "aufnehmen und den Rang dazuschreiben.",
                    )
                }
            }
        }
        assertTrue(verstoesse.isEmpty(), verstoesse.joinToString("\n\n"))
    }

    private val fold = mapOf('ä' to "ae", 'ö' to "oe", 'ü' to "ue", 'ß' to "ss")

    private fun worte(text: String): List<String> {
        val out = ArrayList<String>()
        val current = StringBuilder()
        for (zeichen in text.lowercase()) {
            if (zeichen.isLetter()) {
                val ersatz = fold[zeichen]
                if (ersatz != null) current.append(ersatz) else current.append(zeichen)
            } else if (current.isNotEmpty()) {
                out.add(current.toString())
                current.setLength(0)
            }
        }
        if (current.isNotEmpty()) out.add(current.toString())
        return out
    }

    @Test
    fun keinNeuerTitelKapertEinReserviertesWort() {
        val verstoesse = ArrayList<String>()
        for ((kennung, text) in titel()) {
            val woerter = worte(text)
            for ((wort, erlaubt) in reserviert) {
                if (kennung in erlaubt) continue
                val treffer = woerter.firstOrNull { it.startsWith(wort) } ?: continue
                verstoesse.add(
                    "$kennung heisst \"$text\" und traegt damit das reservierte Wort " +
                        "\"$wort\" (als \"$treffer\") im Titel. Ein Titelwort wiegt 5 Punkte und " +
                        "verdraengt damit den Eintrag, dem das Wort gehoert. Entweder den TITEL " +
                        "aendern — oder, wenn der Eintrag das Wort zu Recht traegt, ihn hier in " +
                        "die Liste aufnehmen und im selben Zug die Rangfolge nachmessen.",
                )
            }
        }
        assertTrue(verstoesse.isEmpty(), verstoesse.joinToString("\n\n"))
    }

    // Der Waechter taugt nur, solange die Listen zum Paket passen. Steht dort
    // eine Kennung, die es nicht mehr gibt, ist die Liste veraltet — und ein
    // veralteter Waechter erlaubt lautlos das Falsche.
    @Test
    fun dieAusnahmelistenNennenNurVorhandeneEintraege() {
        val vorhanden = titel().map { it.first }.toSet()
        for ((wort, erlaubt) in reserviert) {
            for (kennung in erlaubt) {
                assertTrue(
                    kennung in vorhanden,
                    "die Ausnahmeliste fuer \"$wort\" nennt $kennung — den Eintrag gibt es nicht " +
                        "mehr, die Liste ist veraltet",
                )
            }
        }
    }
}
