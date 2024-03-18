package org.openrndr.extra.shapes.blend

import org.openrndr.extra.shapes.rectify.RectifiedContour
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.extra.shapes.utilities.fromContours
import org.openrndr.shape.ShapeContour

/**
 * Split for blending with [other]
 */
fun RectifiedContour.splitForBlend(other: RectifiedContour): RectifiedContour {
    val ts = (0 until other.contour.segments.size + 1).map { it.toDouble() / other.contour.segments.size }
    val rts = ts.map { other.inverseRectify(it) }

    return ShapeContour.fromContours(splitAt(rts), contour.closed && other.contour.closed).rectified()
}

fun RectifiedContour.mix(other: RectifiedContour, blendFunction: (Double) -> Double): ShapeContour {
    val n = this.contour.segments.size.toDouble()
    val segs = (this.contour.segments zip other.contour.segments).mapIndexed { index, it ->
        val t0 = inverseRectify(index / n)
        val t1 = inverseRectify((index + 1 / 3.0) / n)
        val t2 = inverseRectify((index + 2 / 3.0) / n)
        val t3 = inverseRectify((index + 1) / n)

        it.first.mix(it.second, blendFunction(t0), blendFunction(t1), blendFunction(t2), blendFunction(t3))
    }
    return ShapeContour.fromSegments(segs, contour.closed && other.contour.closed)
}

