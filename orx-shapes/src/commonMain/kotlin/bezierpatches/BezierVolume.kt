package org.openrndr.extra.shapes.bezierpatches

import org.openrndr.math.Matrix33
import org.openrndr.math.Vector3
import org.openrndr.shape.Box

/**
 * A tricubic Bezier volume: a 4x4x4 lattice of control [points], indexed as `points[w][v][u]`, that maps the unit cube
 * of u, v and w to a deformed block of space. Moving its control points bends, twists or tapers whatever is placed in
 * it, see [org.openrndr.extra.shapes.distort.envelope].
 */
class BezierVolume(val points: List<List<List<Vector3>>>) {
    init {
        require(points.size == 4 && points.all { it.size == 4 && it.all { row -> row.size == 4 } }) {
            "a Bezier volume has 4x4x4 control points"
        }
    }

    // the coordinates of the control points, index w * 16 + v * 4 + u, for evaluation without allocations
    private val xs = DoubleArray(64) { points[it / 16][(it / 4) % 4][it % 4].x }
    private val ys = DoubleArray(64) { points[it / 16][(it / 4) % 4][it % 4].y }
    private val zs = DoubleArray(64) { points[it / 16][(it / 4) % 4][it % 4].z }

    /** the sum of the control points weighted by [a0]..[a3] along u, [b0]..[b3] along v and [c0]..[c3] along w */
    private fun combine(
        a0: Double, a1: Double, a2: Double, a3: Double,
        b0: Double, b1: Double, b2: Double, b3: Double,
        c0: Double, c1: Double, c2: Double, c3: Double
    ): Vector3 {
        var rx = 0.0
        var ry = 0.0
        var rz = 0.0
        for (k in 0 until 4) {
            val c = when (k) { 0 -> c0; 1 -> c1; 2 -> c2; else -> c3 }
            for (j in 0 until 4) {
                val bc = (when (j) { 0 -> b0; 1 -> b1; 2 -> b2; else -> b3 }) * c
                val o = k * 16 + j * 4
                rx += (xs[o] * a0 + xs[o + 1] * a1 + xs[o + 2] * a2 + xs[o + 3] * a3) * bc
                ry += (ys[o] * a0 + ys[o + 1] * a1 + ys[o + 2] * a2 + ys[o + 3] * a3) * bc
                rz += (zs[o] * a0 + zs[o + 1] * a1 + zs[o + 2] * a2 + zs[o + 3] * a3) * bc
            }
        }
        return Vector3(rx, ry, rz)
    }

    /** the point of the volume at [u], [v] and [w], each from 0 to 1 */
    fun position(u: Double, v: Double, w: Double): Vector3 = combine(
        bernstein3(u, 0), bernstein3(u, 1), bernstein3(u, 2), bernstein3(u, 3),
        bernstein3(v, 0), bernstein3(v, 1), bernstein3(v, 2), bernstein3(v, 3),
        bernstein3(w, 0), bernstein3(w, 1), bernstein3(w, 2), bernstein3(w, 3)
    )

    /** the derivative of the volume along u at [u], [v], [w] */
    fun du(u: Double, v: Double, w: Double): Vector3 = combine(
        dbernstein3(u, 0), dbernstein3(u, 1), dbernstein3(u, 2), dbernstein3(u, 3),
        bernstein3(v, 0), bernstein3(v, 1), bernstein3(v, 2), bernstein3(v, 3),
        bernstein3(w, 0), bernstein3(w, 1), bernstein3(w, 2), bernstein3(w, 3)
    )

    /** the derivative of the volume along v at [u], [v], [w] */
    fun dv(u: Double, v: Double, w: Double): Vector3 = combine(
        bernstein3(u, 0), bernstein3(u, 1), bernstein3(u, 2), bernstein3(u, 3),
        dbernstein3(v, 0), dbernstein3(v, 1), dbernstein3(v, 2), dbernstein3(v, 3),
        bernstein3(w, 0), bernstein3(w, 1), bernstein3(w, 2), bernstein3(w, 3)
    )

    /** the derivative of the volume along w at [u], [v], [w] */
    fun dw(u: Double, v: Double, w: Double): Vector3 = combine(
        bernstein3(u, 0), bernstein3(u, 1), bernstein3(u, 2), bernstein3(u, 3),
        bernstein3(v, 0), bernstein3(v, 1), bernstein3(v, 2), bernstein3(v, 3),
        dbernstein3(w, 0), dbernstein3(w, 1), dbernstein3(w, 2), dbernstein3(w, 3)
    )

    /**
     * The Jacobian of the volume at [u], [v], [w]: the matrix whose columns are [du], [dv] and [dw], which maps small
     * steps in u, v and w to the steps of the point they map to. Normals map by its inverse transpose.
     */
    fun jacobian(u: Double, v: Double, w: Double): Matrix33 = positionAndJacobian(u, v, w).second

    /**
     * The [position] and the [jacobian] of the volume at [u], [v], [w] together, in one pass over the control points
     */
    fun positionAndJacobian(u: Double, v: Double, w: Double): Pair<Vector3, Matrix33> {
        val bu0 = bernstein3(u, 0); val bu1 = bernstein3(u, 1); val bu2 = bernstein3(u, 2); val bu3 = bernstein3(u, 3)
        val du0 = dbernstein3(u, 0); val du1 = dbernstein3(u, 1); val du2 = dbernstein3(u, 2); val du3 = dbernstein3(u, 3)
        var px = 0.0; var py = 0.0; var pz = 0.0
        var ux = 0.0; var uy = 0.0; var uz = 0.0
        var vx = 0.0; var vy = 0.0; var vz = 0.0
        var wx = 0.0; var wy = 0.0; var wz = 0.0
        for (k in 0 until 4) {
            val bw = bernstein3(w, k)
            val dw = dbernstein3(w, k)
            for (j in 0 until 4) {
                val bv = bernstein3(v, j)
                val dv = dbernstein3(v, j)
                val o = k * 16 + j * 4
                // the row along u, and its derivative
                val rx = xs[o] * bu0 + xs[o + 1] * bu1 + xs[o + 2] * bu2 + xs[o + 3] * bu3
                val ry = ys[o] * bu0 + ys[o + 1] * bu1 + ys[o + 2] * bu2 + ys[o + 3] * bu3
                val rz = zs[o] * bu0 + zs[o + 1] * bu1 + zs[o + 2] * bu2 + zs[o + 3] * bu3
                val sx = xs[o] * du0 + xs[o + 1] * du1 + xs[o + 2] * du2 + xs[o + 3] * du3
                val sy = ys[o] * du0 + ys[o + 1] * du1 + ys[o + 2] * du2 + ys[o + 3] * du3
                val sz = zs[o] * du0 + zs[o + 1] * du1 + zs[o + 2] * du2 + zs[o + 3] * du3
                val a = bv * bw
                val b = dv * bw
                val c = bv * dw
                px += rx * a; py += ry * a; pz += rz * a
                ux += sx * a; uy += sy * a; uz += sz * a
                vx += rx * b; vy += ry * b; vz += rz * b
                wx += rx * c; wy += ry * c; wz += rz * c
            }
        }
        return Vector3(px, py, pz) to Matrix33.fromColumnVectors(Vector3(ux, uy, uz), Vector3(vx, vy, vz), Vector3(wx, wy, wz))
    }

    /**
     * The point of the volume for the point [position] of the [basis] box: the corner of the box goes to where u, v
     * and w are 0, the opposite corner to where they are 1
     */
    fun deform(basis: Box, position: Vector3): Vector3 {
        val uvw = (position - basis.corner) / Vector3(basis.width, basis.height, basis.depth)
        return position(uvw.x, uvw.y, uvw.z)
    }

    /**
     * The point of the volume for the point [position] of the [basis] box, see [deform], and the [normal] there mapped
     * by the inverse transpose of the Jacobian of that mapping, of unit length; where the Jacobian is singular the
     * normal is kept as it is. One pass over the control points for both.
     */
    fun deform(basis: Box, position: Vector3, normal: Vector3): Pair<Vector3, Vector3> {
        val size = Vector3(basis.width, basis.height, basis.depth)
        val uvw = (position - basis.corner) / size
        val (p, j) = positionAndJacobian(uvw.x, uvw.y, uvw.z)
        // the derivatives by the position in the box: the columns divided by the size of the box
        val ju = Vector3(j.c0r0, j.c0r1, j.c0r2) / size.x
        val jv = Vector3(j.c1r0, j.c1r1, j.c1r2) / size.y
        val jw = Vector3(j.c2r0, j.c2r1, j.c2r2) / size.z
        // the inverse transpose of the Jacobian is its cofactor matrix divided by its determinant
        val r0 = jv.cross(jw)
        val r1 = jw.cross(ju)
        val r2 = ju.cross(jv)
        val det = ju.dot(r0)
        val n = r0 * normal.x + r1 * normal.y + r2 * normal.z
        val mapped = if (n.length > 0.0 && n.x.isFinite()) n.normalized * (if (det < 0.0) -1.0 else 1.0) else normal
        return p to mapped
    }

    /** a volume with its control points moved by [transform] */
    fun map(transform: (Vector3) -> Vector3) = BezierVolume(points.map { plane -> plane.map { row -> row.map(transform) } })

    companion object {
        /**
         * The volume that maps the unit cube to [box] without deforming it: its control points are spread evenly
         * through the box, a starting point for moving them
         */
        fun fromBox(box: Box): BezierVolume = BezierVolume(List(4) { k ->
            List(4) { j ->
                List(4) { i ->
                    box.corner + Vector3(box.width * i / 3.0, box.height * j / 3.0, box.depth * k / 3.0)
                }
            }
        })
    }
}
