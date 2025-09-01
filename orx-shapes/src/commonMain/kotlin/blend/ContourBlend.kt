package org.openrndr.extra.shapes.blend

import org.openrndr.extra.shapes.rectify.RectifiedContour
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.shape.ShapeContour

/**
 * A utility class for blending between two rectified contours.
 *
 * The `ContourBlend` class facilitates blending operations between two
 * `RectifiedContour` instances, assuming that they have a compatible structure
 * with an equal number of segments.
 *
 * @constructor Creates a `ContourBlend` instance with two provided contours.
 * @param a The first `RectifiedContour` to blend.
 * @param b The second `RectifiedContour` to blend.
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
 * Creates a ContourBlend instance for blending between two ShapeContour instances.
 *
 * This function rectifies the provided contours, splits them into segments suitable for blending,
 * and verifies that both resulting rectified contours have an equal number of segments.
 *
 * @param a the first ShapeContour to blend
 * @param b the second ShapeContour to blend
 * @param distanceTolerance the distance tolerance used in the rectification preprocess of [a] and [b]
 * @param lengthScale the length scale used in the rectification preprocess of [a] and [b]
 * @return a ContourBlend instance representing the blended contours
 * @throws IllegalArgumentException if the preprocessing for contours fails to produce an equal number of segments
 */
fun ContourBlend(
    a: ShapeContour,
    b: ShapeContour,
    distanceTolerance: Double = 0.5,
    lengthScale: Double = 1.0
): ContourBlend {
    val ra = a.rectified(distanceTolerance, lengthScale)
    val rb = b.rectified(distanceTolerance, lengthScale)
    val sa = ra.splitForBlend(rb)
    val sb = rb.splitForBlend(ra)
    require(sa.originalPath.segments.size == sb.originalPath.segments.size) {
        "preprocessing for contours failed to produce equal number of segments. ${sa.originalPath.segments.size}, ${sb.originalPath.segments.size}"
    }
    return ContourBlend(sa, sb)
}