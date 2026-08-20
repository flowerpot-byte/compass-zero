package org.compasszero.security

class TrustedKey(name: String, publicKey: ByteArray) {

    val name: String = name.trim()
    private val key: ByteArray = publicKey.copyOf()

    // Kopie nach aussen: ein vertrauenswuerdiger Schluessel darf sich zur Laufzeit
    // nicht mehr aendern lassen, auch nicht versehentlich.
    val publicKey: ByteArray get() = key.copyOf()

    init {
        require(this.name.isNotEmpty()) { "key name must not be blank" }
        require(key.size == PackFormat.KEY_SIZE) { "public key must be ${PackFormat.KEY_SIZE} bytes" }
    }

    internal fun matches(other: ByteArray): Boolean = key.contentEquals(other)
}

class TrustStore(keys: List<TrustedKey>) {

    private val keys: List<TrustedKey> = keys.toList()

    init {
        for (i in this.keys.indices) {
            for (j in i + 1 until this.keys.size) {
                require(!this.keys[i].matches(this.keys[j].publicKey)) {
                    "duplicate public key: ${this.keys[i].name} / ${this.keys[j].name}"
                }
            }
        }
    }

    fun find(publicKey: ByteArray): TrustedKey? = keys.firstOrNull { it.matches(publicKey) }
}
