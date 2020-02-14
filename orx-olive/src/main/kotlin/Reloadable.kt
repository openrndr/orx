package org.openrndr.extra.olive

import mu.KotlinLogging
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.jvmName

private val logger = KotlinLogging.logger {}

private val store = mutableMapOf<String, Any>()

/**
 * Clear reloadable values
 */
fun clearReloadables() {
    store.clear()
}

/**
 * A class with which persistent state can be reloaded from inside Olive scripts.
 */
open class Reloadable {
    private fun normalizeClassName(name: String): String {
        return name.replace(Regex("ScriptingHost[0-9a-f]+_"), // -- since kotlin 1.3.61 the scripting host prepends class names with the host id
                "").replace(Regex("Line_[0-9]+"),"") // -- when reusing the script engine the line number increments.
    }

    /**
     * reload property values from store
     */
    @Suppress("UNCHECKED_CAST")
    fun reload() {
        val className = normalizeClassName(this::class.jvmName)
        val existing = store[className]
        if (existing != null) {
            for (p in this::class.declaredMemberProperties) {
                val e = existing::class.declaredMemberProperties.find { it.name == p.name }
                if (e != null) {
                    try {
                        val value = (e as KProperty1<Any, Any?>).get(existing)
                        val mp = (p as KMutableProperty1<Any, Any?>)
                        mp.set(this, value as Any)
                        logger.info("reloaded property ${p.name} <- ${value}")
                    } catch (e: Throwable) {
                        logger.warn("error while reloading property ${p.name}: ${e.message}")
                    }
                }
            }
        } else {
            logger.info("no existing store found for $className")
        }
        store[normalizeClassName(this::class.jvmName)] = this
    }
}
