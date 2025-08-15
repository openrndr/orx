package org.openrndr.extra.shapes.primitives

import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import kotlin.math.abs
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
fun Circle.invert(circle: Circle): Circle {
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
        val farPoint = this.center + direction * 1e6
        return Circle(farPoint, 1e6)
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
fun Circle.invertConformal(circle: Circle): Circle {
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
        val farPoint = this.center + direction * 1e6
        return Circle(farPoint, 1e6)
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