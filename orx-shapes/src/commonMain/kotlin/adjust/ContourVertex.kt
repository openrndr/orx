package org.openrndr.extra.shapes.vertex

import org.openrndr.extra.shapes.adjust.ContourAdjuster
import org.openrndr.extra.shapes.adjust.SegmentAdjustments
import org.openrndr.extra.shapes.adjust.SegmentOperation
import org.openrndr.extra.shapes.adjust.adjust
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.buildTransform
import org.openrndr.shape.ShapeContour

data class ContourVertex(val contour: ShapeContour, val segmentIndex: Int, val adjustments: List<SegmentOperation> = emptyList())  {
    fun withoutAdjustments() : ContourVertex {
        return if (adjustments.isEmpty()) {
            this
        } else {
            copy(adjustments = emptyList())
        }
    }

    val position: Vector2
        get() {
            return contour.segments[segmentIndex].start
        }

    fun remove(updateTangents: Boolean = true) : ContourVertex {
        if (contour.empty) {
            return withoutAdjustments()
        }
        val segmentInIndex = if (contour.closed) (segmentIndex-1).mod(contour.segments.size) else segmentIndex-1
        val segmentOutIndex = if (contour.closed) (segmentIndex+1).mod(contour.segments.size) else segmentIndex+1
        val newSegments = contour.segments.map { it }.toMutableList()
        val refIn = newSegments.getOrNull(segmentInIndex)
        val refOut = newSegments.getOrNull(segmentOutIndex)

        val segment = newSegments[segmentIndex]
        if (refIn != null) {
            newSegments[segmentInIndex] = refIn.copy(end = segment.end)
        }
        val adjustments = newSegments.adjust {
            removeAt(segmentIndex)
        }
        return ContourVertex(ShapeContour.fromSegments(newSegments, contour.closed), segmentIndex, adjustments)
    }

    fun scaledBy(scaleFactor: Double): ContourVertex {
        if (contour.empty) {
            return withoutAdjustments()
        }
        val transform = buildTransform {
            translate(position)
            this.scale(scaleFactor)
            translate(-position)
        }
        return transformTangents(transform, transform)
    }

    fun rotatedBy(rotationInDegrees: Double): ContourVertex {
        if (contour.empty) {
            return withoutAdjustments()
        }
        val transform = buildTransform {
            translate(position)
            this.rotate(rotationInDegrees)
            translate(-position)
        }
        return transformTangents(transform, transform)
    }

    fun transformTangents(transformIn: Matrix44, transformOut: Matrix44 = transformIn): ContourVertex {
        if (contour.empty) {
            return withoutAdjustments()
        }
        val newSegments = contour.segments.map { it }.toMutableList()
        val refOut = contour.segments[segmentIndex]
        val refIn = if (contour.closed) contour.segments[(segmentIndex - 1).mod(contour.segments.size)] else
            contour.segments.getOrNull(segmentIndex - 1)
        newSegments[segmentIndex] = run {
            val cubicSegment = refOut.cubic
            val newControls = arrayOf((transformOut * cubicSegment.control[0].xy01).xy, cubicSegment.control[1])
            refOut.copy(control = newControls)
        }
        val segmentIndexIn = (segmentIndex - 1).mod(contour.segments.size)
        if (refIn != null) {
            newSegments[segmentIndexIn] = run {
                val cubicSegment = refIn.cubic
                val newControls = arrayOf(cubicSegment.control[0], (transformIn * cubicSegment.control[1].xy01).xy)
                refIn.copy(control = newControls)
            }
        }
        val newContour = ShapeContour.fromSegments(newSegments, contour.closed, contour.polarity)

        return ContourVertex(newContour, segmentIndex)

    }

    fun movedBy(translation: Vector2, updateTangents: Boolean = true): ContourVertex {
        if (contour.empty) {
            return withoutAdjustments()
        }

        val newSegments = contour.segments.map { it }.toMutableList()
        val refOut = contour.segments[segmentIndex]
        val refIn = if (contour.closed) contour.segments[(segmentIndex - 1).mod(contour.segments.size)] else
            contour.segments.getOrNull(segmentIndex - 1)
        val newPosition = refOut.start + translation
        newSegments[segmentIndex] = if (updateTangents && !refOut.linear) {
            val cubicSegment = refOut.cubic
            val newControls = arrayOf(cubicSegment.control[0] + translation, cubicSegment.control[1])
            refOut.copy(start = newPosition, control = newControls)
        } else {
            newSegments[segmentIndex].copy(start = newPosition)
        }

        val segmentIndexIn = (segmentIndex - 1).mod(contour.segments.size)
        if (refIn != null) {
            newSegments[segmentIndexIn] =
                if (updateTangents && !refIn.linear) {
                    val cubicSegment = refIn.cubic
                    val newControls = arrayOf(cubicSegment.control[0], cubicSegment.control[1] + translation)
                    newSegments[segmentIndexIn].copy(control = newControls, end = newPosition)
                } else {

                    newSegments[segmentIndexIn].copy(end = newPosition)
                }
        }
        val newContour = ShapeContour.fromSegments(newSegments, contour.closed, contour.polarity)

        return ContourVertex(newContour, segmentIndex)
    }
}

