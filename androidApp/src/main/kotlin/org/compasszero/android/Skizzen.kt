package org.compasszero.android

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.compasszero.security.AssetRead

// Skizzen liegen im Paket und werden erst beim Anzeigen gelesen. Der einzige
// erlaubte Weg dorthin ist OpenedPack.readAsset: Dort haengt die Pruefsumme aus
// dem signierten Durchlauf. Wer die Paketdatei selbst aufmachte, bekaeme
// denselben Inhalt ohne jeden Manipulationsschutz -- und eine untergeschobene
// Bauskizze ist in diesem Handbuch kein Schoenheitsfehler.
object Skizzen {

    // Was beim Laden schiefgehen kann, wird benannt statt verschwiegen. Eine
    // fehlende Skizze ist ein Hinweis; eine beschaedigte ist eine Warnung, denn
    // dann stimmt etwas mit dem Paket nicht.
    sealed interface Ergebnis {
        class Da(val bild: Bitmap) : Ergebnis
        data object Fehlt : Ergebnis
        class Beschaedigt(val grund: String) : Ergebnis
    }

    fun laden(paket: GeladenesPaket, name: String): Ergebnis {
        val eintrag = paket.geoeffnet.assets.firstOrNull { it.name == name } ?: return Ergebnis.Fehlt
        return when (val gelesen = paket.geoeffnet.readAsset(eintrag)) {
            is AssetRead.Ok -> {
                val bild = BitmapFactory.decodeByteArray(gelesen.bytes, 0, gelesen.bytes.size)
                // Die Pruefsumme hat gehalten, das Bild laesst sich trotzdem
                // nicht dekodieren: dann ist schon das Original kaputt.
                if (bild == null) Ergebnis.Beschaedigt("nicht lesbar") else Ergebnis.Da(bild)
            }

            is AssetRead.Missing -> Ergebnis.Fehlt
            is AssetRead.Damaged -> Ergebnis.Beschaedigt(gelesen.damage.kind.name)
        }
    }
}
