package org.openrndr.extra.shapes.primitives

import org.openrndr.math.LinearType
import org.openrndr.math.Polar
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.LineSegment
import org.openrndr.shape.ShapeContour

/**
 * Represents a "Tear" consisting of a point and a circle.
 *
 * This class allows operations such as addition, subtraction, scaling, and division,
 * which are defined element-wise for the point and circle components of the Tear.
 * Additionally, it provides a computed property that generates a closed shape contour
 * based on the geometry of the Tear.
 *
 * @property point The [Vector2] coordinate representing a point in the Tear.
 * @property circle The [Circle] geometry associated with the Tear.
 */
class Tear(val point: Vector2, val circle: Circle) : LinearType<Tear> {
    override fun div(scale: Double) = Tear(point / scale, circle / scale)

    override fun times(scale: Double) = Tear(point * scale, circle * scale)

    override fun plus(right: Tear) = Tear(point + right.point, circle + right.circle)

    override fun minus(right: Tear) = Tear(point - right.point, circle - right.circle)

    val contour: ShapeContour
        get() {
            val tangents = circle.tangents(point)
            var k = LineSegment(point, tangents.first).contour
            run {
                val th0 = Polar.fromVector(tangents.first - circle.center).theta
                var th1 = Polar.fromVector(tangents.second - circle.center).theta
                if (th1 < th0) th1 += 360.0
                k += Arc(circle.center, circle.radius, th0, th1).contour
            }
            k += LineSegment(tangents.second, point).contour
            return k.close()
        }
}