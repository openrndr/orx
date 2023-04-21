@file:Suppress("PackageDirectoryMismatch")

package org.openrndr.extra.delegatemagic.smoothing

import org.openrndr.Program
import org.openrndr.math.LinearType
import kotlin.math.pow
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0

class DoublePropertySmoother(
    private val program: Program,
    private val property: KProperty0<Double>,
    private val factor: Double = 0.99,
    private val factorProperty: KProperty0<Double>?
) {
    private var output: Double? = null
    private var lastTime: Double? = null
    operator fun getValue(any: Any?, property: KProperty<*>): Double {
        if (lastTime != null) {
            val dt = program.seconds - lastTime!!
            if (dt > 1E-10) {
                val steps = dt * 60.0
                val ef = (factorProperty?.get() ?: factor).pow(steps)
                output = output!! * ef + this.property.get() * (1.0 - ef)
            }
        } else {
            output = this.property.get()
        }
        lastTime = program.seconds
        return output ?: error("no value")
    }
}

class PropertySmoother<T : LinearType<T>>(
    private val program: Program,
    private val property: KProperty0<T>,
    private val factor: Double = 0.99,
    private val factorProperty: KProperty0<Double>?
) {
    private var output: T? = null
    private var lastTime: Double? = null
    operator fun getValue(any: Any?, property: KProperty<*>): T {
        if (lastTime != null) {
            val dt = program.seconds - lastTime!!
            if (dt > 1E-10) {
                val steps = dt * 60.0
                val ef = (factorProperty?.get() ?: factor).pow(steps)

                val target = this.property.get()
                output = output!! * ef + target * (1.0 - ef)
            }
        } else {
            output = this.property.get()
        }
        lastTime = program.seconds
        return output ?: error("no value")
    }
}

/**
 * Create a property smoother delegate
 * @param property the property to smooth
 * @param factor the smoothing factor
 * @since 0.4.3
 */
fun Program.smoothing(property: KProperty0<Double>, factor: Double = 0.99): DoublePropertySmoother {
    return DoublePropertySmoother(this, property, factor, null)
}

/**
 * Create a property smoother delegate
 * @param property the property to smooth
 * @param factor the smoothing factor property
 * @since 0.4.3
 */
fun Program.smoothing(
    property: KProperty0<Double>,
    factor: KProperty0<Double>
): DoublePropertySmoother {
    return DoublePropertySmoother(this, property, 1E10, factor)
}

/**
 * Create a property smoother delegate
 * @param property the property to smooth
 * @param factor the smoothing factor
 * @since 0.4.3
 */
fun <T : LinearType<T>> Program.smoothing(property: KProperty0<T>, factor: Double = 0.99): PropertySmoother<T> {
    return PropertySmoother(this, property, factor, null)
}

/**
 * Create a property smoother delegate
 * @param property the property to smooth
 * @param factor the smoothing factor property
 * @since 0.4.3
 */
fun <T : LinearType<T>> Program.smoothing(property: KProperty0<T>, factor: KProperty0<Double>): PropertySmoother<T> {
    return PropertySmoother(this, property, 1E10, factor)
}