package org.openrndr.extra.shapes.primitives

import org.openrndr.math.GeometricPrimitive2D
import org.openrndr.math.LinearType
import org.openrndr.math.Polar
import org.openrndr.math.Vector2
import org.openrndr.math.asRadians
import org.openrndr.math.clamp
import org.openrndr.math.mix
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour
import kotlin.jvm.JvmRecord
import kotlin.math.absoluteValue

/**
 * Represents a sector defined by a center point, a radius, and a range of angles.
 *
 * @property center The center point of the sector.
 * @property radius The radius of the sector.
 * @property angle0 The starting angle of the sector, in degrees.
 * @property angle1 The ending angle of the sector, in degrees.
 */
@JvmRecord
data class Sector(val center: Vector2, val radius: Double, val angle0: Double, val angle1: Double) : LinearType<Sector>,
    GeometricPrimitive2D {
    /**
     * Calculates the position of a point along the arc at a specified parameter `t`.
     * The parameter `t` interpolates between the starting and ending angles of the arc.
     *
     * @param t A parameter ranging from 0.0 to 1.0, where 0.0 corresponds to the starting point of the arc
     * and 1.0 corresponds to the ending point of the arc.
     * @return The position of the point on the arc as a [Vector2].
     */
    fun arcPosition(t: Double): Vector2 {
        val angle = mix(angle0, angle1, t.clamp(0.0, 1.0))
        return Polar(angle, radius).cartesian + center
    }

    /**
     * The area of the sector.
     */
    val area get() = 0.5 * radius * radius * (angle1 - angle0).asRadians.absoluteValue

    /**
     * The perimeter of the sector, including its two sides and the arc.
     */
    val perimeter get() = radius * (angle1 - angle0).asRadians.absoluteValue + 2 * radius

    /**
     * Returns the conjugate of this sector: the complementary portion of the circle
     * starting where this arc ends and ending where this arc starts.
     * Together, a sector and its conjugate form a full circle.
     */
    fun conjugate() = Sector(center, radius, angle1 - 360.0, angle0)

    /**
     * A computed property that provides a closed [ShapeContour]
     * representation of the sector, connecting both ends of an arc
     * to its [center].
     */
    val contour: ShapeContour
        get() {
            return contour {
                moveTo(center)
                lineTo(arcPosition(0.0))
                circularArcTo(arcPosition(0.5), arcPosition(1.0))
                close()
            }
        }

    override fun div(scale: Double) = Sector(center / scale, radius / scale, angle0 / scale, angle1 / scale)

    override fun times(scale: Double) = Sector(center * scale, radius * scale, angle0 * scale, angle1 * scale)

    override fun plus(right: Sector) =
        Sector(center + right.center, radius + right.radius, angle0 + right.angle0, angle1 + right.angle1)

    override fun minus(right: Sector) =
        Sector(center - right.center, radius - right.radius, angle0 - right.angle0, angle1 - right.angle1)
}
