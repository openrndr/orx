package org.openrndr.extra.shapes.primitives

import org.openrndr.math.GeometricPrimitive2D
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.Line2D
import org.openrndr.shape.LineSegment
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt


/**
 * Performs circle inversion of a point.
 *
 * Circle inversion is a geometric transformation where a point is mapped to another point along the same ray from the center,
 * but at a distance that is inversely proportional to the original distance.
 *
 * The formula used is: P' = C + r²/|P-C|² * (P-C)
 * Where:
 * - P is the point to invert
 * - C is the center of the circle
 * - r is the radius of the circle
 * - P' is the inverted point
 *
 * @param point The point to invert
 * @return The inverted point
 */
fun Circle.invert(point: Vector2): Vector2 {
    // Vector from center to point
    val v = point - center

    // Distance from center to point
    val distanceSquared = v.squaredLength

    // If the point is at the center, we can't invert it
    if (distanceSquared < 1e-10) {
        throw IllegalArgumentException("Cannot invert a point at the center of the circle")
    }

    // Calculate the inverted point
    val factor = (radius * radius) / distanceSquared
    return center + v * factor
}

/**
 * Performs circle inversion of another circle.
 *
 * Circle inversion maps a circle to another circle (or a line, which can be considered a circle with infinite radius).
 *
 * There are several cases:
 * 1. If the circle to be inverted passes through the center of the inverting circle, the result is a line
 * 2. If the circle to be inverted doesn't contain the center of the inverting circle, the result is another circle
 * 3. If the circle to be inverted contains the center of the inverting circle, the result is also a circle
 *
 * @param circle The circle to invert
 * @return The inverted circle
 * @throws IllegalArgumentException if the circle to be inverted is centered at the center of the inverting circle
 */
fun Circle.invert(circle: Circle): GeometricPrimitive2D {
    // Vector from this circle's center to the other circle's center
    val v = circle.center - this.center

    // Distance between centers
    val distanceSquared = v.squaredLength

    // If the circle to be inverted is centered at the center of the inverting circle, we can't invert it
    if (distanceSquared < 1e-10) {
        throw IllegalArgumentException("Cannot invert a circle centered at the center of the inverting circle")
    }

    // Distance between centers
    val distance = sqrt(distanceSquared)

    // Check if the circle to be inverted passes through the center of the inverting circle
    if (abs(circle.radius - distance) < 1e-10) {
        // Special case: the result would be a line, which we can't represent as a Circle
        // We'll approximate it as a very large circle
        val direction = v.normalized
        return Line2D(this.center, direction)
    }

    // Calculate power of the point (center of the inverting circle) with respect to the circle being inverted
    // power = d² - r²
    val power = distanceSquared - circle.radius * circle.radius

    // Calculate the new center
    val newCenterFactor = (this.radius * this.radius) / power
    val newCenter = this.center + v * newCenterFactor

    // Calculate the new radius
    val newRadius = abs(this.radius * circle.radius / power) * distance

    return Circle(newCenter, newRadius)
}

/**
 * Performs conformal inversion of another circle.
 *
 * Conformal inversion is a special type of circle inversion that preserves tangency between circles.
 * If two circles are tangent, their images under conformal inversion will also be tangent.
 *
 * @param circle The circle to invert
 * @return The conformally inverted circle
 * @throws IllegalArgumentException if the circle to be inverted is centered at the center of the inverting circle
 */
fun Circle.invertConformal(circle: Circle): GeometricPrimitive2D {
    // Vector from this circle's center to the other circle's center
    val v = circle.center - this.center

    // Distance between centers
    val distanceSquared = v.squaredLength

    // If the circle to be inverted is centered at the center of the inverting circle, we can't invert it
    if (distanceSquared < 1e-10) {
        throw IllegalArgumentException("Cannot invert a circle centered at the center of the inverting circle")
    }

    // Distance between centers
    val distance = sqrt(distanceSquared)

    // Check if the circle to be inverted passes through the center of the inverting circle
    if (abs(circle.radius - distance) < 1e-10) {
        val direction = v.normalized
        return Line2D(this.center, direction)
    }

    // For conformal inversion that preserves tangency, we use the standard circle inversion formula
    // but with a specific calculation for the radius

    // Calculate power of the point (center of the inverting circle) with respect to the circle being inverted
    // power = d² - r²
    val power = distanceSquared - circle.radius * circle.radius

    // Calculate the new center
    val newCenterFactor = (this.radius * this.radius) / power
    val newCenter = this.center + v * newCenterFactor

    // Calculate the new radius for conformal inversion
    // This is the key difference from regular inversion - the formula preserves tangency
    val newRadius = abs(this.radius * this.radius * circle.radius / power)

    return Circle(newCenter, newRadius)
}

fun Circle.invert(segment: LineSegment): GeometricPrimitive2D {
    val a = segment.start
    val b = segment.end
    val c = segment.position(0.5)

    // Direction of the line (normalized)
    val dir = (b - a)
    val dirLen = dir.length
    if (dirLen < 1e-10) {
        // Degenerate segment: treat as a point inversion
        return invert(a)
    }
    val u = dir / dirLen

    // Foot of the perpendicular from circle center to the infinite line AB
    val ao = center - a
    val t = ao.dot(u)
    val foot = a + u * t

    val perpVec = center - foot
    val dist = perpVec.length

    // If the line passes through the center of inversion, it maps to a line
    if (dist < 1e-10) {
        val aInv = invert(a)
        val bInv = invert(b)
        return LineSegment(aInv, bInv)
    }

    // Inverse of a line not through the center is a circle passing through the center
    val rPrime = (radius * radius) / (2.0 * dist)
    val n = (foot - center).normalized // direction from center towards the line
    val circleCenter = center + n * rPrime

    // The circle radius equals rPrime (since it passes through the center)
    val aInv = invert(a)
    val bInv = invert(b)
    val cInv = invert(c)

    // Compute angles (in degrees) for the arc between inverted endpoints
    val angleA = atan2(aInv.y - circleCenter.y, aInv.x - circleCenter.x) * 180.0 / kotlin.math.PI
    val angleB = atan2(bInv.y - circleCenter.y, bInv.x - circleCenter.x) * 180.0 / kotlin.math.PI
    val angleC = atan2(cInv.y - circleCenter.y, cInv.x - circleCenter.x) * 180.0 / kotlin.math.PI

    var angle0 = min(angleA, angleB)
    var angle1 = max(angleA, angleB)

    if (angleC in angle0..angle1) {
        angle1 -= 360.0
    }


    return Arc(circleCenter, rPrime, angle0, angle1).conjugate()

}
