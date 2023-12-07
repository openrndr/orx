package org.openrndr.extra.shapes.adjust

import org.openrndr.extra.shapes.vertex.ContourVertex
import org.openrndr.math.Vector2

class ContourAdjusterVertex(val contourAdjuster: ContourAdjuster, val segmentIndex: () -> Int) {
    private fun wrap(block: ContourVertex.() -> ContourVertex) {
        val newVertex = ContourVertex(contourAdjuster.contour, segmentIndex()).block()
        contourAdjuster.contour = newVertex.contour
        contourAdjuster.updateSelection(newVertex.adjustments)
    }

    val previous: ContourAdjusterVertex?
        get() {
            return if (contourAdjuster.contour.closed || segmentIndex() > 0) {
                ContourAdjusterVertex(contourAdjuster, { (segmentIndex() - 1).mod(contourAdjuster.contour.segments.size) })
            } else {
                null
            }
        }
    val next: ContourAdjusterVertex?
        get() {
            return if (contourAdjuster.contour.closed || segmentIndex() < contourAdjuster.contour.segments.size-1) {
                ContourAdjusterVertex(contourAdjuster, { (segmentIndex() + 1).mod(contourAdjuster.contour.segments.size) })
            } else {
                null
            }
        }


    val t: Double
        get() = ContourVertex(contourAdjuster.contour, segmentIndex(), emptyList()).t


    val position: Vector2
        get() = ContourVertex(contourAdjuster.contour, segmentIndex(), emptyList()).position

    val normal: Vector2
        get() = ContourVertex(contourAdjuster.contour, segmentIndex(), emptyList()).normal


    fun select() {
        contourAdjuster.selectVertex(segmentIndex())
    }

    fun remove(updateTangents: Boolean = true) = wrap { remove(updateTangents) }
    fun moveBy(translation: Vector2, updateTangents: Boolean = true) = wrap { movedBy(translation, updateTangents) }
    fun moveTo(position: Vector2, updateTangents: Boolean = true) = wrap { movedBy(position - this.position, updateTangents) }
    fun rotate(rotationInDegrees: Double) = wrap { rotatedBy(rotationInDegrees) }
    fun scale(scaleFactor: Double) = wrap { scaledBy(scaleFactor) }

    fun rotate(rotationInDegrees: Double, anchor: Vector2) = wrap { rotatedBy(rotationInDegrees, anchor) }
    fun scale(scaleFactor: Double, anchor: Vector2) = wrap { scaledBy(scaleFactor, anchor) }

}