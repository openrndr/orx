package org.openrndr.extra.shapes

import org.openrndr.math.LinearType
import org.openrndr.math.Polar
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.LineSegment
import org.openrndr.shape.ShapeContour

class Net(val point0: Vector2, val point1: Vector2, val circle: Circle) : LinearType<Net> {
    override fun div(scale: Double) = Net(point0 / scale, point1 / scale, circle / scale)

    override fun times(scale: Double) = Net(point0 * scale, point1 * scale, circle * scale)

    override fun plus(right: Net) = Net(point0 + right.point0, point1 + right.point1, circle + right.circle)

    override fun minus(right: Net) = Net(point0 - right.point0, point1 - right.point1, circle - right.circle)

    val contour: ShapeContour
        get() {
            val tangents0 = circle.tangents(point0)
            val tangents1 = circle.tangents(point1)
            var k = LineSegment(point0, tangents0.first).contour
            run {
                val th0 = Polar.fromVector(tangents0.first - circle.center).theta
                var th1 = Polar.fromVector(tangents1.second - circle.center).theta
                if (th1 < th0) th1 += 360.0
                k += Arc(circle.center, circle.radius, th0, th1).contour
            }
            k += LineSegment(tangents1.second, point1).contour
            return k
        }
}