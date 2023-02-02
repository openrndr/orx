package org.openrndr.extra.shapes

import org.openrndr.math.*
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour

/**
 * A circular arc
 */
class Arc(val center: Vector2, val radius: Double, val angle0: Double, val angle1: Double) : LinearType<Arc> {
    fun position(t: Double): Vector2 {
        val angle = mix(angle0, angle1, t.clamp(0.0, 1.0))
        return Polar(angle, radius).cartesian + center
    }

    val contour: ShapeContour
        get() {
            return contour {
                moveTo(position(0.0))
                circularArcTo(position(0.5), position(1.0))
            }
        }

    override fun div(scale: Double) = Arc(center / scale, radius / scale, angle0 / scale, angle1 / scale)

    override fun times(scale: Double) = Arc(center * scale, radius * scale, angle0 * scale, angle1 * scale)

    override fun plus(right: Arc) =
        Arc(center + right.center, radius + right.radius, angle0 + right.angle0, angle1 + right.angle1)

    override fun minus(right: Arc) =
        Arc(center - right.center, radius - right.radius, angle0 - right.angle0, angle1 - right.angle1)
}
