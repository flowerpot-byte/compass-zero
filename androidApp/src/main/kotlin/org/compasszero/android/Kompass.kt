package org.compasszero.android

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

// Der Kompass rechnet aus Beschleunigungs- und Magnetfeldsensor eine
// Himmelsrichtung. Bewusst nicht ueber den Drehvektor: der braucht ein
// Gyroskop, und genau das fehlt in alten und billigen Geraeten -- also in der
// Haelfte der Zielgeraete. Beide hier benutzten Sensoren brauchen keine
// Berechtigung und keine Ortung.
class Kompass(private val sensoren: SensorManager) : SensorEventListener {

    // Was der Kompass gerade sagen kann. Ein Messwert ohne seine Verlaesslichkeit
    // waere im Gelaende gefaehrlich: Eine falsche Richtung sieht genauso aus wie
    // eine richtige.
    sealed interface Stand {
        data object KeinSensor : Stand
        data object NochKeineMessung : Stand
        // Das Geraet ist zu schraeg. Bei aufrecht gehaltenem Geraet steht die
        // Rechnung auf der Kante, und der Wert springt -- dann lieber gar keine
        // Zahl zeigen als eine falsche.
        data object ZuSchraeg : Stand
        class Richtung(val grad: Float, val verlaesslich: Boolean) : Stand
    }

    var stand: Stand = Stand.NochKeineMessung
        private set

    var beiAenderung: (() -> Unit)? = null

    private val magnetfeld = sensoren.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val lage = sensoren.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val ausLage = FloatArray(3)
    private val ausFeld = FloatArray(3)
    private var lageDa = false
    private var feldDa = false
    private val drehung = FloatArray(9)
    private val neigung = FloatArray(9)
    private val winkel = FloatArray(3)

    // Geglaettet wird ueber Sinus und Kosinus, nicht ueber den Gradwert. Sonst
    // laeuft der Mittelwert beim Sprung von 359 auf 0 Grad einmal quer durch den
    // ganzen Kreis, und die Nadel schlaegt nach Sueden aus.
    private val mittel = Mittel()

    private var genauigkeit = SensorManager.SENSOR_STATUS_ACCURACY_HIGH
    private var laeuft = false

    val vorhanden: Boolean get() = magnetfeld != null && lage != null

    fun starten(sparmodus: Boolean) {
        if (laeuft || !vorhanden) {
            if (!vorhanden) stand = Stand.KeinSensor
            return
        }
        // Im Sparmodus die langsamere Taktung: Der Sensor meldet dann rund
        // fuenfmal je Sekunde statt rund sechzehnmal. Fuer eine Richtung reicht
        // das, und es ist der Unterschied, den der Sparmodus ausmachen soll.
        val takt = if (sparmodus) SensorManager.SENSOR_DELAY_NORMAL else SensorManager.SENSOR_DELAY_UI
        sensoren.registerListener(this, magnetfeld, takt)
        sensoren.registerListener(this, lage, takt)
        laeuft = true
    }

    fun anhalten() {
        if (!laeuft) return
        sensoren.unregisterListener(this)
        laeuft = false
    }

    override fun onAccuracyChanged(sensor: Sensor?, neu: Int) {
        if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) genauigkeit = neu
    }

    override fun onSensorChanged(ereignis: SensorEvent) {
        when (ereignis.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                ereignis.values.copyInto(ausLage, endIndex = 3)
                lageDa = true
            }

            Sensor.TYPE_MAGNETIC_FIELD -> {
                ereignis.values.copyInto(ausFeld, endIndex = 3)
                feldDa = true
            }

            else -> return
        }
        if (!lageDa || !feldDa) return
        if (!SensorManager.getRotationMatrix(drehung, neigung, ausLage, ausFeld)) return
        SensorManager.getOrientation(drehung, winkel)

        val neigungGrad = Math.toDegrees(winkel[1].toDouble())
        val kippungGrad = Math.toDegrees(winkel[2].toDouble())
        if (!istFlach(neigungGrad, kippungGrad)) {
            melde(Stand.ZuSchraeg)
            return
        }

        var roh = Math.toDegrees(winkel[0].toDouble()).toFloat()
        if (roh < 0f) roh += 360f
        val grad = mittel.schritt(roh, GLAETTUNG)
        melde(Stand.Richtung(grad, genauigkeit >= SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM))
    }

        // Die Glaettung haelt Sinus und Kosinus als Zustand, nicht den Gradwert.
        // Ueber den Gradwert zu mitteln liefe beim Sprung von 359 auf 0 Grad
        // einmal quer durch den ganzen Kreis, und die Nadel schluege kurz nach
        // Sueden aus.
        //
        // Der Zustand wird ausdruecklich NICHT nach jedem Schritt auf Laenge
        // eins zurueckgerechnet. Genau daran ist eine frühere Fassung
        // haengengeblieben: Bei einer exakten Kehrtwende um 180 Grad heben sich
        // die Sinus-Anteile auf, der Zeiger laeuft auf der Achse zum Nullpunkt
        // und kommt auf der anderen Seite wieder heraus. Wird zwischendurch auf
        // Laenge eins normiert, steht er fuer immer am Ausgangswert -- der
        // Kompass friert bei einer Kehrtwende ein. Am Geraet gesehen, nicht
        // durch einen Test gefunden.
    internal class Mittel {

        private var s = 0.0
        private var c = 0.0
        private var hatWert = false

        val grad: Float
            get() {
                var g = Math.toDegrees(atan2(s, c)).toFloat()
                if (g < 0f) g += 360f
                return g
            }

        fun schritt(neuGrad: Float, anteil: Float): Float {
            val bogen = Math.toRadians(neuGrad.toDouble())
            if (hatWert) {
                s += (sin(bogen) - s) * anteil
                c += (cos(bogen) - c) * anteil
            } else {
                s = sin(bogen)
                c = cos(bogen)
                hatWert = true
            }
            return grad
    }
    }

    private fun melde(neu: Stand) {
        val vorher = stand
        stand = neu
        // Nur melden, wenn sich etwas Sichtbares geaendert hat. Sonst zeichnet
        // die Ansicht bei jedem Sensorwert neu, auch wenn das Bild gleich
        // bleibt -- das kostet Strom fuer nichts.
        if (nennenswert(vorher, neu)) beiAenderung?.invoke()
    }

    private fun nennenswert(a: Stand, b: Stand): Boolean {
        if (a::class != b::class) return true
        if (a is Stand.Richtung && b is Stand.Richtung) {
            if (a.verlaesslich != b.verlaesslich) return true
            return abstandGrad(a.grad, b.grad) >= 0.5f
        }
        return false
    }

    companion object {
        // Ab dieser Schraeglage wird keine Zahl mehr gezeigt. Nicht der beste
        // Wert, sondern ein sicherer: Bei staerkerer Neigung beginnt der Wert
        // zu wandern, und eine falsche Richtung sieht aus wie eine richtige.
        internal const val FLACH_GRENZE = 25.0
        internal const val GLAETTUNG = 0.15f

        private val STRICHE = listOf(
            0 to "N", 45 to "NO", 90 to "O", 135 to "SO",
            180 to "S", 225 to "SW", 270 to "W", 315 to "NW",
        )

        // Kleinster Winkel zwischen zwei Richtungen, ueber den Nulldurchgang
        // hinweg. 359 und 1 Grad liegen zwei Grad auseinander, nicht 358.
        internal fun abstandGrad(a: Float, b: Float): Float {
            val d = abs(a - b) % 360f
            return minOf(d, 360f - d)
        }

        internal fun istFlach(neigungGrad: Double, kippungGrad: Double): Boolean =
            abs(neigungGrad) <= FLACH_GRENZE && abs(kippungGrad) <= FLACH_GRENZE

        fun himmelsrichtung(grad: Float): String {
            var beste = STRICHE.first()
            var bester = 400f
            for (s in STRICHE) {
                val abstand = abstandGrad(grad, s.first.toFloat())
                if (abstand < bester) {
                    bester = abstand
                    beste = s
                }
            }
            return beste.second
        }
    }
}
