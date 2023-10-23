package org.openrndr.extra.shapes.adjust

import org.openrndr.extra.shapes.vertex.ContourVertex
import org.openrndr.math.Vector2

class ContourAdjusterVertex(val contourAdjuster: ContourAdjuster, val segmentIndex: Int) {
    private fun wrap(block: ContourVertex.() -> ContourVertex) {
        val newVertex =  ContourVertex(contourAdjuster.contour, segmentIndex).block()
        contourAdjuster.contour = newVertex.contour
        contourAdjuster.updateSelection(newVertex.adjustments)
    }
    fun select() {
        contourAdjuster.selectVertex(segmentIndex)
    }
    fun remove(updateTangents: Boolean = true) = wrap { remove(updateTangents) }
    fun moveBy(translation: Vector2, updateTangents: Boolean = true) = wrap { movedBy(translation, updateTangents) }
    fun rotate(rotationInDegrees: Double) = wrap { rotatedBy(rotationInDegrees) }
    fun scale(scaleFactor: Double) = wrap { scaledBy(scaleFactor) }

}