package org.openrndr.extra.shapes.rectify

import org.openrndr.math.Vector3
import org.openrndr.shape.Path3D
import kotlin.math.floor

class RectifiedPath3D(
    contour: Path3D,
    distanceTolerance: Double = 0.5,
    lengthScale: Double = 1.0,
    lengthTolerance: Double = 1E-3
) :
    RectifiedPath<Vector3>(contour, distanceTolerance, lengthScale, lengthTolerance) {

    val path: Path3D get() = originalPath as Path3D

    /**
     * The rectified t of the point of the path nearest to [query]: the best of samples along the path, refined
     * between its neighbours by golden section search
     */
    fun nearest(query: Vector3): Double {
        if (path.empty) return 0.0
        val count = maxOf(64, path.segments.size * 32)
        val closed = path.closed
        fun distance(t: Double) = position(if (closed) t.mod(1.0) else t.coerceIn(0.0, 1.0)).squaredDistanceTo(query)
        var best = 0
        var bestDistance = Double.POSITIVE_INFINITY
        for (i in 0..count) {
            val d = distance(i.toDouble() / count)
            if (d < bestDistance) {
                bestDistance = d
                best = i
            }
        }
        var lo = (best - 1).toDouble() / count
        var hi = (best + 1).toDouble() / count
        if (!closed) {
            lo = lo.coerceAtLeast(0.0)
            hi = hi.coerceAtMost(1.0)
        }
        val g = (kotlin.math.sqrt(5.0) - 1.0) / 2.0
        var a = hi - g * (hi - lo)
        var b = lo + g * (hi - lo)
        var fa = distance(a)
        var fb = distance(b)
        repeat(48) {
            if (fa < fb) {
                hi = b; b = a; fb = fa
                a = hi - g * (hi - lo); fa = distance(a)
            } else {
                lo = a; a = b; fa = fb
                b = lo + g * (hi - lo); fb = distance(b)
            }
        }
        val t = (lo + hi) / 2.0
        return if (closed) t.mod(1.0) else t.coerceIn(0.0, 1.0)
    }

    override fun sub(t0: Double, t1: Double): Path3D {
        if (path.empty) {
            return Path3D(emptyList(), false)
        }

        return if (path.closed) {
            path.sub(rectify(t0.mod(1.0)) + floor(t0), rectify(t1.mod(1.0)) + floor(t1))
        } else {
            path.sub(rectify(t0), rectify(t1))
        }
    }

    override fun splitAt(ascendingTs: List<Double>, weldEpsilon: Double): List<Path3D> {
        @Suppress("UNCHECKED_CAST")
        return super.splitAt(ascendingTs, weldEpsilon) as List<Path3D>
    }
}
