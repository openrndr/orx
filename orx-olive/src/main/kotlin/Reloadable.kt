package org.openrndr.extra.olive

import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.jvmName

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
    /**
     * reload property values from store
     */
    @Suppress("UNCHECKED_CAST")
    fun reload() {
        val existing = store[this::class.jvmName]
        if (existing != null) {
            for (p in this::class.declaredMemberProperties) {
                val e = existing::class.declaredMemberProperties.find { it.name == p.name }
                if (e != null) {
                    try {
                        val value = (e as KProperty1<Any, Any?>).get(existing)
                        val mp = (p as KMutableProperty1<Any, Any?>)
                        mp.set(this, value as Any)
                        println("reloaded property ${p.name} <- ${value}")
                    } catch (e: Throwable) {
                        println("error while reloading property ${p.name}: ${e.message}")
                    }
                }
            }
        }
        store[this::class.jvmName] = this
    }
}
