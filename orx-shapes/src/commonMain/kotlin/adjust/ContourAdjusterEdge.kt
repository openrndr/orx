package org.openrndr.extra.shapes.adjust

import org.openrndr.math.Vector2
import org.openrndr.shape.ShapeContour

data class ContourAdjusterEdge(val contourAdjuster: ContourAdjuster, val segmentIndex: () -> Int) {

    val startPosition
        get() = contourAdjuster.contour.segments[segmentIndex()].start

    val endPosition
        get() = contourAdjuster.contour.segments[segmentIndex()].end

    fun position(t: Double): Vector2 {
        return contourAdjuster.contour.segments[segmentIndex()].position(t)
    }

    fun normal(t: Double): Vector2 {
        return contourAdjuster.contour.segments[segmentIndex()].normal(t)
    }


    /**
     * A [ContourAdjusterVertex] interface for the start-vertex of the edge
     */
    val start
        get() = ContourAdjusterVertex(contourAdjuster, segmentIndex)

    /**
     * A [ContourAdjusterVertex] interface for the end-vertex of the edge
     */
    val end
        get() = ContourAdjusterVertex(
            contourAdjuster,
            { (segmentIndex() + 1).mod(contourAdjuster.contour.segments.size) })

    /**
     * A link to the edge before this edge
     */
    val previous: ContourAdjusterEdge?
        get() = if (contourAdjuster.contour.closed) {
            this.copy(segmentIndex = { (segmentIndex() - 1).mod(contourAdjuster.contour.segments.size) })
        } else {
            if (segmentIndex() > 0) {
                this.copy(segmentIndex = { segmentIndex() - 1 })
            } else {
                null
            }
        }

    /**
     * A link to the edge after this edge
     */
    val next: ContourAdjusterEdge?
        get() = if (contourAdjuster.contour.closed) {
            this.copy(segmentIndex = { (segmentIndex() + 1).mod(contourAdjuster.contour.segments.size) })
        } else {
            if (segmentIndex() < contourAdjuster.contour.segments.size - 1) {
                this.copy(segmentIndex = { segmentIndex() + 1 })
            } else {
                null
            }
        }

    fun select() {
        contourAdjuster.selectEdge(segmentIndex())
    }

    private fun wrap(block: ContourEdge.() -> ContourEdge) {
        val newEdge = ContourEdge(contourAdjuster.contour, segmentIndex()).block()
        contourAdjuster.contour = newEdge.contour
        contourAdjuster.updateSelection(newEdge.adjustments)
    }

    fun toLinear() = wrap { toLinear() }

    fun toCubic() = wrap { toCubic() }
    fun splitAt(t: Double) = wrap { splitAt(t) }

    /**
     * split edge in [numberOfParts] parts of equal length
     */
    fun splitIn(numberOfParts: Int) = wrap { splitIn(numberOfParts) }

    fun moveBy(translation: Vector2, updateTangents: Boolean = true) = wrap { movedBy(translation, updateTangents) }
    fun rotate(rotationInDegrees: Double, anchorT: Double = 0.5, updateTangents: Boolean = true) =
        wrap { rotatedBy(rotationInDegrees, anchorT, updateTangents) }

    fun scale(scaleFactor: Double, anchorT: Double = 0.5, updateTangents: Boolean = true) =
        wrap { scaledBy(scaleFactor, anchorT, updateTangents = true) }

    fun replaceWith(t: Double, updateTangents: Boolean = true) = wrap { replacedWith(t, updateTangents) }

    fun replaceWith(openContour: ShapeContour) = wrap { replacedWith(openContour) }


    fun sub(t0: Double, t1: Double, updateTangents: Boolean = true) {
        contourAdjuster.contour =
            ContourEdge(contourAdjuster.contour, segmentIndex())
                .subbed(t0, t1)
                .contour
    }
}
