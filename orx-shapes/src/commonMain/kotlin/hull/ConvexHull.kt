package org.openrndr.extra.shapes.hull

import org.openrndr.math.Vector2
import org.openrndr.shape.ShapeContour

/**
 * Computes the convex hull of a list of 2D points using the Monotone chain algorithm.
 * The convex hull is the smallest convex shape that encompasses all the points in the input list.
 *
 * The input list should contain at least three distinct points for a meaningful convex hull to be formed.
 * If the input list is empty or contains fewer than three points, a simplified contour is returned.
 * The input list is processed to remove duplicate points and sort them before constructing the hull.
 *
 * @receiver A list of 2D points represented as `Vector2`.
 * @return A `ShapeContour` that represents the convex hull of the points.
 *         If the input list is empty, `ShapeContour.EMPTY` is returned.
 *         If the input list contains fewer than three points after deduplication, a simple contour
 *         connecting the points is returned.
 */
fun List<Vector2>.convexHull(): ShapeContour {
    if (this.isEmpty()) {
        return ShapeContour.EMPTY
    }
    if (this.size < 3) {
        return ShapeContour.fromPoints(this, true)
    }

    val sorted = this.distinct().sortedWith(compareBy({ it.x }, { it.y }))

    if (sorted.size < 3) {
        return ShapeContour.fromPoints(sorted, true)
    }

    fun crossProduct(a: Vector2, b: Vector2, c: Vector2): Double {
        return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)
    }

    val lower = mutableListOf<Vector2>()
    for (p in sorted) {
        while (lower.size >= 2 && crossProduct(lower[lower.size - 2], lower.last(), p) <= 0) {
            lower.removeAt(lower.size - 1)
        }
        lower.add(p)
    }

    val upper = mutableListOf<Vector2>()
    for (i in sorted.indices.reversed()) {
        val p = sorted[i]
        while (upper.size >= 2 && crossProduct(upper[upper.size - 2], upper.last(), p) <= 0) {
            upper.removeAt(upper.size - 1)
        }
        upper.add(p)
    }

    lower.removeAt(lower.size - 1)
    upper.removeAt(upper.size - 1)

    val hull = lower + upper
    if (hull.isEmpty()) {
        return ShapeContour.EMPTY
    }
    return ShapeContour.fromPoints(hull, true)
}

/**
 * Computes the convex hull of a list of 2D points and returns the vertices of the hull as a set.
 * The convex hull is the smallest convex polygon that can enclose all points in the input list.
 *
 * This method uses the `convexHull` function to calculate the hull and extracts the starting points
 * of the contour segments to represent the vertices of the hull.
 *
 * @receiver A list of 2D points represented as `Vector2`.
 * @return A set of `Vector2` representing the vertices of the computed convex hull.
 *         If the input list is empty or has fewer than three unique points, the resulting set will
 *         represent a simplified contour matching the input points.
 */
fun List<Vector2>.convexHullSet(): Set<Vector2> {
    return convexHull().segments.map { it.start }.toSet()
}