package org.openrndr.extra.shapes.bezierpatches

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

    private fun coeffs3(t: Double): DoubleArray {
        val it = 1.0 - t
        return doubleArrayOf(it * it * it, 3 * it * it * t, 3 * it * t * t, t * t * t)
    }

    /** the point of the volume at [u], [v] and [w], each from 0 to 1 */
    fun position(u: Double, v: Double, w: Double): Vector3 {
        val cu = coeffs3(u)
        val cv = coeffs3(v)
        val cw = coeffs3(w)
        var result = Vector3.ZERO
        for (k in 0 until 4) for (j in 0 until 4) for (i in 0 until 4) {
            result += points[k][j][i] * (cu[i] * cv[j] * cw[k])
        }
        return result
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
