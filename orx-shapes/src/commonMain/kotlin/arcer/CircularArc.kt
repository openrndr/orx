package org.openrndr.extra.shapes.arcer

import org.openrndr.math.Vector2
import org.openrndr.shape.Segment2D
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

internal fun perpendicular(v: Vector2) = Vector2(-v.y, v.x)

internal fun rotate(v: Vector2, a: Double) = Vector2(v.x * cos(a) - v.y * sin(a), v.x * sin(a) + v.y * cos(a))

/** [direction] mirrored in the line along [chord]. */
internal fun reflect(direction: Vector2, chord: Vector2): Vector2 {
    val c = chord.normalized
    return c * (2.0 * direction.dot(c)) - direction
}

/**
 * The direction of travel at [p] on the circle through [a], [p] and [b], traversed from [a] via [p] to [b]. Falls back
 * to the direction from [p] to [b] for collinear points.
 */
internal fun circleTangent(a: Vector2, p: Vector2, b: Vector2): Vector2 {
    val u = p - a
    val v = b - p
    val cross = u.x * v.y - u.y * v.x
    if (abs(cross) <= 1E-12 * u.length * v.length) {
        return v.normalized
    }
    // circumcenter relative to a
    val w = b - a
    val d = 2.0 * (u.x * w.y - u.y * w.x)
    val uu = u.squaredLength
    val ww = w.squaredLength
    val center = a + Vector2(w.y * uu - u.y * ww, u.x * ww - w.x * uu) / d
    return perpendicular(p - center).normalized * if (cross > 0.0) 1.0 else -1.0
}

/**
 * An arc as Bézier [segments] with its [length], the direction in which it arrives, its [normal] at the start and,
 * unless it is a straight segment, its [center] and its [signedRadius] along that normal.
 */
internal class Arc(
    val segments: List<Segment2D>,
    val length: Double,
    val exit: Vector2,
    val normal: Vector2,
    val center: Vector2? = null,
    val signedRadius: Double = 0.0
) {
    /** The parallel arc at [distance] along the normal, which is this arc scaled around its center. */
    fun offset(distance: Double): List<Segment2D> = if (center == null) {
        segments.map { it.transform { p -> p + normal * distance } }
    } else {
        val scale = (signedRadius - distance) / signedRadius
        segments.map { it.transform { p -> center + (p - center) * scale } }
    }
}

internal fun Segment2D.transform(f: (Vector2) -> Vector2) = Segment2D(f(start), control.map(f), f(end))

/** The arc from [start] leaving in [tangent] to [end]. */
internal fun arc(start: Vector2, tangent: Vector2, end: Vector2): Arc {
    val chord = end - start
    val normal = perpendicular(tangent)
    val s = normal.dot(chord)
    if (abs(s) <= 1E-9 * chord.length) {
        return Arc(listOf(Segment2D(start, end)), chord.length, chord.normalized, perpendicular(chord.normalized))
    }
    // signed radius, the center lies on the normal at start and on the bisector of the chord
    val signedRadius = chord.squaredLength / (2.0 * s)
    val center = start + normal * signedRadius
    val radius = abs(signedRadius)
    val side = if (s > 0.0) 1.0 else -1.0
    val exit = reflect(tangent, chord)

    var angle = side * atan2(tangent.x * exit.y - tangent.y * exit.x, tangent.dot(exit))
    if (angle < 0.0) angle += 2 * PI

    val pieces = ceil(angle / (PI / 2.0) - 1E-9).toInt().coerceAtLeast(1)
    val step = angle / pieces
    val handle = 4.0 / 3.0 * tan(step / 4.0) * radius
    val result = mutableListOf<Segment2D>()
    var p0 = start
    var d0 = tangent
    for (j in 1..pieces) {
        val a = side * step * j
        val p1 = if (j == pieces) end else center + rotate(start - center, a)
        val d1 = if (j == pieces) exit else rotate(tangent, a)
        result.add(Segment2D(p0, p0 + d0 * handle, p1 - d1 * handle, p1))
        p0 = p1
        d0 = d1
    }
    return Arc(result, radius * angle, exit, normal, center, signedRadius)
}
