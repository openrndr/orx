@file:Suppress("PackageDirectoryMismatch")

package org.openrndr.extra.delegatemagic.tracking

import org.openrndr.Program
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0

class PropertyTracker<T>(private val program: Program, private val property: KProperty0<T>, val length: Int = 30) {
    private val track = mutableListOf<T>()
    private var lastTime: Double? = null

    operator fun getValue(any: Any?, property: KProperty<*>): List<T> {
        if (lastTime != null) {
            val dt = program.seconds - lastTime!!
            if (dt > 1E-10) {
                track.add(this.property.get())
            }
        } else {
            track.add(this.property.get())
        }
        if (track.size > length) {
            track.removeAt(0)
        }
        lastTime = program.seconds
        return track
    }
}

/**
 * Create a property tracker
 * @param property the property to track
 * @param length the maximum length of the tracked history
 * @return a property tracker
 * @since 0.4.3
 */
fun <T> Program.tracking(property: KProperty0<T>, length: Int = 30): PropertyTracker<T> {
    return PropertyTracker(this, property, length)
}