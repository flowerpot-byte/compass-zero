package org.compasszero.content

import java.io.File
import org.compasszero.security.OpenedPack
import org.compasszero.security.PackLimits
import org.compasszero.security.PackVerdict
import org.compasszero.security.PackVerifier
import org.compasszero.security.TrustStore

// Das geoeffnete Paket wird mitgereicht, nicht weggeworfen: Bilder bleiben im
// Paket und duerfen nur ueber OpenedPack.readAsset gelesen werden, weil nur dort
// die Pruefsumme aus dem signierten Durchlauf als Anker haengt. Ohne diesen Weg
// oeffnet die Oberflaeche die Datei irgendwie anders — und der
// Manipulationsschutz fuer Bilder faellt still weg.
class ReadOutcome(val verdict: PackVerdict, val result: LoadResult?, val pack: OpenedPack? = null)

object PackReader {

    // Inhalt gibt es nur, wenn die Signatur gehalten hat. Bei unbekanntem Signierer
    // wird geladen, aber das Urteil wandert unveraendert zur Oberflaeche, die den
    // Warnhinweis anzeigen muss.
    fun read(file: File, trust: TrustStore, limits: PackLimits = PackLimits.DEFAULT): ReadOutcome {
        val opened = PackVerifier(trust).open(file, limits)
        val readable = opened.verdict is PackVerdict.Trusted || opened.verdict is PackVerdict.UnknownSigner
        if (!readable) return ReadOutcome(opened.verdict, null)

        val assetNames = opened.assets.map { it.name }.toSet()
        return ReadOutcome(opened.verdict, PackParser.parse(opened.contentFiles, assetNames), opened)
    }
}
