package org.openrndr.extra.shapes.primitives

import org.openrndr.math.Polar
import org.openrndr.math.Vector2
import org.openrndr.math.asDegrees
import org.openrndr.shape.Circle
import org.openrndr.shape.Circle.Companion.INVALID
import kotlin.math.acos
import kotlin.math.sqrt

/**
 * Calculates the tangent lines between this circle and another circle.
 *
 * Depending on the value of [isInner], the method computes either the inner
 * tangents or outer tangents between the two circles.
 *
 * @param other The other circle with which to calculate tangents.
 * @param isInner If true, computes inner tangents where the tangents go
 * through the region between the two circles. If false (default), computes
 * outer tangents.
 * @return A list of pairs of points where each pair represents a tangent line.
 * Each pair contains one point on this circle and the corresponding point on the other circle.
 * Returns an empty list if no valid tangent lines exist between the circles.
 */
fun Circle.tangents(other: Circle, isInner: Boolean = false): List<Pair<Vector2, Vector2>> {
    if (this == INVALID || other == INVALID) {
        return listOf()
    }

    val distSq = center.squaredDistanceTo(other.center)

    if (isInner) {
        if (sqrt(distSq) <= radius + other.radius) {
            return listOf() // circles too close
        }
    } else {
        val rDiff = radius - other.radius
        if (distSq <= rDiff * rDiff) {
            return listOf() // nested circles
        }
    }

    val otherRadiusSigned = if (isInner) -other.radius else other.radius
    val hyp = other.center - center // hypotenuse
    val adj = radius - otherRadiusSigned // adjacent
    val a = hyp * adj
    val b = hyp.perpendicular() * sqrt(distSq - adj * adj)
    val v1 = (a - b) / distSq
    val v2 = (a + b) / distSq

    return listOf(
        Pair(center + v1 * radius, other.center + v1 * otherRadiusSigned),
        Pair(center + v2 * radius, other.center + v2 * otherRadiusSigned)
    )
}

/**
 * Computes the tangent points from a given external point to a circle.
 *
 * If the circle is invalid (e.g., has an undefined radius or center),
 * the function returns a pair of infinite vectors.
 *
 * @param point The external point from which tangents to the circle are calculated.
 * @return A pair of [Vector2] representing the two points on the circle
 * where the tangents from the given external point touch the circle.
 */
fun Circle.tangents(point: Vector2): Pair<Vector2, Vector2> {
    if (this == INVALID) {
        return Pair(Vector2.INFINITY, Vector2.INFINITY)
    }
    val v = Polar.fromVector(point - center)
    val b = v.radius
    val theta = (acos(radius / b)).asDegrees
    val d1 = v.theta + theta
    val d2 = v.theta - theta

    val tp = center + Polar(d1, radius).cartesian
    val tp2 = center + Polar(d2, radius).cartesian

    return Pair(tp, tp2)
}
