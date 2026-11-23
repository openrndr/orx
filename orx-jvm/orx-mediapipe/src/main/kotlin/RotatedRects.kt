package org.openrndr.extra.mediapipe

import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * A rectangle around a face, a hand or a body in the image, turned by [rotation] radians, in which the landmarks are
 * found
 */
internal class RotatedRect(val center: Vector2, val width: Double, val height: Double, val rotation: Double) {
    /** the bounds of the turned rectangle */
    val bounds: Rectangle
        get() {
            val c = abs(cos(rotation))
            val s = abs(sin(rotation))
            val hw = (width * c + height * s) / 2.0
            val hh = (width * s + height * c) / 2.0
            return Rectangle(center.x - hw, center.y - hh, 2.0 * hw, 2.0 * hh)
        }
}

/** a square around [center] with sides of [side], turned by [rotation] radians */
internal fun square(center: Vector2, side: Double, rotation: Double) = RotatedRect(center, side, side, rotation)

/** [angle] in -π to π */
internal fun normalizedRadians(angle: Double) = angle - 2.0 * PI * floor((angle + PI) / (2.0 * PI))

/**
 * The turn that makes the direction from [from] to [to] point at [target] radians, counterclockwise from the right,
 * like MediaPipe's rotation vectors
 */
internal fun rotationTo(from: Vector2, to: Vector2, target: Double) = normalizedRadians(target - atan2(-(to.y - from.y), to.x - from.x))

/** the turn that makes the direction from [from] to [to] point up */
internal fun uprightRotation(from: Vector2, to: Vector2) = rotationTo(from, to, PI / 2.0)
