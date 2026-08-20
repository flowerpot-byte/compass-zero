package org.compasszero.security

// Aufzaehlung statt freiem Text, damit die Oberflaeche eine eigene, uebersetzte
// Meldung je Ursache zeigen kann. detail dient nur der Fehlersuche.
enum class DamageKind {
    TooShort,
    MagicMismatch,
    SizeMismatch,
    PayloadTooLarge,
    Unreadable,
    ContainerBroken,
    EntryNameForbidden,
    DuplicateEntry,
    TooManyEntries,
    EntryTooLarge,
    PackTooLarge,
    NoEntries,
    HashMismatch,
}

class Damage(val kind: DamageKind, val detail: String = "") {
    override fun toString(): String = if (detail.isEmpty()) "$kind" else "$kind ($detail)"
}
