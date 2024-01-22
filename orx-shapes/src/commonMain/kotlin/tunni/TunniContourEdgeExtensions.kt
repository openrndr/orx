package org.openrndr.extra.shapes.tunni

import org.openrndr.extra.shapes.adjust.ContourAdjusterEdge
import org.openrndr.extra.shapes.adjust.ContourEdge
import org.openrndr.math.Vector2
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Segment
import org.openrndr.shape.ShapeContour


/**
 * The Tunni-point for this [ContourEdge]
 * @see Segment.tunniPoint
 */
val ContourEdge.tunniPoint: Vector2
    get() = contour.segments[segmentIndex].tunniPoint


/**
 * The Tunni-line for this [ContourEdge]
 * @see Segment.tunniLine
 */
val ContourEdge.tunniLine: LineSegment
    get() = contour.segments[segmentIndex].tunniLine


val ContourAdjusterEdge.tunniPoint get() = contourAdjuster.contour.segments[segmentIndex()].tunniPoint

val ContourAdjusterEdge.tunniLine get() = contourAdjuster.contour.segments[segmentIndex()].tunniLine



fun ContourEdge.withTunniPoint(tunniPoint: Vector2): ContourEdge {
    if (contour.empty) {
        return withoutAdjustments()
    } else {
        val segment = contour.segments[segmentIndex].withTunniPoint(tunniPoint)
        val newSegments = contour.segments.map { it }.toMutableList()
        newSegments[segmentIndex] = segment
        return ContourEdge(ShapeContour.fromSegments(newSegments, contour.closed), segmentIndex)
    }
}
fun ContourEdge.withTunniLine(pointOnLine: Vector2): ContourEdge {
    if (contour.empty) {
        return withoutAdjustments()
    } else {
        val segment = contour.segments[segmentIndex].withTunniLine(pointOnLine)
        val newSegments = contour.segments.map { it }.toMutableList()
        newSegments[segmentIndex] = segment
        return ContourEdge(ShapeContour.fromSegments(newSegments, contour.closed), segmentIndex)
    }
}

/**
 * @see Segment.withTunniPoint
 */
fun ContourAdjusterEdge.withTunniPoint(tunniPoint: Vector2) = wrap { withTunniPoint(tunniPoint) }

/**
 * @see Segment.withTunniLine
 */
fun ContourAdjusterEdge.withTunniLine(pointOnLine: Vector2) = wrap { withTunniLine(pointOnLine) }