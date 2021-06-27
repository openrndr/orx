package org.openrndr.extra.triangulation

import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Triangle
import org.openrndr.shape.contour
import org.openrndr.shape.contours
import com.github.ricardomatias.Delaunator
import kotlin.math.pow

/*
ISC License

Copyright 2021 Ricardo Matias.

Permission to use, copy, modify, and/or distribute this software for any purpose
with or without fee is hereby granted, provided that the above copyright notice
and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH
REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND
FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT,
INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS
OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER
TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
THIS SOFTWARE.
*/

/**
 * Use [from] static method to use the delaunay triangulation
 *
 * @description Port of d3-delaunay (JavaScript) library - https://github.com/d3/d3-delaunay
 * @property points flat positions' array - [x0, y0, x1, y1..]
 *
 * @since 9258fa3 - commit
 * @author Ricardo Matias
 */
@Suppress("unused")
class Delaunay(val points: DoubleArray) {
    companion object {
        /**
         * Entry point for the delaunay triangulation
         *
         * @property points a list of 2D points
         */
        fun from(points: List<Vector2>): Delaunay {
            val n = points.size
            val coords = DoubleArray(n * 2)

            for (i in points.indices) {
                val p = points[i]
                coords[2 * i] = p.x
                coords[2 * i + 1] = p.y
            }

            return Delaunay(coords)
        }
    }

    private var delaunator = Delaunator(points)

    val inedges = IntArray(points.size / 2)
    private val hullIndex = IntArray(points.size / 2)

    var halfedges = delaunator.halfedges
    var hull = delaunator.hull
    var triangles = delaunator.triangles

    init {
        init()
    }

    fun update() {
        delaunator.update()
        init()
    }

    fun init() {
        halfedges = delaunator.halfedges
        hull = delaunator.hull
        triangles = delaunator.triangles

        inedges.fill(-1)
        hullIndex.fill(-1)

        // Compute an index from each point to an (arbitrary) incoming halfedge
        // Used to give the first neighbor of each point for this reason,
        // on the hull we give priority to exterior halfedges
        for (e in halfedges.indices) {
            val p = triangles[nextHalfedge(e)]

            if (halfedges[e] == -1 || inedges[p] == -1) inedges[p] = e
        }

        for (i in hull.indices) {
            hullIndex[hull[i]] = i
        }

        // degenerate case: 1 or 2 (distinct) points
        if (hull.size in 1..2) {
            triangles = IntArray(3) { -1 }
            halfedges = IntArray(3) { -1 }
            triangles[0] = hull[0]
            triangles[1] = hull[1]
            triangles[2] = hull[1]
            inedges[hull[0]] = 1
            if (hull.size == 2) inedges[hull[1]] = 0
        }
    }

    fun triangles(): List<Triangle> {
        val list = mutableListOf<Triangle>()

        for (i in triangles.indices step 3 ) {
            val t0 = triangles[i] * 2
            val t1 = triangles[i + 1] * 2
            val t2 = triangles[i + 2] * 2

            val p1 = Vector2(points[t0], points[t0 + 1])
            val p2 = Vector2(points[t1], points[t1 + 1])
            val p3 = Vector2(points[t2], points[t2 + 1])

            // originally they are defined *counterclockwise*
            list.add(Triangle(p3,  p2, p1))
        }

        return list
    }

    // Inner edges of the delaunay triangulation (without hull)
    fun halfedges() = contours {
        for (i in halfedges.indices) {
            val j = halfedges[i]

            if (j < i) continue
            val ti = triangles[i] * 2
            val tj = triangles[j] * 2

            moveTo(points[ti], points[ti + 1])
            lineTo(points[tj], points[tj + 1])
        }
    }

    fun hull() = contour {
        for (h in hull) {
            moveOrLineTo(points[2 * h], points[2 * h + 1])
        }
        close()
    }

    fun find(x: Double, y: Double, i: Int = 0): Int {
        var i1 = i
        var c = step(i, x, y)

        while (c >= 0 && c != i && c != i1) {
            i1 = c
            c = step(i1, x, y)
        }
        return c
    }

    fun nextHalfedge(e: Int) = if (e % 3 == 2) e - 2 else e + 1
    fun prevHalfedge(e: Int) = if (e % 3 == 0) e + 2 else e - 1

    fun step(i: Int, x: Double, y: Double): Int {
        if (inedges[i] == -1 || points.isEmpty()) return (i + 1) % (points.size shr 1)

        var c = i
        var dc = (x - points[i * 2]).pow(2) + (y - points[i * 2 + 1]).pow(2)
        val e0 = inedges[i]
        var e = e0
        do {
            val t = triangles[e]
            val dt = (x - points[t * 2]).pow(2) + (y - points[t * 2 + 1]).pow(2)

            if (dt < dc) {
                dc = dt
                c = t
            }

            e = nextHalfedge(e)

            if (triangles[e] != i) break // bad triangulation

            e = halfedges[e]

            if (e == -1) {
                e = hull[(hullIndex[i] + 1) % hull.size]
                if (e != t) {
                    if ((x - points[e * 2]).pow(2) + (y - points[e * 2 + 1]).pow(2) < dc) return e
                }
                break
            }
        } while (e != e0)

        return c
    }

    fun voronoi(bounds: Rectangle): Voronoi = Voronoi(this, bounds)
}