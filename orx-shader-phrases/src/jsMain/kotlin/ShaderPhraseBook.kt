package org.openrndr.extra.shaderphrases

/**
 * A book of shader phrases.
 */
actual open class ShaderPhraseBook actual constructor(val bookId: String) {
    private var registered = false
    /**
     * Registers all known shader phrases
     */
    actual fun register() {
        error("not supported")
    }

}