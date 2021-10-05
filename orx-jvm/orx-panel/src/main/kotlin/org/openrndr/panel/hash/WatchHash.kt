package org.openrndr.panel.hash

import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

fun watchHash(toHash: Any): Int {
    var hash = 0
    for (property in toHash::class.declaredMemberProperties) {
        val v = ((property as KProperty1<Any, Any?>).getter).invoke(toHash)
        if (v is KProperty0<*>) {
            val pv = v.get()
            hash = 31 * hash + (pv?.hashCode() ?: 0)
        } else {
            hash = 31 * hash + (v?.hashCode() ?: 0)
        }
    }
    return hash
}

