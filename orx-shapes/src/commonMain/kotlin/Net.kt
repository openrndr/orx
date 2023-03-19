package org.openrndr.extra.shapes

import org.openrndr.math.LinearType
import org.openrndr.math.Polar
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.LineSegment
import org.openrndr.shape.ShapeContour

class Net(val point0: Vector2, val point1: Vector2, val circle: Circle) :
    LinearType<Net> {
    override fun div(scale: Double) =
        Net(point0 / scale, point1 / scale, circle / scale)

    override fun times(scale: Double) =
        Net(point0 * scale, point1 * scale, circle * scale)

    override fun plus(right: Net) =
        Net(point0 + right.point0, point1 + right.point1, circle + right.circle)

    override fun minus(right: Net) =
        Net(point0 - right.point0, point1 - right.point1, circle - right.circle)

    /**
     * Creates a [ShapeContour] with three segments: two [LineSegment] and one [Arc].
     * These three components form a contour that resemble a string starting
     * at [point0], wrapping around the [circle] and ending at [point1].
     * If one of the points is inside the circle only a line segment tangent
     * to the circle that starts at the other point is returned. If both
     * points are inside the circle an empty contour is returned.
     */
    val contour: ShapeContour
        get() {
            val p0Inside = circle.contains(point0)
            val p1Inside = circle.contains(point1)

            return when {
                !p0Inside && !p1Inside -> {
                    val tangents0 = circle.tangents(point0)
                    val tangents1 = circle.tangents(point1)

                    val th0 =
                        Polar.fromVector(tangents0.first - circle.center).theta
                    val th1 =
                        Polar.fromVector(tangents1.second - circle.center).theta

                    LineSegment(point0, tangents0.first).contour +
                            Arc(
                                circle.center,
                                circle.radius,
                                th0,
                                if (th1 < th0) th1 + 360.0 else th1
                            ).contour +
                            LineSegment(tangents1.second, point1).contour
                }

                p0Inside ->
                    LineSegment(circle.tangents(point1).second, point1).contour

                p1Inside ->
                    LineSegment(circle.tangents(point0).first, point0).contour

                else ->
                    ShapeContour.EMPTY
            }
        }
}