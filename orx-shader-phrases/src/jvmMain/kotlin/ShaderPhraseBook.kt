package org.openrndr.extra.shaderphrases

import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

/**
 * A book of shader phrases.
 */
actual open class ShaderPhraseBook actual constructor(val bookId: String) {
    private var registered = false
    /**
     * Registers all known shader phrases
     */
    actual fun register() {
        if (!registered) {
            this::class.declaredMemberProperties.filter {
                it.returnType.toString() == "org.openrndr.extra.shaderphrases.ShaderPhrase"
            }.map {
                @Suppress("UNCHECKED_CAST")
                val m = it as? KProperty1<ShaderPhraseBook, ShaderPhrase>
                m?.get(this)?.register(bookId)
            }
            registered = true
        }
    }
}