package org.compasszero.android

import android.content.Context

/**
 * Was die App sich ueber einen Neustart hinweg merkt.
 *
 * WARUM ES DAS GIBT (Rueckmeldung vom 17.08.2026): Bis dahin merkte die App
 * sich NICHTS -- nach jedem Start war der Sparmodus wieder aus, die Karte
 * wieder voll und jede abgeschaltete Ebene wieder da. Wer im Dunkeln mit
 * halbem Akku den Sparmodus einschaltet, schaltet ihn nach dem naechsten Start
 * wieder ein, und so weiter.
 *
 * WAS HIER NICHT PASSIERT: Es geht nichts ins Netz und nichts an einen
 * Anbieter. `getSharedPreferences` legt eine kleine Datei im PRIVATEN Ordner
 * der App ab -- derselbe Ordner, den eine Deinstallation mitnimmt. Keine
 * Berechtigung, kein Konto, keine Kennung.
 *
 * WAS ABSICHTLICH NICHT GEMERKT WIRD: nichts, was jemand ueber sich selbst
 * verraet. Die eigenen Wegpunkte liegen wie bisher in ihrer eigenen Datei,
 * und wo das Geraet zuletzt stand, ist eine Angabe ueber einen Menschen --
 * die Kartenmitte wird deshalb NICHT hier abgelegt.
 *
 * FEHLERFALL: Laesst sich die Ablage nicht oeffnen, faellt jeder Wert auf
 * seine Voreinstellung zurueck und Schreiben tut nichts. Eine App, die wegen
 * einer gemerkten Einstellung nicht startet, waere der schlimmere Fehler.
 */
class Gemerkt(context: Context) {

    private val ablage = try {
        context.getSharedPreferences("einstellungen", Context.MODE_PRIVATE)
    } catch (fehler: Exception) {
        null
    }

    var sparmodus: Boolean
        get() = lies(SPARMODUS, false)
        set(wert) = schreibe(SPARMODUS, wert)

    var ruhigeKarte: Boolean
        get() = lies(RUHIG, false)
        set(wert) = schreibe(RUHIG, wert)

    var satellitenbild: Boolean
        get() = lies(SATELLIT, false)
        set(wert) = schreibe(SATELLIT, wert)

    /**
     * Ob der Bildschirm anbleibt, solange ein Eintrag der Kategorie
     * "erste-hilfe" geoeffnet ist -- siehe Lexikon.aktualisiereBildschirmwach.
     * Voreinstellung an: Wer eine Herzdruckmassage nach Anleitung macht, soll
     * nicht nach dreissig Sekunden vor einem schwarzen Bildschirm stehen.
     */
    var bildschirmBeiErsteHilfeAn: Boolean
        get() = lies(BILDSCHIRM_ERSTE_HILFE, true)
        set(wert) = schreibe(BILDSCHIRM_ERSTE_HILFE, wert)

    /** Ob eine Gruppe der gezeichneten Karte gezeigt wird. */
    fun zeichnungAn(gruppe: String): Boolean = lies(ZEICHNUNG + gruppe, true)

    fun setzeZeichnung(gruppe: String, an: Boolean) = schreibe(ZEICHNUNG + gruppe, an)

    /**
     * Ob eine Kartenebene eingeschaltet ist.
     *
     * Ueber den NAMEN der Ebene und nicht ueber ihre Nummer: Kommt eine dazu
     * oder faellt eine weg, verschiebt sich sonst still, was der Nutzer
     * eingestellt hat -- aus "Hilfe aus" wuerde "Schutz aus".
     */
    fun ebeneAn(ebene: Kartenblatt.Ebene, voreinstellung: Boolean): Boolean =
        lies(EBENE + ebene.name, voreinstellung)

    fun setzeEbene(ebene: Kartenblatt.Ebene, an: Boolean) = schreibe(EBENE + ebene.name, an)

    /**
     * Ob der Haftungshinweis beim ersten Start bestaetigt wurde -- siehe
     * MainActivity.zeigeHaftungshinweisFallsNoetig. Ueber die Suche erreichbar
     * zu sein reicht rechtlich nicht: Er muss einmal aktiv bestaetigt worden
     * sein, danach nie wieder gezeigt werden.
     */
    var haftungshinweisBestaetigt: Boolean
        get() = lies(HAFTUNGSHINWEIS, false)
        set(wert) = schreibe(HAFTUNGSHINWEIS, wert)

    /**
     * Der Dateiname eines empfangenen Inhaltspakets, das statt der Beigabe
     * benutzt werden soll -- leer, solange es keines gibt.
     *
     * HIER STEHT NUR DER NAME, NIE EINE ENTSCHEIDUNG UEBER VERTRAUEN. Ob das
     * Paket echt ist, wurde beim Annehmen geprueft und wird beim Laden erneut
     * geprueft; diese Zeile merkt sich lediglich, wonach zu schauen ist.
     */
    var eigenesPaket: String
        get() = try {
            ablage?.getString(PAKET, "").orEmpty()
        } catch (fehler: Exception) {
            ""
        }
        set(wert) {
            try {
                ablage?.edit()?.putString(PAKET, wert)?.apply()
            } catch (fehler: Exception) {
                // Siehe schreibe(): nicht merken zu koennen bricht nichts ab.
            }
        }

    private fun lies(schluessel: String, voreinstellung: Boolean): Boolean =
        try {
            ablage?.getBoolean(schluessel, voreinstellung) ?: voreinstellung
        } catch (fehler: Exception) {
            voreinstellung
        }

    // apply() und nicht commit(): geschrieben wird im Hintergrund, der
    // Aufrufer wartet nicht. Ein Schalter, der die Oberflaeche anhaelt, waere
    // an einem Geraet mit langsamem Speicher sofort zu spueren.
    private fun schreibe(schluessel: String, wert: Boolean) {
        try {
            ablage?.edit()?.putBoolean(schluessel, wert)?.apply()
        } catch (fehler: Exception) {
            // Nicht merken zu koennen ist kein Grund, die Bedienung abzubrechen.
        }
    }

    private companion object {
        const val SPARMODUS = "sparmodus"
        const val RUHIG = "ruhige-karte"
        const val EBENE = "ebene-"
        const val SATELLIT = "satellitenbild"
        const val ZEICHNUNG = "zeichnung-"
        const val PAKET = "eigenes-paket"
        const val BILDSCHIRM_ERSTE_HILFE = "bildschirm-erste-hilfe"
        const val HAFTUNGSHINWEIS = "haftungshinweis-bestaetigt"
    }
}
