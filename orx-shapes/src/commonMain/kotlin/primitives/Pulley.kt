package org.openrndr.extra.shapes.primitives

import org.openrndr.math.GeometricPrimitive2D
import org.openrndr.math.LinearType
import org.openrndr.math.Polar
import org.openrndr.shape.Circle
import org.openrndr.shape.LineSegment
import org.openrndr.shape.ShapeContour
import kotlin.jvm.JvmRecord

/**
 * Represents a pulley system defined by two circles.
 *
 * This class models a geometric structure where two circles are connected with
 * tangential lines, forming a pulley-like system. The `Pulley` class provides
 * operations for scaling, addition, and subtraction, as well as generating
 * a contour that represents the shape of the pulley.
 *
 * @property circle0 The first circle in the pulley system.
 * @property circle1 The second circle in the pulley system.
 */
@JvmRecord
data class Pulley(val circle0: Circle, val circle1: Circle) : LinearType<Pulley>, GeometricPrimitive2D {
    override fun div(scale: Double): Pulley {
        return Pulley(circle0 / scale, circle1 / scale)
    }

    override fun times(scale: Double): Pulley {
        return Pulley(circle0 * scale, circle1 * scale)
    }

    override fun plus(right: Pulley): Pulley {
        return Pulley(circle0 + right.circle0, circle1 + right.circle1)
    }

    override fun minus(right: Pulley): Pulley {
        return Pulley(circle0 - right.circle0, circle1 - right.circle1)
    }

    val contour: ShapeContour
        get() {
            val tangents = circle0.tangents(circle1)
            if (tangents.isEmpty()) {
                return ShapeContour.EMPTY
            } else {
                var k = LineSegment(tangents[0].first, tangents[0].second).contour

                run {
                    var th0 = Polar.fromVector(tangents[0].second - circle1.center).theta
                    val th1 = Polar.fromVector(tangents[1].second - circle1.center).theta
                    if (th0 < th1) th0 += 360.0
                    k += Arc(circle1.center, circle1.radius, th0, th1).contour
                }
                k += LineSegment(tangents[1].first, tangents[1].second).contour.reversed
                run {
                    val th0 = Polar.fromVector(tangents[0].first - circle0.center).theta
                    var th1 = Polar.fromVector(tangents[1].first - circle0.center).theta
                    if (th0 > th1) th1 += 360.0
                    k += Arc(circle0.center, circle0.radius, th0, th1).contour.reversed
                }
                return k.close()
            }
        }
}