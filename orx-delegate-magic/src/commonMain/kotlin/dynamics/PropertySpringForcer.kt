@file:Suppress("PackageDirectoryMismatch")

package org.openrndr.extra.delegatemagic.dynamics

import org.openrndr.Program
import org.openrndr.math.LinearType
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0

class DoublePropertySpringForcer(
    private val program: Program,
    private val property: KProperty0<Double>,
    private val k: Double,
    private val kProperty: KProperty0<Double>?,
    private val decay: Double,
    private val decayProperty: KProperty0<Double>?

) {
    private var output: Double? = null
    private var lastTime: Double? = null
    private var velocity = 0.0
    operator fun getValue(any: Any?, property: KProperty<*>): Double {
        val k = kProperty?.get() ?: k
        val decay = decayProperty?.get() ?: decay

        val anchor = this.property.get()
        if (lastTime != null) {
            val dt = program.seconds - lastTime!!
            if (dt > 0.0) {
                val sfY = -k * (output!! - anchor)
                velocity = velocity * decay + sfY * dt * 10.0
                output = output!! + velocity * dt * 10.0
            }
        } else {
            output = this.property.get()
        }
        lastTime = program.seconds
        return output ?: error("no value")
    }
}

class LinearTypePropertySpringForcer<T : LinearType<T>>(
    private val program: Program,
    private val property: KProperty0<T>,
    private val k: Double,
    private val kProperty: KProperty0<Double>?,
    private val decay: Double,
    private val decayProperty: KProperty0<Double>?
) {
    private var output: T? = null
    private var lastTime: Double? = null
    private var velocity: T? = null
    operator fun getValue(any: Any?, property: KProperty<*>): T {
        val k = kProperty?.get() ?: k
        val decay = decayProperty?.get() ?: decay

        val anchor = this.property.get()
        if (lastTime != null) {
            val dt = program.seconds - lastTime!!
            if (dt > 0.0) {
                val sfY = (output!! - anchor) * -k

                velocity = if (velocity != null) {
                    velocity!! * decay + sfY * dt * 10.0
                } else {
                    sfY * dt * 10.0
                }
                output = output!! + velocity!! * dt * 10.0
            }
        } else {
            output = this.property.get()
        }
        lastTime = program.seconds
        return output ?: error("no value")
    }
}

/**
 * Create a property spring force delegate
 * @param property the property that is used as the spring anchor
 * @param k the spring stiffness
 * @param kProperty the spring stiffness property, overrides [k]
 * @param decay velocity decay, best to set to < 1
 * @param decayProperty velocity decay property, overrides [decay]
 * @since 0.4.3
 */
fun Program.springForcing(
    property: KProperty0<Double>,
    k: Double = 1.0,
    kProperty: KProperty0<Double>? = null,
    decay: Double = 0.9,
    decayProperty: KProperty0<Double>? = null
): DoublePropertySpringForcer {
    return DoublePropertySpringForcer(
        program = this,
        property = property,
        k = k,
        kProperty = kProperty,
        decay = decay,
        decayProperty = decayProperty
    )
}

/**
 * Create a property spring force delegate
 * @param property the property that is used as the spring anchor
 * @param k the spring stiffness
 * @param kProperty the spring stiffness property, overrides [k]
 * @param decay velocity decay, best to set to < 1
 * @param decayProperty velocity decay property, overrides [decay]
 * @since 0.4.3
 */
fun <T : LinearType<T>> Program.springForcing(
    property: KProperty0<T>,
    k: Double = 1.0,
    kProperty: KProperty0<Double>? = null,
    decay: Double = 0.9,
    decayProperty: KProperty0<Double>? = null
): LinearTypePropertySpringForcer<T> {
    return LinearTypePropertySpringForcer(
        program = this,
        property = property,
        k = k,
        kProperty = kProperty,
        decay = decay,
        decayProperty = decayProperty
    )
}