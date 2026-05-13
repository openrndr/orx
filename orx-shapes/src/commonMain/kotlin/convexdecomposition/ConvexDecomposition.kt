package org.openrndr.extra.shapes.convexdecomposition

import org.openrndr.math.Vector2
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.triangulate

/**
 * Decomposes a [Shape] into a list of convex [ShapeContour]s using the Hertel-Mehlhorn algorithm.
 * The algorithm starts with a triangulation of the shape and then merges adjacent triangles
 * if their union forms a convex polygon.
 *
 * @param distanceTolerance The tolerance used for adaptive sampling of the shape's contours.
 * @return A list of closed, convex [ShapeContour]s that together represent the original shape.
 */
fun Shape.convexDecompose(distanceTolerance: Double = 0.5): List<ShapeContour> {
    val contours = mutableListOf<List<Vector2>>()
    for (contour in closedContours) {
        if (contour.segments.isNotEmpty()) {
            val positions = contour.adaptivePositions(distanceTolerance)
            if (positions.size >= 3) {
                // remove the last point if it's identical to the first one (closed contour)
                if (positions.first().distanceTo(positions.last()) < 1e-6) {
                    contours.add(positions.dropLast(1))
                } else {
                    contours.add(positions)
                }
            }
        }
    }

    if (contours.isEmpty()) return emptyList()

    val points = contours.flatMap { it }
    val indices = triangulate(contours)

    if (indices.isEmpty()) return emptyList()

    // Represent each triangle as a list of point indices
    val polygons = mutableListOf<MutableList<Int>>()
    for (i in indices.indices step 3) {
        polygons.add(mutableListOf(indices[i], indices[i + 1], indices[i + 2]))
    }

    fun getEdge(p1: Int, p2: Int): Pair<Int, Int> {
        return if (p1 < p2) p1 to p2 else p2 to p1
    }

    // Helper to check if three points form a convex angle (cross product >= 0)
    // Assumes points are in counter-clockwise order.
    fun isConvex(a: Vector2, b: Vector2, c: Vector2): Boolean {
        val cross = (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)
        return cross >= -1e-8 // Small epsilon for numerical stability
    }

    val edgeToPolygons = mutableMapOf<Pair<Int, Int>, MutableSet<MutableList<Int>>>()
    for (poly in polygons) {
        for (i in poly.indices) {
            val edge = getEdge(poly[i], poly[(i + 1) % poly.size])
            edgeToPolygons.getOrPut(edge) { mutableSetOf() }.add(poly)
        }
    }

    val interiorEdges = edgeToPolygons.filter { it.value.size == 2 }.keys.toMutableSet()

    while (interiorEdges.isNotEmpty()) {
        val edge = interiorEdges.first()
        interiorEdges.remove(edge)

        val sharingPolys = edgeToPolygons[edge] ?: continue
        if (sharingPolys.size != 2) continue

        val polyList = sharingPolys.toList()
        val poly1 = polyList[0]
        val poly2 = polyList[1]

        // Find indices of the edge in both polygons
        var e1_1 = -1
        for (i in poly1.indices) {
            val p1 = poly1[i]
            val p2 = poly1[(i + 1) % poly1.size]
            if ((p1 == edge.first && p2 == edge.second) || (p1 == edge.second && p2 == edge.first)) {
                e1_1 = i
                break
            }
        }
        
        if (e1_1 == -1) continue

        val e1_2 = (e1_1 + 1) % poly1.size
        val a = poly1[e1_1]
        val b = poly1[e1_2]

        val ia2 = poly2.indexOf(a)
        val ib2 = poly2.indexOf(b)
        
        if (ia2 == -1 || ib2 == -1) continue

        val p2_after_a = poly2[(ia2 + 1) % poly2.size]
        val p2_before_a = poly2[(ia2 + poly2.size - 1) % poly2.size]
        val p2_after_b = poly2[(ib2 + 1) % poly2.size]
        val p2_before_b = poly2[(ib2 + poly2.size - 1) % poly2.size]

        val p2_adj_a = if (p2_after_a == b) p2_before_a else p2_after_a
        val p2_adj_b = if (p2_after_b == a) p2_before_b else p2_after_b

        val p1_adj_a = if (poly1[(poly1.indexOf(a) + 1) % poly1.size] == b) {
            poly1[(poly1.indexOf(a) + poly1.size - 1) % poly1.size]
        } else {
            poly1[(poly1.indexOf(a) + 1) % poly1.size]
        }
        val p1_adj_b = if (poly1[(poly1.indexOf(b) + 1) % poly1.size] == a) {
            poly1[(poly1.indexOf(b) + poly1.size - 1) % poly1.size]
        } else {
            poly1[(poly1.indexOf(b) + 1) % poly1.size]
        }

        if (isConvex(points[p1_adj_a], points[a], points[p2_adj_a]) &&
            isConvex(points[p2_adj_b], points[b], points[p1_adj_b])
        ) {
            val newPoly = mutableListOf<Int>()
            if (poly1[(poly1.indexOf(a) + 1) % poly1.size] == b) {
                val poly1_b_idx = poly1.indexOf(b)
                for (k in 0 until poly1.size - 1) {
                    newPoly.add(poly1[(poly1_b_idx + k) % poly1.size])
                }
                val poly2_a_idx = poly2.indexOf(a)
                for (k in 0 until poly2.size - 1) {
                    newPoly.add(poly2[(poly2_a_idx + k) % poly2.size])
                }
            } else {
                val poly1_a_idx2 = poly1.indexOf(a)
                for (k in 0 until poly1.size - 1) {
                    newPoly.add(poly1[(poly1_a_idx2 + k) % poly1.size])
                }
                val poly2_b_idx = poly2.indexOf(b)
                for (k in 0 until poly2.size - 1) {
                    newPoly.add(poly2[(poly2_b_idx + k) % poly2.size])
                }
            }

            // Remove old polygons from edgeToPolygons and interiorEdges
            for (poly in listOf(poly1, poly2)) {
                for (i in poly.indices) {
                    val e = getEdge(poly[i], poly[(i + 1) % poly.size])
                    val set = edgeToPolygons[e]
                    if (set != null) {
                        set.remove(poly)
                        if (set.isEmpty()) {
                            edgeToPolygons.remove(e)
                            interiorEdges.remove(e)
                        } else if (set.size == 1) {
                            interiorEdges.remove(e)
                        }
                    }
                }
            }

            polygons.remove(poly1)
            polygons.remove(poly2)
            polygons.add(newPoly)

            // Add new polygon to edgeToPolygons and update interiorEdges
            for (i in newPoly.indices) {
                val e = getEdge(newPoly[i], newPoly[(i + 1) % newPoly.size])
                val set = edgeToPolygons.getOrPut(e) { mutableSetOf() }
                set.add(newPoly)
                if (set.size == 2) {
                    interiorEdges.add(e)
                } else if (set.size > 2) {
                    interiorEdges.remove(e)
                }
            }
        }
    }

    return polygons.map { poly ->
        ShapeContour.fromPoints(poly.map { points[it] }, true)
    }
}