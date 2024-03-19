package org.openrndr.extra.shapes.blend

import org.openrndr.extra.shapes.rectify.RectifiedContour
import org.openrndr.extra.shapes.rectify.RectifiedPath3D
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.shape.Path3D
import org.openrndr.shape.ShapeContour

/**
 * ContourBlend holds two rectified contours with an equal amount of segments
 */
class Path3DBlend(val a: RectifiedPath3D, val b: RectifiedPath3D) {
    fun mix(blendFunction: (Double) -> Double): Path3D {
        return a.mix(b, blendFunction)
    }

    fun mix(blend: Double): Path3D {
        return a.mix(b) { blend }
    }
}

/**
 * Create a [ContourBlend] for contours [a] and [b]
 *
 * Finding the pose that minimizes the error between [a] and [b] is not part of this function's work.
 *
 */
fun Path3DBlend(a: Path3D, b: Path3D): Path3DBlend {
    val ra = a.rectified()
    val rb = b.rectified()
    val sa = ra.splitForBlend(rb)
    val sb = rb.splitForBlend(ra)
    require(sa.path.segments.size == sb.path.segments.size) {
        "preprocessing for contours failed to produce equal number of segments. ${sa.path.segments.size}, ${sb.path.segments.size}"
    }
    return Path3DBlend(sa, sb)
}