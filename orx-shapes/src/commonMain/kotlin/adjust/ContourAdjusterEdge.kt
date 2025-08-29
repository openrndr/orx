package org.openrndr.extra.shapes.adjust

import org.openrndr.math.Vector2
import org.openrndr.shape.ShapeContour
import kotlin.jvm.JvmRecord

@JvmRecord
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

    val length: Double
        get() {
            return contourAdjuster.contour.segments[segmentIndex()].length
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

    internal fun wrap(block: ContourEdge.() -> ContourEdge) {
        val newEdge = ContourEdge(contourAdjuster.contour, segmentIndex()).block()
        contourAdjuster.contour = newEdge.contour
        contourAdjuster.updateSelection(newEdge.adjustments)
    }

    /**
     * Convert the edge to a linear edge, truncating control points if those exist
     */
    fun toLinear() = wrap { toLinear() }

    /**
     * Convert the edge to a cubic edge
     */
    fun toCubic() = wrap { toCubic() }

    /**
     * Split the edge at [t]
     * @param t an edge t value between 0 and 1. No splitting happens when t == 0 or t == 1.
     */
    fun splitAt(t: Double) = wrap { splitAt(t) }

    /**
     * Split the edge in [numberOfParts] parts of equal length
     */
    fun splitIn(numberOfParts: Int) = wrap { splitIn(numberOfParts) }

    /**
     * Creates a new contour edge by applying a translation to the current edge.
     *
     * @param translation the translation vector to apply to the contour edge.
     * @param updateTangents whether the tangents of adjacent segments should be updated after the transformation.
     * @return a new instance of the contour edge, transformed by the given translation.
     */
    fun moveBy(translation: Vector2, updateTangents: Boolean = true) = wrap { movedBy(translation, updateTangents) }

    /**
     * Rotates the current edge by a specified angle around an anchor point relative to the edge.
     * Optionally updates the tangents of adjacent segments after the rotation.
     *
     * @param rotationInDegrees the rotation angle in degrees to be applied.
     * @param anchorT the relative position along the edge (range 0.0 to 1.0) defining the anchor point of rotation. Defaults to 0.5 (the center of the edge).
     * @param updateTangents whether the tangents of adjacent segments should be updated after the rotation. Defaults to true.
     */
    fun rotate(rotationInDegrees: Double, anchorT: Double = 0.5, updateTangents: Boolean = true) =
        wrap { rotatedBy(rotationInDegrees, anchorT, updateTangents) }

    /**
     * Scales the current edge by a specified factor, with an optional anchor point determining the
     * scaling center along the edge. The scaling operation updates the tangents of the edge.
     */
    fun scale(scaleFactor: Double, anchorT: Double = 0.5) =
        wrap { scaledBy(scaleFactor, anchorT, updateTangents = true) }

    /**
     * Replace this edge with a point at [t]
     * @param t an edge t value between 0 and 1
     */
    fun replaceWith(t: Double) = wrap { replacedWith(t) }

    /**
     * Replaces the current edge with the segments of an open shape contour.
     *
     * @param openContour the open shape contour whose segments replace the current edge. The provided
     *                    contour must not be closed.
     * @return a new ContourEdge instance with the updated segments from the `openContour`.
     */
    fun replaceWith(openContour: ShapeContour) = wrap { replacedWith(openContour) }

    /**
     * Returns part of the edge between [t0] to [t1].
     * Preserves topology unless t0 = t1.
     * @param t0 the edge's start t-value, between 0 and 1
     * @param t1 the edge's end t-value, between 0 and 1
     */
    fun sub(t0: Double, t1: Double) {
        contourAdjuster.contour =
            ContourEdge(contourAdjuster.contour, segmentIndex())
                .subbed(t0, t1)
                .contour
    }

    /**
     * Moves the starting point of the contour edge by the given translation vector.
     *
     * @param translation the translation vector to apply to the starting point of the contour edge.
     * @param updateTangents whether the tangents of adjacent segments should be updated after the transformation. Defaults to true.
     * @return a new instance of the contour edge with the starting point moved by the given translation.
     */
    fun moveStartBy(translation: Vector2, updateTangents: Boolean = true) = wrap { startMovedBy(translation, updateTangents) }

    /**
     * Moves the first control point of a contour edge by a specified translation vector.
     *
     * @param translation the translation vector to apply to the first control point of the contour edge.
     * @return a new instance of the contour edge with the first control point moved by the given translation.
     */
    fun moveControl0By(translation: Vector2) = wrap { control0MovedBy(translation) }

    /**
     * Moves the second control point (Control1) of a contour edge by a specified translation vector.
     *
     * @param translation the translation vector to apply to the second control point of the contour edge.
     * @return a new instance of the contour edge with the second control point moved by the given translation.
     */
    fun moveControl1By(translation: Vector2) = wrap { control1MovedBy(translation) }

    /**
     * Moves the end point of the contour edge by the specified translation vector.
     *
     * @param translation the translation vector to apply to the end point of the contour edge.
     * @param updateTangents whether the tangents of adjacent segments should be updated after the transformation. Defaults to true.
     * @return a new instance of the contour edge with the end point moved by the given translation.
     */
    fun moveEndBy(translation: Vector2, updateTangents: Boolean = true) = wrap { startMovedBy(translation, updateTangents) }
}
