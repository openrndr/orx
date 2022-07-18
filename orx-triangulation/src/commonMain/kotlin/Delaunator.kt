package org.openrndr.extra.triangulation

import kotlin.math.*

val EPSILON: Double = 2.0.pow(-52)

/**
 * A Kotlin port of Mapbox's Delaunator incredibly fast JavaScript library for Delaunay triangulation of 2D points.
 *
 * @description Port of Mapbox's Delaunator (JavaScript) library - https://github.com/mapbox/delaunator
 * @property coords flat positions' array - [x0, y0, x1, y1..]
 *
 * @since f0ed80d - commit
 * @author Ricardo Matias
 */
@Suppress("unused")
class Delaunator(val coords: DoubleArray) {
    val EDGE_STACK = IntArray(512)

    private var count = coords.size shr 1

    // arrays that will store the triangulation graph
    val maxTriangles = (2 * count - 5).coerceAtLeast(0)
    private val _triangles = IntArray(maxTriangles * 3)
    private val _halfedges = IntArray(maxTriangles * 3)

    lateinit var triangles: IntArray
    lateinit var halfedges: IntArray

    // temporary arrays for tracking the edges of the advancing convex hull
    private var hashSize = ceil(sqrt(count * 1.0)).toInt()
    private var hullPrev = IntArray(count) // edge to prev edge
    private var hullNext = IntArray(count) // edge to next edge
    private var hullTri = IntArray(count) // edge to adjacent triangle
    private var hullHash = IntArray(hashSize) // angular edge hash
    private var hullStart: Int = -1

    // temporary arrays for sorting points
    private var ids = IntArray(count)
    private var dists = DoubleArray(count)

    private var cx: Double = Double.NaN
    private var cy: Double = Double.NaN

    private var trianglesLen: Int = -1

    lateinit var hull: IntArray

    init {
        update()
    }

    fun update() {
        // populate an array of point indices calculate input data bbox
        var minX = Double.POSITIVE_INFINITY
        var minY = Double.POSITIVE_INFINITY
        var maxX = Double.NEGATIVE_INFINITY
        var maxY = Double.NEGATIVE_INFINITY

        // points -> points
        // minX, minY, maxX, maxY
        for (i in 0 until count) {
            val x = coords[2 * i]
            val y = coords[2 * i + 1]
            if (x < minX) minX = x
            if (y < minY) minY = y
            if (x > maxX) maxX = x
            if (y > maxY) maxY = y

            ids[i] = i
        }

        val cx = (minX + maxX) / 2
        val cy = (minY + maxY) / 2

        var minDist = Double.POSITIVE_INFINITY

        var i0: Int = -1
        var i1: Int = -1
        var i2: Int = -1

        // pick a seed point close to the center
        for (i in 0 until count) {
            val d = dist(cx, cy, coords[2 * i], coords[2 * i + 1])

            if (d < minDist) {
                i0 = i
                minDist = d
            }
        }

        val i0x = coords[2 * i0]
        val i0y = coords[2 * i0 + 1]

        minDist = Double.POSITIVE_INFINITY

        // Find the point closest to the seed
        for(i in 0 until count) {
            if (i == i0) continue

            val d = dist(i0x, i0y, coords[2 * i], coords[2 * i + 1])

            if (d < minDist && d > 0) {
                i1 = i
                minDist = d
            }
        }

        var i1x = coords[2 * i1]
        var i1y = coords[2 * i1 + 1]

        var minRadius = Double.POSITIVE_INFINITY

        // Find the third point which forms the smallest circumcircle with the first two
        for (i in 0 until count) {
            if(i == i0 || i == i1) continue

            val r = circumradius(i0x, i0y, i1x, i1y, coords[2 * i], coords[2 * i + 1])

            if(r < minRadius) {
                i2 = i
                minRadius = r
            }
        }

        if (minRadius == Double.POSITIVE_INFINITY) {
            // order collinear points by dx (or dy if all x are identical)
            // and return the list as a hull
            for (i in 0 until count) {
                val a = (coords[2 * i] - coords[0])
                val b = (coords[2 * i + 1] - coords[1])
                dists[i] =  if (a == 0.0) b else a
            }

            quicksort(ids, dists, 0, count - 1)

            val nhull = IntArray(count)
            var j = 0
            var d0 = Double.NEGATIVE_INFINITY

            for (i in 0 until count) {
                val id = ids[i]
                if (dists[id] > d0) {
                    nhull[j++] = id
                    d0 = dists[id]
                }
            }

            hull = nhull.copyOf(j)
            triangles = IntArray(0)
            halfedges = IntArray(0)

            return
        }

        var i2x = coords[2 * i2]
        var i2y = coords[2 * i2 + 1]

        // swap the order of the seed points for counter-clockwise orientation
        if (orient2d(i0x, i0y, i1x, i1y, i2x, i2y) < 0.0) {
            val i = i1
            val x = i1x
            val y = i1y
            i1 = i2
            i1x = i2x
            i1y = i2y
            i2 = i
            i2x = x
            i2y = y
        }


        val center = circumcenter(i0x, i0y, i1x, i1y, i2x, i2y)

        this.cx = center[0]
        this.cy = center[1]

        for (i in 0 until count) {
            dists[i] = dist(coords[2 * i], coords[2 * i + 1], center[0], center[1])
        }

        // sort the points by distance from the seed triangle circumcenter
        quicksort(ids, dists, 0, count - 1)

        // set up the seed triangle as the starting hull
        hullStart = i0
        var hullSize = 3

        hullNext[i0] = i1
        hullNext[i1] = i2
        hullNext[i2] = i0

        hullPrev[i2] = i1
        hullPrev[i0] = i2
        hullPrev[i1] = i0

        hullTri[i0] = 0
        hullTri[i1] = 1
        hullTri[i2] = 2

        hullHash.fill(-1)
        hullHash[hashKey(i0x, i0y)] = i0
        hullHash[hashKey(i1x, i1y)] = i1
        hullHash[hashKey(i2x, i2y)] = i2

        trianglesLen = 0
        addTriangle(i0, i1, i2, -1, -1, -1)

        var xp = 0.0
        var yp = 0.0

        for (k in ids.indices) {
            val i = ids[k]
            val x = coords[2 * i]
            val y = coords[2 * i + 1]

            // skip near-duplicate points
            if (k > 0 && abs(x - xp) <= EPSILON && abs(y - yp) <= EPSILON) continue

            xp = x
            yp = y

            // skip seed triangle points
            if (i == i0 || i == i1 || i == i2) continue

            // find a visible edge on the convex hull using edge hash
            var start = 0
            val key = hashKey(x, y)

            for (j in 0 until hashSize) {
                start = hullHash[(key + j) % hashSize]

                if (start != -1 && start != hullNext[start]) break
            }

            start = hullPrev[start]

            var e = start
            var q = hullNext[e]

            while (orient2d(x, y, coords[2 * e], coords[2 * e + 1], coords[2 * q], coords[2 * q + 1]) >= 0) {
                e = q

                if (e == start) {
                    e = -1
                    break
                }

                q = hullNext[e]
            }

            if (e == -1) continue // likely a near-duplicate point skip it

            // add the first triangle from the point
            var t = addTriangle(e, i, hullNext[e], -1, -1, hullTri[e])

            // recursively flip triangles from the point until they satisfy the Delaunay condition
            hullTri[i] = legalize(t + 2)
            hullTri[e] = t // keep track of boundary triangles on the hull
            hullSize++

            // walk forward through the hull, adding more triangles and flipping recursively
            var next = hullNext[e]
            q = hullNext[next]

            while (orient2d(x, y, coords[2 * next], coords[2 * next + 1], coords[2 * q], coords[2 * q + 1]) < 0) {
                t = addTriangle(next, i, q, hullTri[i], -1, hullTri[next])
                hullTri[i] = legalize(t + 2)
                hullNext[next] = next // mark as removed
                hullSize--

                next = q
                q = hullNext[next]
            }

            // walk backward from the other side, adding more triangles and flipping
            if (e == start) {
                q = hullPrev[e]

                while (orient2d(x, y, coords[2 * q], coords[2 * q + 1], coords[2 * e], coords[2 * e + 1]) < 0) {
                    t = addTriangle(q, i, e, -1, hullTri[e], hullTri[q])
                    legalize(t + 2)
                    hullTri[q] = t
                    hullNext[e] = e // mark as removed
                    hullSize--

                    e = q
                    q = hullPrev[e]
                }
            }

            // update the hull indices
            hullStart = e
            hullPrev[i] = e

            hullNext[e] = i
            hullPrev[next] = i
            hullNext[i] = next

            // save the two new edges in the hash table
            hullHash[hashKey(x, y)] = i
            hullHash[hashKey(coords[2 * e], coords[2 * e + 1])] = e
        }

        hull = IntArray(hullSize)

        var e = hullStart

        for (i in 0 until hullSize) {
            hull[i] = e
            e = hullNext[e]
        }

        // trim typed triangle mesh arrays
        triangles = _triangles.copyOf(trianglesLen)
        halfedges = _halfedges.copyOf(trianglesLen)
    }

    private fun legalize(a: Int): Int {
        var i = 0
        var na = a
        var ar: Int

        // recursion eliminated with a fixed-size stack
        while (true) {
            val b = _halfedges[na]

            /* if the pair of triangles doesn't satisfy the Delaunay condition
             * (p1 is inside the circumcircle of [p0, pl, pr]), flip them,
             * then do the same check/flip recursively for the new pair of triangles
             *
             *           pl                    pl
             *          /||\                  /  \
             *       al/ || \bl            al/    \a
             *        /  ||  \              /      \
             *       /  a||b  \    flip    /___ar___\
             *     p0\   ||   /p1   =>   p0\---bl---/p1
             *        \  ||  /              \      /
             *       ar\ || /br             b\    /br
             *          \||/                  \  /
             *           pr                    pr
             */
            val a0 = na - na % 3
            ar = a0 + (na + 2) % 3

            if (b == -1) { // convex hull edge
                if (i == 0) break
                na = EDGE_STACK[--i]
                continue
            }

            val b0 = b - b % 3
            val al = a0 + (na + 1) % 3
            val bl = b0 + (b + 2) % 3

            val p0 = _triangles[ar]
            val pr = _triangles[na]
            val pl = _triangles[al]
            val p1 = _triangles[bl]

            val illegal = inCircleRobust(
                coords[2 * p0], coords[2 * p0 + 1],
                coords[2 * pr], coords[2 * pr + 1],
                coords[2 * pl], coords[2 * pl + 1],
                coords[2 * p1], coords[2 * p1 + 1])

            if (illegal) {
                _triangles[na] = p1
                _triangles[b] = p0

                val hbl = _halfedges[bl]

                // edge swapped on the other side of the hull (rare) fix the halfedge reference
                if (hbl == -1) {
                    var e = hullStart
                    do {
                        if (hullTri[e] == bl) {
                            hullTri[e] = na
                            break
                        }
                        e = hullPrev[e]
                    } while (e != hullStart)
                }
                link(na, hbl)
                link(b, _halfedges[ar])
                link(ar, bl)

                val br = b0 + (b + 1) % 3

                // don't worry about hitting the cap: it can only happen on extremely degenerate input
                if (i < EDGE_STACK.size) {
                    EDGE_STACK[i++] = br
                }
            } else {
                if (i == 0) break
                na = EDGE_STACK[--i]
            }
        }

        return ar

    }

    private fun link(a:Int, b:Int) {
        _halfedges[a] = b
        if (b != -1) _halfedges[b] = a
    }

    // add a new triangle given vertex indices and adjacent half-edge ids
    private fun addTriangle(i0: Int, i1: Int, i2: Int, a: Int, b: Int, c: Int): Int {
        val t = trianglesLen

        _triangles[t] = i0
        _triangles[t + 1] = i1
        _triangles[t + 2] = i2

        link(t, a)
        link(t + 1, b)
        link(t + 2, c)

        trianglesLen += 3

        return t
    }

    private fun hashKey(x: Double, y: Double): Int {
        return (floor(pseudoAngle(x - cx, y - cy) * hashSize) % hashSize).toInt()
    }
}

fun circumradius(ax: Double, ay: Double,
                 bx: Double, by: Double,
                 cx: Double, cy: Double): Double {
    val dx = bx - ax
    val dy = by - ay
    val ex = cx - ax
    val ey = cy - ay

    val bl = dx * dx + dy * dy
    val cl = ex * ex + ey * ey
    val d = 0.5 / (dx * ey - dy * ex)

    val x = (ey * bl - dy * cl) * d
    val y = (dx * cl - ex * bl) * d

    return x * x + y * y
}

fun circumcenter(ax: Double, ay: Double,
                 bx: Double, by: Double,
                 cx: Double, cy: Double): DoubleArray {
    val dx = bx - ax
    val dy = by - ay
    val ex = cx - ax
    val ey = cy - ay

    val bl = dx * dx + dy * dy
    val cl = ex * ex + ey * ey
    val d = 0.5 / (dx * ey - dy * ex)

    val x = ax + (ey * bl - dy * cl) * d
    val y = ay + (dx * cl - ex * bl) * d

    return doubleArrayOf(x, y)
}

fun quicksort(ids: IntArray, dists: DoubleArray, left: Int, right: Int) {
    if (right - left <= 20) {
        for (i in (left + 1)..right) {
            val temp = ids[i]
            val tempDist = dists[temp]
            var j = i - 1
            while (j >= left && dists[ids[j]] > tempDist) ids[j + 1] = ids[j--]
            ids[j + 1] = temp
        }
    } else {
        val median = (left + right) shr 1
        var i = left + 1
        var j = right

        swap(ids, median, i)

        if (dists[ids[left]] > dists[ids[right]]) swap(ids, left, right)
        if (dists[ids[i]] > dists[ids[right]]) swap(ids, i, right)
        if (dists[ids[left]] > dists[ids[i]]) swap(ids, left, i)

        val temp = ids[i]
        val tempDist = dists[temp]

        while (true) {
            do i++ while (dists[ids[i]] < tempDist)
            do j-- while (dists[ids[j]] > tempDist)
            if (j < i) break
            swap(ids, i, j)
        }

        ids[left + 1] = ids[j]
        ids[j] = temp

        if (right - i + 1 >= j - left) {
            quicksort(ids, dists, i, right)
            quicksort(ids, dists, left, j - 1)
        } else {
            quicksort(ids, dists, left, j - 1)
            quicksort(ids, dists, i, right)
        }
    }
}

private fun swap(arr: IntArray, i: Int, j: Int) {
    val tmp = arr[i]
    arr[i] = arr[j]
    arr[j] = tmp
}

// monotonically increases with real angle, but doesn't need expensive trigonometry
private fun pseudoAngle(dx: Double, dy: Double): Double {
    val p = dx / (abs(dx) + abs(dy))
    val a =  if (dy > 0.0) 3.0 - p else 1.0 + p

    return a / 4.0 // [0..1]
}

private fun inCircle(ax: Double, ay: Double,
                     bx: Double, by: Double,
                     cx: Double, cy: Double,
                     px: Double, py: Double): Boolean {
    val dx = ax - px
    val dy = ay - py
    val ex = bx - px
    val ey = by - py
    val fx = cx - px
    val fy = cy - py

    val ap = dx * dx + dy * dy
    val bp = ex * ex + ey * ey
    val cp = fx * fx + fy * fy

    return dx * (ey * cp - bp * fy) -
            dy * (ex * cp - bp * fx) +
            ap * (ex * fy - ey * fx) < 0
}

private fun inCircleRobust(
    ax: Double, ay: Double,
    bx: Double, by: Double,
    cx: Double, cy: Double,
    px: Double, py: Double
): Boolean {

    val dx = twoDiff(ax, px)
    val dy = twoDiff(ay, py)
    val ex = twoDiff(bx, px)
    val ey = twoDiff(by, py)
    val fx = twoDiff(cx, px)
    val fy = twoDiff(cy, py)

    val ap = ddAddDd(ddMultDd(dx, dx), ddMultDd(dy, dy))
    val bp = ddAddDd(ddMultDd(ex, ex), ddMultDd(ey, ey))
    val cp = ddAddDd(ddMultDd(fx, fx), ddMultDd(fy, fy))

    val dd = ddAddDd(
        ddDiffDd(
            ddMultDd(dx, ddDiffDd(ddMultDd(ey, cp), ddMultDd(bp, fy))),
            ddMultDd(dy, ddDiffDd(ddMultDd(ex, cp), ddMultDd(bp, fx)))
        ),
        ddMultDd(ap, ddDiffDd(ddMultDd(ex, fy), ddMultDd(ey, fx)))
    )
    // add a small bias here, it seems to help
    return (dd[1]) <= 1E-8
}


private fun dist(ax: Double, ay: Double, bx: Double, by: Double): Double {
    //val dx = ax - bx
    //val dy = ay - by
    //return dx * dx + dy * dy

    // double-double implementation but I think it is overkill.

    val dx = twoDiff(ax, bx)
    val dy = twoDiff(ay, by)
    val dx2 = ddMultDd(dx, dx)
    val dy2 = ddMultDd(dy, dy)
    val d2 = ddAddDd(dx2, dy2)

    return d2[0] + d2[1]

}