package org.openrndr.extra.shapes.adjust

import org.openrndr.extra.shapes.utilities.insertPointAt
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.buildTransform
import org.openrndr.shape.SegmentType
import org.openrndr.shape.ShapeContour
import kotlin.math.abs

private fun Vector2.transformedBy(t: Matrix44) = (t * (this.xy01)).xy
fun <E> List<E>.update(vararg updates: Pair<Int, E>): List<E> {
    if (updates.isEmpty()) {
        return this
    }
    val result = this.toMutableList()
    for ((index, value) in updates) {
        result[index] = value
    }
    return result
}

/**
 * Helper for querying and adjusting [ShapeContour].
 * * An edge embodies exactly the same thing as a [Segment][org.openrndr.shape.Segment]
 * * All edge operations are immutable and will create a new [ContourEdge] pointing to a copied and updated [ShapeContour]
 */
data class ContourEdge(
    val contour: ShapeContour,
    val segmentIndex: Int,
    val adjustments: List<SegmentOperation> = emptyList()
) {
    fun withoutAdjustments(): ContourEdge {
        return if (adjustments.isEmpty()) {
            this
        } else {
            copy(adjustments = emptyList())
        }
    }


    /**
     *
     */
    fun toCubic(): ContourEdge {
        if (contour.segments[segmentIndex].type != SegmentType.CUBIC) {
            val newSegment = contour.segments[segmentIndex].cubic
            val newSegments = contour.segments
                .update(segmentIndex to newSegment)

            return ContourEdge(
                ShapeContour.fromSegments(newSegments, contour.closed),
                segmentIndex
            )
        } else {
            return this
        }
    }

    /**
     *
     */
    fun replacedWith(t: Double, updateTangents: Boolean): ContourEdge {
        if (contour.empty) {
            return withoutAdjustments()
        }
        val point = contour.segments[segmentIndex].position(t)
        val segmentInIndex = if (contour.closed) (segmentIndex - 1).mod(contour.segments.size) else segmentIndex - 1
        val segmentOutIndex = if (contour.closed) (segmentIndex + 1).mod(contour.segments.size) else segmentIndex + 1
        val refIn = contour.segments.getOrNull(segmentInIndex)
        val refOut = contour.segments.getOrNull(segmentOutIndex)

        val newSegments = contour.segments.toMutableList()
        if (refIn != null) {
            newSegments[segmentInIndex] = newSegments[segmentInIndex].copy(end = point)
        }
        if (refOut != null) {
            newSegments[segmentOutIndex] = newSegments[segmentOutIndex].copy(start = point)
        }
        val adjustments = newSegments.adjust {
            removeAt(segmentIndex)
        }
        return ContourEdge(ShapeContour.fromSegments(newSegments, contour.closed), segmentIndex, adjustments)
    }

    fun subbed(t0: Double, t1: Double, updateTangents: Boolean = true): ContourEdge {
        if (contour.empty) {
            return withoutAdjustments()
        }
        if (abs(t0 -t1) > 1E-6) {
            val sub = contour.segments[segmentIndex].sub(t0, t1)
            val segmentInIndex = if (contour.closed) (segmentIndex - 1).mod(contour.segments.size) else segmentIndex - 1
            val segmentOutIndex =
                if (contour.closed) (segmentIndex + 1).mod(contour.segments.size) else segmentIndex + 1
            val refIn = contour.segments.getOrNull(segmentInIndex)
            val refOut = contour.segments.getOrNull(segmentOutIndex)

            val newSegments = contour.segments.toMutableList()
            if (refIn != null) {
                newSegments[segmentInIndex] = newSegments[segmentInIndex].copy(end = sub.start)
            }
            if (refOut != null) {
                newSegments[segmentOutIndex] = newSegments[segmentOutIndex].copy(start = sub.end)
            }
            newSegments[segmentIndex] = sub
            return ContourEdge(ShapeContour.fromSegments(newSegments, contour.closed), segmentIndex)
        } else {
            return replacedWith(t0, updateTangents)
        }
    }

    fun splitAt(t: Double): ContourEdge {
        if (contour.empty) {
            return withoutAdjustments()
        }
        val newContour = contour.insertPointAt(segmentIndex, t)
        if (newContour.segments.size == contour.segments.size + 1) {
            return ContourEdge(newContour, segmentIndex, listOf(SegmentOperation.Insert(segmentIndex + 1, 1)))
        } else {
            return this.copy(adjustments = emptyList())
        }
    }

    fun transformedBy(transform: Matrix44, updateTangents: Boolean = true): ContourEdge {
        val segment = contour.segments[segmentIndex]
        val newSegment = segment.copy(
            start = segment.start.transformedBy(transform),
            control = segment.control.map { it.transformedBy(transform) }.toTypedArray<Vector2>(),
            end = segment.end.transformedBy(transform)
        )
        val segmentInIndex = if (contour.closed) (segmentIndex - 1).mod(contour.segments.size) else segmentIndex - 1
        val segmentOutIndex = if (contour.closed) (segmentIndex + 1).mod(contour.segments.size) else segmentIndex + 1
        val refIn = contour.segments.getOrNull(segmentInIndex)
        val refOut = contour.segments.getOrNull(segmentOutIndex)


        val newSegments = contour.segments.map { it }.toMutableList()

        if (refIn != null) {
            val control = if (refIn.linear || !updateTangents) {
                refIn.control
            } else {
                refIn.cubic.control
            }
            control[1] = control[1].transformedBy(transform)
            newSegments[segmentInIndex] = refIn.copy(end = segment.start.transformedBy(transform), control = control)
        }
        if (refOut != null) {
            val control = if (refOut.linear || !updateTangents) {
                refOut.control
            } else {
                refOut.cubic.control
            }
            control[0] = control[0].transformedBy(transform)
            newSegments[segmentOutIndex] = refOut.copy(start = segment.end.transformedBy(transform))
        }

        newSegments[segmentIndex] = newSegment
        return ContourEdge(ShapeContour.fromSegments(newSegments, contour.closed), segmentIndex)
    }

    fun movedBy(translation: Vector2, updateTangents: Boolean = true): ContourEdge {
        return transformedBy(buildTransform {
            translate(translation)
        }, updateTangents)
    }

    fun rotatedBy(rotationInDegrees: Double, anchorT: Double, updateTangents: Boolean = true): ContourEdge {
        val anchor = contour.segments[segmentIndex].position(anchorT)
        return transformedBy(buildTransform {
            translate(anchor)
            rotate(rotationInDegrees)
            translate(-anchor)
        }, updateTangents)
    }

    fun scaledBy(scaleFactor: Double, anchorT: Double, updateTangents: Boolean = true): ContourEdge {
        val anchor = contour.segments[segmentIndex].position(anchorT)
        return transformedBy(buildTransform {
            translate(anchor)
            scale(scaleFactor)
            translate(-anchor)
        }, updateTangents)
    }
}

