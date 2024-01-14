package org.openrndr.extra.shapes.vertex

import org.openrndr.extra.shapes.adjust.*
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.buildTransform
import org.openrndr.shape.ShapeContour

data class ContourVertex(
    val contour: ShapeContour,
    val segmentIndex: Int,
    val adjustments: List<SegmentOperation> = emptyList()
) {
    fun withoutAdjustments(): ContourVertex {
        return if (adjustments.isEmpty()) {
            this
        } else {
            copy(adjustments = emptyList())
        }
    }

    val normal: Vector2
        get() {
            return if (contour.closed || segmentIndex > 0 || segmentIndex < contour.segments.size) {
                val segmentIn = contour.segments[(segmentIndex - 1).mod(contour.segments.size)]
                val normalIn = segmentIn.normal(1.0)
                val normalOut = contour.segments[segmentIndex].normal(0.0)
                (normalIn + normalOut).normalized
            } else if (segmentIndex == 0) {
                contour.normal(0.0)
            } else if (segmentIndex == contour.segments.size) {
                contour.normal(1.0)
            } else {
                error("segmentIndex out of bounds ${segmentIndex} >= ${contour.segments.size}")
            }
        }

    val t: Double
        get() = segmentIndex.toDouble() / contour.segments.size

    val position: Vector2
        get() {
            return if (contour.closed || segmentIndex < contour.segments.size) {
                contour.segments[segmentIndex].start
            } else {
                contour.segments[segmentIndex - 1].end
            }
        }

    fun remove(updateTangents: Boolean = true): ContourVertex {
        if (contour.empty) {
            return withoutAdjustments()
        }
        val segmentInIndex = if (contour.closed) (segmentIndex - 1).mod(contour.segments.size) else segmentIndex - 1
        val segmentOutIndex = if (contour.closed) (segmentIndex + 1).mod(contour.segments.size) else segmentIndex + 1
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
            this@buildTransform.rotate(rotationInDegrees)
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
            val newControls = listOf((transformOut * cubicSegment.control[0].xy01).xy, cubicSegment.control[1])
            refOut.copy(control = newControls)
        }
        val segmentIndexIn = (segmentIndex - 1).mod(contour.segments.size)
        if (refIn != null) {
            newSegments[segmentIndexIn] = run {
                val cubicSegment = refIn.cubic
                val newControls = listOf(cubicSegment.control[0], (transformIn * cubicSegment.control[1].xy01).xy)
                refIn.copy(control = newControls)
            }
        }
        val newContour = ShapeContour.fromSegments(newSegments, contour.closed, contour.polarity)

        return ContourVertex(newContour, segmentIndex)

    }

    fun movedBy(translation: Vector2, updateTangents: Boolean = true): ContourVertex =
        transformedBy(buildTransform { translate(translation) }, updateTangents)

    fun rotatedBy(rotationInDegrees: Double, anchor: Vector2, updateTangents: Boolean = true): ContourVertex {
        return transformedBy(buildTransform {
            translate(anchor)
            rotate(rotationInDegrees)
            translate(-anchor)
        }, updateTangents)
    }

    fun scaledBy(scaleFactor: Double, anchor: Vector2, updateTangents: Boolean = true): ContourVertex {
        return transformedBy(buildTransform {
            translate(anchor)
            scale(scaleFactor)
            translate(-anchor)
        }, updateTangents)
    }

    fun transformedBy(transform: Matrix44, updateTangents: Boolean): ContourVertex {
        return transformedBy(updateTangents) { v -> v.transformedBy(transform) }
    }

    fun transformedBy(updateTangents: Boolean = true, transform: (Vector2) -> Vector2): ContourVertex {
        if (contour.empty) {
            return withoutAdjustments()
        }

        val newSegments = contour.segments.map { it }.toMutableList()
        val refOut = if (contour.closed || segmentIndex < contour.segments.size) {
            contour.segments[segmentIndex]
        } else {
            contour.segments.last()
        }
        val refIn = if (contour.closed) {
            contour.segments[(segmentIndex - 1).mod(contour.segments.size)]
        } else {
            contour.segments.getOrNull(segmentIndex - 1)
        }
        val newPosition = if (contour.closed || segmentIndex < contour.segments.size) {
            transform(refOut.start)
        } else {
            transform(refOut.end)
        }

        if (contour.closed || segmentIndex< contour.segments.size) {
            newSegments[segmentIndex] = if (updateTangents && !refOut.linear) {
                val cubicSegment = refOut.cubic
                val newControls = listOf(transform(cubicSegment.control[0]), cubicSegment.control[1])
                refOut.copy(start = newPosition, control = newControls)
            } else {
                newSegments[segmentIndex].copy(start = newPosition)
            }
        } else {
            newSegments[segmentIndex-1] = if (updateTangents && !refOut.linear) {
                val cubicSegment = refOut.cubic
                val newControls = listOf(cubicSegment.control[0], transform(cubicSegment.control[1]))
                refOut.copy(end = newPosition, control = newControls)
            } else {
                newSegments[segmentIndex-1].copy(end = newPosition)
            }
        }

        val segmentIndexIn = (segmentIndex - 1).mod(contour.segments.size)
        if (refIn != null && (contour.closed || segmentIndex < contour.segments.size)) {
            newSegments[segmentIndexIn] =
                if (updateTangents && !refIn.linear) {
                    val cubicSegment = refIn.cubic
                    val newControls = listOf(cubicSegment.control[0], transform(cubicSegment.control[1]))
                    newSegments[segmentIndexIn].copy(control = newControls, end = newPosition)
                } else {

                    newSegments[segmentIndexIn].copy(end = newPosition)
                }
        }
        for (s in newSegments.windowed(2, 1)) {
            require(s[0].end.distanceTo(s[1].start) < 1E-3)
        }
        val newContour = ShapeContour.fromSegments(newSegments, contour.closed, contour.polarity)

        return ContourVertex(newContour, segmentIndex)
    }
}

