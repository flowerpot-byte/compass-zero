package org.compasszero.security

sealed interface PackVerdict {
    class Trusted(val signer: TrustedKey) : PackVerdict
    class UnknownSigner(val fingerprint: String) : PackVerdict
    data object BadSignature : PackVerdict
    data object Aborted : PackVerdict
    class Unsupported(val version: Int) : PackVerdict
    class Damaged(val damage: Damage) : PackVerdict
}
