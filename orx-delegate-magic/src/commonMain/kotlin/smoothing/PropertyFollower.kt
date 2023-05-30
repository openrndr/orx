@file:Suppress("PackageDirectoryMismatch")

package org.openrndr.extra.delegatemagic.smoothing

import org.openrndr.Clock
import org.openrndr.math.EuclideanVector
import org.openrndr.math.LinearType
import org.openrndr.math.clamp
import org.openrndr.math.map
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0

class DoublePropertyFollower(
    private val clock: Clock,
    private val property: KProperty0<Double>,
    private val maxAccel: Double,
    private val maxAccelProperty: KProperty0<Double>?,
    private val maxSpeed: Double,
    private val maxSpeedProperty: KProperty0<Double>?,
    private val dampDist: Double,
    private val dampDistProperty: KProperty0<Double>?
) {
    private var current: Double? = null
    private var lastTime: Double? = null
    private var velocity = 0.0
    operator fun getValue(any: Any?, property: KProperty<*>): Double {
        if (lastTime != null) {
            val dt = clock.seconds - lastTime!!
            if (dt > 1E-10) {
                val maxAccel = maxAccelProperty?.get() ?: maxAccel
                val maxSpeed = maxSpeedProperty?.get() ?: maxSpeed
                val dampDist = dampDistProperty?.get() ?: dampDist

                var offset = this.property.get() - current!!
                val len = abs(offset)
                val dist = min(dampDist, len) // 0.0 .. dampDist

                // convert dist to desired speed
                offset = offset.sign *
                        dist.map(0.0, dampDist, 0.0, maxSpeed)

                val acceleration = clamp(
                    offset - velocity,
                    -maxAccel, maxAccel
                )

                velocity = clamp(
                    velocity + acceleration,
                    -maxSpeed, maxSpeed
                )

                current = current!! + velocity
            }
        } else {
            current = this.property.get()
        }
        lastTime = clock.seconds
        return current ?: error("no value")
    }
}

class PropertyFollower<T>(
    private val clock: Clock,
    private val property: KProperty0<T>,
    private val maxAccel: Double,
    private val maxAccelProperty: KProperty0<Double>?,
    private val maxSpeed: Double,
    private val maxSpeedProperty: KProperty0<Double>?,
    private val dampDist: Double,
    private val dampDistProperty: KProperty0<Double>?
) where T : LinearType<T>, T : EuclideanVector<T> {
    private var current: T? = null
    private var lastTime: Double? = null
    private var velocity = property.get().zero
    operator fun getValue(any: Any?, property: KProperty<*>): T {
        if (lastTime != null) {
            val dt = clock.seconds - lastTime!!
            if (dt > 1E-10) {
                val maxAccel = maxAccelProperty?.get() ?: maxAccel
                val maxSpeed = maxSpeedProperty?.get() ?: maxSpeed
                val dampDist = dampDistProperty?.get() ?: dampDist

                var offset = this.property.get() - current!!
                val len = offset.length
                val dist = min(dampDist, len) // 0.0 .. dampDist

                // convert dist to desired speed
                offset = offset.normalized *
                        dist.map(0.0, dampDist, 0.0, maxSpeed)

                var acceleration = offset - velocity
                if (acceleration.length > maxAccel) {
                    acceleration = acceleration.normalized * maxAccel
                }

                velocity += acceleration
                if (velocity.length > maxSpeed) {
                    velocity = velocity.normalized * maxSpeed
                }

                current = current!! + velocity
            }
        } else {
            current = this.property.get()
        }
        lastTime = clock.seconds
        return current ?: error("no value")
    }
}

/**
 * Create a property follower delegate
 * @param property the property to smooth
 * @param cfg the simulation parameters
 * @since 0.4.3
 */
fun Clock.following(
    property: KProperty0<Double>,
    maxAccel: Double = 0.1,
    maxAccelProperty: KProperty0<Double>? = null,
    maxSpeed: Double = 10.0,
    maxSpeedProperty: KProperty0<Double>? = null,
    dampDist: Double = 400.0,
    dampDistProperty: KProperty0<Double>? = null
) = DoublePropertyFollower(
    clock = this,
    property = property,
    maxAccel = maxAccel,
    maxAccelProperty = maxAccelProperty,
    maxSpeed = maxSpeed,
    maxSpeedProperty = maxSpeedProperty,
    dampDist = dampDist,
    dampDistProperty = dampDistProperty
)

/**
 * Create a property follower delegate
 * @param property the property to smooth
 * @param cfg the simulation parameters
 * @since 0.4.3
 */
fun <T> Clock.following(
    property: KProperty0<T>,
    maxAccel: Double = 0.1,
    maxAccelProperty: KProperty0<Double>? = null,
    maxSpeed: Double = 10.0,
    maxSpeedProperty: KProperty0<Double>? = null,
    dampDist: Double = 400.0,
    dampDistProperty: KProperty0<Double>? = null
) where T : LinearType<T>, T : EuclideanVector<T> =
    PropertyFollower(
        clock = this,
        property = property,
        maxAccel = maxAccel,
        maxAccelProperty = maxAccelProperty,
        maxSpeed = maxSpeed,
        maxSpeedProperty = maxSpeedProperty,
        dampDist = dampDist,
        dampDistProperty = dampDistProperty
    )
