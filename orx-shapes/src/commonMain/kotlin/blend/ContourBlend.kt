package org.openrndr.extra.shapes.blend

import org.openrndr.extra.shapes.rectify.RectifiedContour
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.shape.ShapeContour

/**
 * ContourBlend holds two rectified contours with an equal amount of segments
 */
class ContourBlend(val a: RectifiedContour, val b: RectifiedContour) {
    fun mix(blendFunction: (Double) -> Double): ShapeContour {
        return a.mix(b, blendFunction)
    }

    fun mix(blend: Double): ShapeContour {
        return a.mix(b) { blend }
    }
}

/**
 * Create a [ContourBlend] for contours [a] and [b]
 *
 * Finding the pose that minimizes the error between [a] and [b] is not part of this function's work.
 *
 */
fun ContourBlend(a: ShapeContour, b: ShapeContour): ContourBlend {
    val ra = a.rectified()
    val rb = b.rectified()
    val sa = ra.splitForBlend(rb)
    val sb = rb.splitForBlend(ra)
    require(sa.contour.segments.size == sb.contour.segments.size) {
        "preprocessing for contours failed to produce equal number of segments. ${sa.contour.segments.size}, ${sb.contour.segments.size}"
    }
    return ContourBlend(sa, sb)
}