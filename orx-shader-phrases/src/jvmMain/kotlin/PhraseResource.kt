package org.openrndr.extra.shaderphrases

import org.openrndr.resourceUrl
import java.net.URL
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty


/**
 * PhraseResource can be used as a delegate
 */
class PhraseResource<R>(private val resourceUrl: String) : ReadOnlyProperty<R, String> {
    override fun getValue(thisRef: R, property: KProperty<*>): String {
        return URL(resourceUrl).readText()
    }
}

/**
 *
 * PhraseResource delegate builder function
 */
fun phraseResource(resource: String) : PhraseResource<Any?> {
    return PhraseResource(resourceUrl(resource))
}
