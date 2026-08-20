package org.compasszero.content

import kotlinx.serialization.Serializable

// `detail` nennt Dokument und Abrufdatum bzw. Fassung, `name` nur die Herkunft.
// Das Feld ist Pflicht: als Kann-Angabe bleibt nach ein paar Runden nur noch der
// Name der Organisation stehen, und dann laesst sich keine einzige Aussage mehr
// zu der Stelle zurueckverfolgen, aus der sie stammt.
@Serializable
class SourceRef(val name: String, val detail: String)
