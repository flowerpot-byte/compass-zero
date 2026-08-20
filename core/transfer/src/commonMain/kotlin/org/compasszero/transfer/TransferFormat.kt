package org.compasszero.transfer

import org.compasszero.security.PackFormat

// Rahmenformat fuer den Geraete-zu-Geraete-Austausch. Es laeuft ueber jeden
// zuverlaessigen Byte-Strom (Bluetooth-RFCOMM, Socket ueber WLAN-Direct) und
// transportiert fertige .czp-Pakete unveraendert.
//
// Bewusst kein JSON: der Strom kommt von einem fremden Geraet und ist bis zur
// Signaturpruefung unvertrauenswuerdig. Feste Binaerfelder mit harten Grenzen
// sind hier die kleinere Angriffsflaeche.
object TransferFormat {

    val MAGIC = byteArrayOf(0x43, 0x5A, 0x54, 0x31) // "CZT1"
    const val VERSION = 1
    const val GRUSS_SIZE = 6
    const val KOPF_SIZE = 5
    const val HASH_SIZE = 32
    const val MAX_NAME_BYTES = 200

    // Steht fuer einen Namen, den das Gegenueber unlesbar geschickt hat. Der
    // Name ist Anzeige; an ihm darf keine Uebertragung scheitern.
    const val ERSATZNAME = "ohne Namen"

    // Der Empfangspuffer hat diese feste Groesse. Ein angesagter Wert darueber
    // wird abgelehnt, statt Speicher nach fremder Ansage zu belegen.
    const val MAX_RAHMEN_NUTZLAST = 64 * 1024

    // Groesser als Envelope plus groesstmoegliche Nutzlast kann keine gueltige
    // Paketdatei sein — aus dem Paketformat abgeleitet, nicht doppelt gepflegt.
    const val MAX_PAKET_BYTES = PackFormat.MAX_PAYLOAD_BYTES + PackFormat.HEADER_SIZE

    const val TYP_ANGEBOT = 0x01
    const val TYP_ANNAHME = 0x02
    const val TYP_ABLEHNUNG = 0x03
    const val TYP_DATEN = 0x04
    const val TYP_FERTIG = 0x05
    const val TYP_ABBRUCH = 0x06
}
