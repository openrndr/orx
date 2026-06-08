package org.openrndr.extra.shapes.polygon

import org.openrndr.math.Vector2
import org.openrndr.shape.bounds

fun Polygon2D.intersects(other: Polygon2D, ignoreAdjacent: Boolean = false): Boolean {
    val bounds = bounds
    val otherBounds = other.bounds

    if (!bounds.intersects(otherBounds))
        return false

    // check if there are pairs of intersecting edges
    for (i in points.indices) {
        val a0 = points[i]
        val a1 = points[(i + 1) % points.size]

        for (j in other.points.indices) {
            val b0 = other.points[j]
            val b1 = other.points[(j + 1) % other.points.size]

            if (segmentsIntersect(a0, a1, b0, b1)) {
                if (ignoreAdjacent) {
                    val ccw1 = ccw(a0, a1, b0)
                    val ccw2 = ccw(a0, a1, b1)
                    val ccw3 = ccw(b0, b1, a0)
                    val ccw4 = ccw(b0, b1, a1)

                    val collinear = ccw1 == 0 && ccw2 == 0 && ccw3 == 0 && ccw4 == 0
                    if (collinear) {
                        continue
                    }

                    val sharedVertex = a0.distanceTo(b0) < 1E-8 || a0.distanceTo(b1) < 1E-8 ||
                            a1.distanceTo(b0) < 1E-8 || a1.distanceTo(b1) < 1E-8

                    if (sharedVertex) {
                        continue
                    }

                    if (ccw1 == 0 || ccw2 == 0 || ccw3 == 0 || ccw4 == 0) {
                        // T-junction: one endpoint lies on the other segment.
                        // This is considered "adjacent".
                        continue
                    }
                }
                return true
            }
        }
    }

    // if there are no intersecting edges, check if either polygon fully contains the other polygon
    // but we should avoid returning true if they only touch at boundaries when ignoreAdjacent is true.
    if (this.points.isNotEmpty()) {
        for (p in this.points) {
            if (other.containsPoint(p)) {
                // isPointInConcavePolygon implementation may or may not return true for points on the boundary.
                // However, if we reached here, it means no edges intersect (or they were ignored).
                // If they were ignored, it means they are adjacent.
                // If it's a true containment, there should be no adjacency (unless one is entirely on the boundary of another, which is collinear edges).

                // Re-check containment more carefully if ignoreAdjacent is true?
                // Actually, if there are no edge intersections (proper ones), then one polygon is either
                // completely inside, completely outside, or they only touch at boundaries.
                // If they touch at boundaries and ignoreAdjacent is true, we should return false.
                if (ignoreAdjacent) {
                    // To be sure, check if the point is NOT on any edge of the other polygon
                    var onBoundary = false
                    for (j in other.points.indices) {
                        val b0 = other.points[j]
                        val b1 = other.points[(j + 1) % other.points.size]
                        if (ccw(b0, b1, p) == 0 && p.x >= minOf(b0.x, b1.x) - 1E-8 && p.x <= maxOf(b0.x, b1.x) + 1E-8 &&
                            p.y >= minOf(b0.y, b1.y) - 1E-8 && p.y <= maxOf(b0.y, b1.y) + 1E-8
                        ) {
                            onBoundary = true
                            break
                        }
                    }
                    if (!onBoundary) return true
                } else {
                    return true
                }
            }
        }
    }

    if (other.points.isNotEmpty()) {
        for (p in other.points) {
            if (this.containsPoint(p)) {
                if (ignoreAdjacent) {
                    var onBoundary = false
                    for (i in points.indices) {
                        val a0 = points[i]
                        val a1 = points[(i + 1) % points.size]
                        if (ccw(a0, a1, p) == 0 && p.x >= minOf(a0.x, a1.x) - 1E-8 && p.x <= maxOf(a0.x, a1.x) + 1E-8 &&
                            p.y >= minOf(a0.y, a1.y) - 1E-8 && p.y <= maxOf(a0.y, a1.y) + 1E-8
                        ) {
                            onBoundary = true
                            break
                        }
                    }
                    if (!onBoundary) return true
                } else {
                    return true
                }
            }
        }
    }

    return false
}

private fun ccw(a: Vector2, b: Vector2, c: Vector2): Int {
    val area = (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)
    return when {
        area > 1E-8 -> 1
        area < -1E-8 -> -1
        else -> 0
    }
}

internal fun segmentsIntersect(p1: Vector2, p2: Vector2, p3: Vector2, p4: Vector2): Boolean {
    fun onSegment(a: Vector2, b: Vector2, p: Vector2): Boolean {
        return p.x >= minOf(a.x, b.x) - 1E-8 && p.x <= maxOf(a.x, b.x) + 1E-8 &&
                p.y >= minOf(a.y, b.y) - 1E-8 && p.y <= maxOf(a.y, b.y) + 1E-8
    }

    val ccw1 = ccw(p1, p2, p3)
    val ccw2 = ccw(p1, p2, p4)
    val ccw3 = ccw(p3, p4, p1)
    val ccw4 = ccw(p3, p4, p2)

    if (((ccw1 > 0 && ccw2 < 0) || (ccw1 < 0 && ccw2 > 0)) &&
        ((ccw3 > 0 && ccw4 < 0) || (ccw3 < 0 && ccw4 > 0))
    ) return true

    if (ccw1 == 0 && onSegment(p1, p2, p3)) return true
    if (ccw2 == 0 && onSegment(p1, p2, p4)) return true
    if (ccw3 == 0 && onSegment(p3, p4, p1)) return true
    if (ccw4 == 0 && onSegment(p3, p4, p2)) return true

    return false
}