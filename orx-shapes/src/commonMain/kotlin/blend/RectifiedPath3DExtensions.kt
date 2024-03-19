package org.openrndr.extra.shapes.blend

import org.openrndr.extra.shapes.rectify.RectifiedPath3D
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.extra.shapes.utilities.fromPaths
import org.openrndr.shape.Path3D
import org.openrndr.shape.Segment3D

/**
 * Split for blending with [other]
 */
fun RectifiedPath3D.splitForBlend(other: RectifiedPath3D): RectifiedPath3D {
    val ts = (0 until other.path.segments.size + 1).map { it.toDouble() / other.path.segments.size }
    val rts = ts.map { other.inverseRectify(it) }

    return Path3D.fromPaths(splitAt(rts), path.closed && other.path.closed).rectified()
}

fun RectifiedPath3D.mix(other: RectifiedPath3D, blendFunction: (Double) -> Double): Path3D {
    val n = this.path.segments.size.toDouble()
    val segs = (this.path.segments zip other.path.segments).mapIndexed { index, it ->
        val t0 = inverseRectify(index / n)
        val t1 = inverseRectify((index + 1 / 3.0) / n)
        val t2 = inverseRectify((index + 2 / 3.0) / n)
        val t3 = inverseRectify((index + 1) / n)
        (it.first as Segment3D).mix(it.second as Segment3D, blendFunction(t0), blendFunction(t1), blendFunction(t2), blendFunction(t3))
    }
    return Path3D.fromSegments(segs, path.closed && other.path.closed)
}

