package org.compasszero.android

import android.app.Activity
import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ScrollView

/**
 * Taschenlampe mit zwei Betriebsarten: Dauerlicht und ein Notsignal in
 * Dreiergruppen.
 *
 * WARUM ES DAS GIBT: Drei Tipps im Paket lehren Lichtsignale ("Notsignale:
 * alles in Dreiergruppen", "Pfeifton oder Lichtblitz", "Spiegel: das
 * staerkste Signal bei Sonne"), aber die App konnte selbst keines geben.
 *
 * `CameraManager.setTorchMode` braucht seit Android 6 KEINE Berechtigung --
 * anders als das Oeffnen einer Kamera zum Aufnehmen. Das ist hier absichtlich
 * so gewaehlt: Die App wirbt damit, nur drei Bluetooth-Rechte zu verlangen,
 * und diese Funktion darf daran nichts aendern (siehe androidApp/build.gradle.kts,
 * ERLAUBTE_BERECHTIGUNGEN). Auf Android 5 gibt es setTorchMode noch nicht --
 * dort gilt die Funktion als nicht verfuegbar, statt gegen eine fehlende
 * Systemmethode zu laufen.
 */
class Taschenlampe(private val gastgeber: Activity) : Bereich {

    override val name = "Licht"
    override val bild = R.drawable.sym_taschenlampe

    private enum class Modus { AUS, DAUER, SIGNAL }

    private val manager: CameraManager? =
        if (Build.VERSION.SDK_INT >= 23) gastgeber.getSystemService(Context.CAMERA_SERVICE) as? CameraManager else null
    private val kamera: String? = sucheBlitzkamera()

    private var modus = Modus.AUS
    private val takt = Handler(Looper.getMainLooper())
    private var blinkstand = 0

    private var dauerknopf: Button? = null
    private var signalknopf: Button? = null

    val verfuegbar: Boolean get() = kamera != null

    private fun sucheBlitzkamera(): String? {
        val m = manager ?: return null
        return try {
            m.cameraIdList.firstOrNull { id ->
                m.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (fehler: CameraAccessException) {
            null
        }
    }

    private fun schalte(an: Boolean) {
        // manager ist nur ab API 23 ueberhaupt gesetzt (siehe oben) -- die
        // Abfrage hier ist fuer den Lint-Pruefer, der das ueber ein Feld nicht
        // nachvollzieht, nicht fuer die Logik selbst.
        if (Build.VERSION.SDK_INT < 23) return
        val m = manager ?: return
        val id = kamera ?: return
        try {
            m.setTorchMode(id, an)
        } catch (fehler: CameraAccessException) {
            // Ein Geraet, das den Blitz gerade selbst braucht (etwa eine
            // Kamera-App im Vordergrund), verweigert das Umschalten -- kein
            // Grund, die App abstuerzen zu lassen.
        }
    }

    override fun baue(b: Bausteine): View {
        val spalte = b.spalte().apply { setPadding(0, b.stil.abstand, 0, b.stil.abstand) }
        spalte.addView(b.ueberschrift("Taschenlampe"), b.breit())

        if (!verfuegbar) {
            dauerknopf = null
            signalknopf = null
            spalte.addView(
                b.nebentext(
                    if (Build.VERSION.SDK_INT < 23) {
                        "Auf dieser Android-Fassung nicht verfügbar."
                    } else {
                        "Dieses Gerät hat keinen ansteuerbaren Blitz."
                    },
                ),
                b.breit(),
            )
            return ScrollView(gastgeber).apply {
                addView(spalte)
                setBackgroundColor(b.stil.hintergrund)
            }
        }

        dauerknopf = knopf(b) { schalteDauerlicht() }.also { spalte.addView(it, b.breit()) }
        spalte.addView(
            b.nebentext("Bleibt an, bis du sie wieder ausschaltest."),
            b.breit(),
        )

        spalte.addView(b.trennstrich(), b.strichbreit())

        signalknopf = knopf(b) { schalteSignal() }.also { spalte.addView(it, b.breit()) }
        spalte.addView(
            b.nebentext(
                "Drei Blitze, Pause, drei Blitze: das weltweit erkannte Notzeichen (siehe " +
                    "„Notsignale: alles in Dreiergruppen“ unter Orientierung). Vorher ausprobieren, " +
                    "nicht erst, wenn es gebraucht wird.",
            ),
            b.breit(),
        )

        aktualisiereKnopftexte()
        return ScrollView(gastgeber).apply {
            addView(spalte)
            setBackgroundColor(b.stil.hintergrund)
        }
    }

    private fun knopf(b: Bausteine, aktion: () -> Unit) = Button(gastgeber).apply {
        textSize = b.stil.textGroesse
        typeface = b.stil.textSchrift
        setTextColor(b.stil.text)
        background = b.randfeld()
        setPadding(b.stil.abstand, b.stil.abstand / 2, b.stil.abstand, b.stil.abstand / 2)
        setOnClickListener { aktion() }
    }

    private fun aktualisiereKnopftexte() {
        dauerknopf?.text = if (modus == Modus.DAUER) "Dauerlicht ausschalten" else "Dauerlicht einschalten"
        signalknopf?.text = if (modus == Modus.SIGNAL) "Notsignal stoppen" else "Notsignal starten"
    }

    private fun schalteDauerlicht() {
        if (modus == Modus.DAUER) {
            stoppeAlles()
        } else {
            stoppeAlles()
            modus = Modus.DAUER
            schalte(true)
        }
        aktualisiereKnopftexte()
    }

    private fun schalteSignal() {
        if (modus == Modus.SIGNAL) {
            stoppeAlles()
        } else {
            stoppeAlles()
            starteSignal()
        }
        aktualisiereKnopftexte()
    }

    private fun starteSignal() {
        modus = Modus.SIGNAL
        blinkstand = 0
        naechsterBlinkschritt()
    }

    // Drei Blitze, dann eine Pause -- das ist die ganze Regel aus dem Tipp
    // "Notsignale: alles in Dreiergruppen". Sechs Schritte wechseln an/aus fuer
    // drei Blitze, danach eine laengere Pause, dann von vorn.
    private fun naechsterBlinkschritt() {
        if (modus != Modus.SIGNAL) return
        if (blinkstand >= BLINK_SCHRITTE) {
            blinkstand = 0
            schalte(false)
            takt.postDelayed(::naechsterBlinkschritt, GRUPPENPAUSE_MS)
            return
        }
        schalte(blinkstand % 2 == 0)
        blinkstand++
        takt.postDelayed(::naechsterBlinkschritt, BLINK_MS)
    }

    private fun stoppeAlles() {
        takt.removeCallbacksAndMessages(null)
        schalte(false)
        modus = Modus.AUS
    }

    // Sowohl beim Wechsel zu einem anderen Bereich als auch beim Zurueckgehen
    // der App: Im Hintergrund laeuft nichts weiter, und ein vergessenes
    // Dauerlicht in der Tasche soll nicht den Akku leeren.
    override fun anhalten() {
        stoppeAlles()
        aktualisiereKnopftexte()
    }

    private companion object {
        const val BLINK_MS = 300L
        const val GRUPPENPAUSE_MS = 1_500L
        const val BLINK_SCHRITTE = 6 // drei Blitze = drei mal an und aus
    }
}
