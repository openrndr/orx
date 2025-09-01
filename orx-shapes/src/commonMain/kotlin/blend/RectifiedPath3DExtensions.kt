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
    val ts = (0 until other.originalPath.segments.size + 1).map { it.toDouble() / other.originalPath.segments.size }
    val rts = ts.map { other.inverseRectify(it) }

    return Path3D.fromPaths(splitAt(rts), originalPath.closed && other.originalPath.closed).rectified()
}

/**
 * Produces a blended 3D path by mixing two rectified paths using a custom blending function.
 *
 * This function combines segments from the current `RectifiedPath3D` instance with segments
 * from another specified `RectifiedPath3D` instance. Each segment is blended based on a
 * provided blending function that accepts a normalized parameter and returns a blend weight.
 * The resultant path retains characteristics such as closedness if both input paths are closed.
 *
 * @param other the other `RectifiedPath3D` to be blended with the current path
 * @param blendFunction a function that provides blending weights based on a normalized parameter (t-value)
 * @return a `Path3D` representing the blended result of the two input paths
 */
fun RectifiedPath3D.mix(other: RectifiedPath3D, blendFunction: (Double) -> Double): Path3D {
    val n = this.originalPath.segments.size.toDouble()
    val segs = (this.originalPath.segments zip other.originalPath.segments).mapIndexed { index, it ->
        val t0 = inverseRectify(index / n)
        val t1 = inverseRectify((index + 1 / 3.0) / n)
        val t2 = inverseRectify((index + 2 / 3.0) / n)
        val t3 = inverseRectify((index + 1) / n)
        (it.first as Segment3D).mix(it.second as Segment3D, blendFunction(t0), blendFunction(t1), blendFunction(t2), blendFunction(t3))
    }
    return Path3D.fromSegments(segs, originalPath.closed && other.originalPath.closed)
}

