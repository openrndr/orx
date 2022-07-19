package org.openrndr.extra.triangulation

import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.bounds
import kotlin.js.JsName
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sign

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
 * This is a fast library for computing the Voronoi diagram of a set of two-dimensional points.
 * The Voronoi diagram is constructed by connecting the circumcenters of adjacent triangles
 * in the Delaunay triangulation.
 *
 * @description Port of d3-delaunay (JavaScript) library - https://github.com/d3/d3-delaunay
 * @property points flat positions' array - [x0, y0, x1, y1..]
 *
 * @since 9258fa3 - commit
 * @author Ricardo Matias
 */
class Voronoi(val delaunay: Delaunay, val bounds: Rectangle) {
    private val _circumcenters = DoubleArray(delaunay.points.size * 2)
    lateinit var circumcenters: DoubleArray
        private set

    val vectors = DoubleArray(delaunay.points.size * 2)

    init {
        init()
    }

    fun update() {
        delaunay.update()
        init()
    }

    fun init() {
        val points = delaunay.points
        val triangles = delaunay.triangles
        val hull = delaunay.hull

        circumcenters = _circumcenters.copyOf(delaunay.triangles.size / 3 * 2)

        // Compute circumcenters
        var i = 0
        var j = 0

        var x: Double
        var y: Double

        while (i < triangles.size) {
            val t1 = triangles[i] * 2
            val t2 = triangles[i + 1] * 2
            val t3 = triangles[i + 2] * 2
            val x1 = points[t1]
            val y1 = points[t1 + 1]
            val x2 = points[t2]
            val y2 = points[t2 + 1]
            val x3 = points[t3]
            val y3 = points[t3 + 1]

            val dx = x2 - x1
            val dy = y2 - y1
            val ex = x3 - x1
            val ey = y3 - y1
            val ab = (dx * ey - dy * ex) * 2

            if (abs(ab) < 1e-9) {
                var a = 1e9
                val r = triangles[0] * 2
                a *= sign((points[r] - x1) * ey - (points[r + 1] - y1) * ex)
                x = (x1 + x3) / 2 - a * ey
                y = (y1 + y3) / 2 + a * ex
            } else {
                val d = 1 / ab
                val bl = dx * dx + dy * dy
                val cl = ex * ex + ey * ey
                x = x1 + (ey * bl - dy * cl) * d
                y = y1 + (dx * cl - ex * bl) * d
            }

            circumcenters[j] = x
            circumcenters[j + 1] = y

            i += 3
            j += 2
        }

        // Compute exterior cell rays.
        var h = hull[hull.size - 1]
        var p0: Int
        var p1 = h * 4
        var x0: Double
        var x1 = points[2 * h]
        var y0: Double
        var y1 = points[2 * h + 1]
        var y01: Double
        var x10: Double

        vectors.fill(0.0)

        for (idx in hull.indices) {
            h = hull[idx]
            p0 = p1
            x0 = x1
            y0 = y1
            p1 = h * 4
            x1 = points[2 * h]
            y1 = points[2 * h + 1]

            y01 = y0 - y1
            x10 = x1 - x0

            vectors[p0 + 2] = y01
            vectors[p1] = y01
            vectors[p0 + 3] = x10
            vectors[p1 + 1] = x10
        }

    }



    private fun cell(i: Int): MutableList<Double>? {
        val inedges = delaunay.inedges
        val halfedges = delaunay.halfedges
        val triangles = delaunay.triangles

        val e0 = inedges[i]

        if (e0 == -1) return null // coincident point

        val points = mutableListOf<Double>()

        var e = e0

        do {
            val t = floor(e / 3.0).toInt()

            points.add(circumcenters[t * 2])
            points.add(circumcenters[t * 2 + 1])

            e = if (e % 3 == 2) e - 2 else e + 1 // next half edge

            if (triangles[e] != i) break

            e = halfedges[e]
        } while (e != e0 && e != -1)

        return points
    }

    fun neighbors(i: Int) = sequence {
        val ci = clip(i)
        if (ci != null) {
            for (j in delaunay.neighbors(i)) {
                val cj = clip(j)
                if (cj != null) {
                    val li = ci.size
                    val lj = cj.size
                    loop@ for (ai in 0 until ci.size step 2) {
                        for (aj in 0 until cj.size step 2) {
                            if (ci[ai] == cj[aj]
                                && ci[ai + 1] == cj[aj + 1]
                                && ci[(ai + 2) % li] == cj[(aj + lj - 2) % lj]
                                && ci[(ai + 3) % li] == cj[(aj + lj - 1) % lj]
                            ) {
                                yield(j)
                                break@loop
                            }
                        }
                    }
                }
            }
        }
    }

    internal fun clip(i: Int): List<Double>? {
        // degenerate case (1 valid point: return the box)
        if (i == 0 && delaunay.hull.size == 1) {
            return listOf(
                bounds.xmax,
                bounds.ymin,
                bounds.xmax,
                bounds.ymax,
                bounds.xmin,
                bounds.ymax,
                bounds.xmin,
                bounds.ymin
            )
        }

        val points = cell(i) ?: return null

        val clipVectors = vectors
        val v = i * 4

        val a = !clipVectors[v].isFalsy()
        val b = !clipVectors[v + 1].isFalsy()

        return if (a || b) {
            this.clipInfinite(i, points, clipVectors[v], clipVectors[v + 1], clipVectors[v + 2], clipVectors[v + 3])
        } else {
            this.clipFinite(i, points)
        }
    }

    private fun clipInfinite(
        i: Int,
        points: MutableList<Double>,
        vx0: Double,
        vy0: Double,
        vxn: Double,
        vyn: Double
    ): List<Double>? {
        var P: MutableList<Double>? = points.mutableCopyOf()

        P!!
        project(P[0], P[1], vx0, vy0)?.let { p -> P!!.add(0, p[1]); P!!.add(0, p[0]) }
        project(P[P.size - 2], P[P.size - 1], vxn, vyn)?.let { p -> P!!.add(p[0]); P!!.add(p[1]) }

        P = this.clipFinite(i, P!!)
        var n = 0
        if (P != null) {
            n = P!!.size
            var c0 = -1
            var c1 = edgeCode(P[n - 2], P[n - 1])
            var j = 0
            var n = P.size
            while (j < n) {
                c0 = c1
                c1 = edgeCode(P[j], P[j + 1])
                if (c0 != 0 && c1 != 0) {
                    j = edge(i, c0, c1, P, j)
                    n = P.size
                }
                j += 2
            }
        } else if (this.contains(i, (bounds.xmin + bounds.xmax) / 2.0, (bounds.ymin + bounds.ymax) / 2.0)) {
            P = mutableListOf(
                bounds.xmin,
                bounds.ymin,
                bounds.xmax,
                bounds.ymin,
                bounds.xmax,
                bounds.ymax,
                bounds.xmin,
                bounds.ymax
            )
        }
        return P
    }

    private fun clipFinite(i: Int, points: MutableList<Double>): MutableList<Double>? {
        val n = points.size

        val P = mutableListOf<Double>()
        var x0: Double
        var y0: Double
        var x1 = points[n - 2]
        var y1 = points[n - 1]
        var c0: Int
        var c1: Int = regionCode(x1, y1)
        var e0: Int? = null
        var e1: Int? = 0

        for (j in 0 until n step 2) {
            x0 = x1
            y0 = y1
            x1 = points[j]
            y1 = points[j + 1]
            c0 = c1
            c1 = regionCode(x1, y1)

            if (c0 == 0 && c1 == 0) {
                e0 = e1
                e1 = 0

                P.add(x1)
                P.add(y1)
            } else {
                var S: DoubleArray?
                var sx0: Double
                var sy0: Double
                var sx1: Double
                var sy1: Double

                if (c0 == 0) {
                    S = clipSegment(x0, y0, x1, y1, c0, c1)
                    if (S == null) continue
                    sx0 = S[0]
                    sy0 = S[1]
                    sx1 = S[2]
                    sy1 = S[3]
                } else {
                    S = clipSegment(x1, y1, x0, y0, c1, c0)
                    if (S == null) continue
                    sx1 = S[0]
                    sy1 = S[1]
                    sx0 = S[2]
                    sy0 = S[3]

                    e0 = e1
                    e1 = this.edgeCode(sx0, sy0)

                    if (e0 != 0 && e1 != 0) this.edge(i, e0!!, e1, P, P.size)

                    P.add(sx0)
                    P.add(sy0)
                }

                e0 = e1
                e1 = this.edgeCode(sx1, sy1);

                if (e0.isTruthy() && e1.isTruthy()) this.edge(i, e0!!, e1, P, P.size);

                P.add(sx1)
                P.add(sy1)
            }
        }

        if (P.isNotEmpty()) {
            e0 = e1
            e1 = this.edgeCode(P[0], P[1])

            if (e0.isTruthy() && e1.isTruthy()) this.edge(i, e0!!, e1!!, P, P.size);
        } else if (this.contains(i, (bounds.xmin + bounds.xmax) / 2, (bounds.ymin + bounds.ymax) / 2)) {
            return mutableListOf(
                bounds.xmax,
                bounds.ymin,
                bounds.xmax,
                bounds.ymax,
                bounds.xmin,
                bounds.ymax,
                bounds.xmin,
                bounds.ymin
            )
        } else {
            return null
        }
        return P
    }

    private fun clipSegment(x0: Double, y0: Double, x1: Double, y1: Double, c0: Int, c1: Int): DoubleArray? {
        var nx0: Double = x0
        var ny0: Double = y0
        var nx1: Double = x1
        var ny1: Double = y1
        var nc0: Int = c0
        var nc1: Int = c1

        while (true) {
            if (nc0 == 0 && nc1 == 0) return doubleArrayOf(nx0, ny0, nx1, ny1)
            // SHAKY STUFF
            if ((nc0 and nc1) != 0) return null

            var x: Double
            var y: Double
            val c: Int = if (nc0 != 0) nc0 else nc1

            when {
                (c and 0b1000) != 0 -> {
                    x = nx0 + (nx1 - nx0) * (bounds.ymax - ny0) / (ny1 - ny0)
                    y = bounds.ymax;
                }
                (c and 0b0100) != 0 -> {
                    x = nx0 + (nx1 - nx0) * (bounds.ymin - ny0) / (ny1 - ny0)
                    y = bounds.ymin
                }
                (c and 0b0010) != 0 -> {
                    y = ny0 + (ny1 - ny0) * (bounds.xmax - nx0) / (nx1 - nx0)
                    x = bounds.xmax
                }
                else -> {
                    y = ny0 + (ny1 - ny0) * (bounds.xmin - nx0) / (nx1 - nx0)
                    x = bounds.xmin;
                }
            }

            if (nc0 != 0) {
                nx0 = x
                ny0 = y
                nc0 = this.regionCode(nx0, ny0)
            } else {
                nx1 = x
                ny1 = y
                nc1 = this.regionCode(nx1, ny1)
            }
        }
    }

    private fun regionCode(x: Double, y: Double): Int {
        val xcode = when {
            x < bounds.xmin -> 0b0001
            x > bounds.xmax -> 0b0010
            else -> 0b0000
        }
        val ycode = when {
            y < bounds.ymin -> 0b0100
            y > bounds.ymax -> 0b1000
            else -> 0b0000
        }
        return xcode or ycode
    }


    private fun contains(i: Int, x: Double, y: Double): Boolean {
        if (x.isNaN() || y.isNaN()) return false
        return this.delaunay.step(i, x, y) == i;
    }

    private fun edge(i: Int, e0: Int, e1: Int, p: MutableList<Double>, j: Int): Int {
        var j = j
        var e = e0
        loop@ while (e != e1) {
            var x: Double = Double.NaN
            var y: Double = Double.NaN

            when (e) {
                0b0101 -> { // top-left
                    e = 0b0100
                    continue@loop
                }
                0b0100 -> { // top
                    e = 0b0110
                    x = bounds.xmax
                    y = bounds.ymin
                }
                0b0110 -> { // top-right
                    e = 0b0010
                    continue@loop
                }
                0b0010 -> { // right
                    e = 0b1010
                    x = bounds.xmax
                    y = bounds.ymax
                }
                0b1010 -> { // bottom-right
                    e = 0b1000
                    continue@loop
                }
                0b1000 -> { // bottom
                    e = 0b1001
                    x = bounds.xmin
                    y = bounds.ymax
                }
                0b1001 -> { // bottom-left
                    e = 0b0001
                    continue@loop
                }
                0b0001 -> { // left
                    e = 0b0101
                    x = bounds.xmin
                    y = bounds.ymin
                }
            }

            if (((j < p.size && (p[j] != x)) || ((j + 1) < p.size && p[j + 1] != y)) && contains(i, x, y)) {
                require(!x.isNaN())
                require(!y.isNaN())
                p.add(j, y)
                p.add(j, x)
                j += 2
            } else if (j >= p.size && contains(i, x, y)) {
                require(!x.isNaN())
                require(!y.isNaN())
                p.add(x)
                p.add(y)
                j += 2
            }
        }

        if (p.size > 4) {
            var idx = 0
            var n = p.size
            while (idx < n) {
                val j = (idx + 2) % p.size
                val k = (idx + 4) % p.size

                if ((p[idx] == p[j] && p[j] == p[k])
                    || (p[idx + 1] == p[j + 1] && p[j + 1] == p[k + 1])
                ) {
                    // SHAKY
                    p.removeAt(j)
                    p.removeAt(j)
                    idx -= 2
                    n -= 2
                }
                idx += 2
            }
        }
        return j
    }

    private fun project(x0: Double, y0: Double, vx: Double, vy: Double): Vector2? {
        var t = Double.POSITIVE_INFINITY
        var c: Double
        var x = Double.NaN
        var y = Double.NaN

        // top
        if (vy < 0) {
            if (y0 <= bounds.ymin) return null
            c = (bounds.ymin - y0) / vy

            if (c < t) {
                t = c

                y = bounds.ymin
                x = x0 + t * vx
            }
        } else if (vy > 0) {    // bottom
            if (y0 >= bounds.ymax) return null
            c = (bounds.ymax - y0) / vy

            if (c < t) {
                t = c

                y = bounds.ymax
                x = x0 + t * vx
            }
        }
        // right
        if (vx > 0) {
            if (x0 >= bounds.xmax) return null
            c = (bounds.xmax - x0) / vx

            if (c < t) {
                t = c

                x = bounds.xmax
                y = y0 + t * vy
            }
        } else if (vx < 0) { // left
            if (x0 <= bounds.xmin) return null
            c = (bounds.xmin - x0) / vx

            if (c < t) {
                t = c

                x = bounds.xmin
                y = y0 + t * vy
            }
        }

        if (x.isNaN() || y.isNaN()) return null

        return Vector2(x, y)
    }

    private fun edgeCode(x: Double, y: Double): Int {
        val xcode = when (x) {
            bounds.xmin -> 0b0001
            bounds.xmax -> 0b0010
            else -> 0b0000
        }
        val ycode = when (y) {
            bounds.ymin -> 0b0100
            bounds.ymax -> 0b1000
            else -> 0b0000
        }
        return xcode or ycode
    }

}

private fun Int?.isTruthy(): Boolean {
    return (this != null && this != 0)
}

private fun <T> List<T>.mutableCopyOf(): MutableList<T> {
    val original = this
    return mutableListOf<T>().apply { addAll(original) }
}

private val Rectangle.xmin: Double
    get() = this.corner.x

private val Rectangle.xmax: Double
    get() = this.corner.x + width

private val Rectangle.ymin: Double
    get() = this.corner.y

private val Rectangle.ymax: Double
    get() = this.corner.y + height

private fun Double?.isFalsy() = this == null || this == -0.0 || this == 0.0 || isNaN()


