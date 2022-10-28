package org.openrndr.extra.triangulation

import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

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

    private var delaunator: Delaunator = Delaunator(points)

    val inedges = IntArray(points.size / 2)
    private val hullIndex = IntArray(points.size / 2)

    var halfedges: IntArray = delaunator.halfedges
    var hull: IntArray = delaunator.hull
    var triangles: IntArray = delaunator.triangles

    init {
        init()
    }

    fun update() {
        delaunator.update()
        init()
    }

    fun neighbors(i: Int) = sequence {
        val e0 = inedges.getOrNull(i) ?: return@sequence
        if (e0 != -1) {
            var e = e0
            var p0 : Int

            loop@ do {
                p0 = triangles[e]
                yield(p0)
                e = if (e % 3 == 2) e - 2 else e + 1
                if (e == -1) {
                    break@loop
                }

                if (triangles[e] != i) {
                    break@loop
                    //error("bad triangulation")
                }
                e = halfedges[e]

                if (e == -1) {
                    val p = hull[(hullIndex[i] + 1) % hull.size]
                    if (p != p0) {
                        yield(p)
                    }
                    break@loop
                }
            } while (e != e0)
        }
    }

    fun collinear(): Boolean {
        for (i in triangles.indices step 3) {
            val a = 2 * triangles[i]
            val b = 2 * triangles[i + 1]
            val c = 2 * triangles[i + 2]
            val coords = points
            val cross = (coords[c] - coords[a]) * (coords[b + 1] - coords[a + 1])
            -(coords[b] - coords[a]) * (coords[c + 1] - coords[a + 1])
            if (cross > 1e-10) return false
        }
        return true
    }

    private fun jitter(x: Double, y: Double, r: Double): DoubleArray {
        return doubleArrayOf(x + sin(x + y) * r, y + cos(x - y) * r)
    }

    fun init() {

        if (hull.size > 2 && collinear()) {
            println("warning: triangulation is collinear")
            val r = 1E-8
            for (i in points.indices step 2) {
                val p = jitter(points[i], points[i + 1], r)
                points[i] = p[0]
                points[i + 1] = p[1]
            }

            delaunator = Delaunator(points)
            halfedges = delaunator.halfedges
            hull = delaunator.hull
            triangles = delaunator.triangles

        }

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
            inedges[hull[0]] = 1
            if (hull.size == 2) {
                inedges[hull[1]] = 0
                triangles[1] = hull[1]
                triangles[2] = hull[1]
            }
        }
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

            e = if (e % 3 == 2) e - 2 else e + 1

            if (triangles[e] != i) {
                //error("bad triangulation")
                break
            } // bad triangulation

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

