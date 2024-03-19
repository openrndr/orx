package org.openrndr.extra.shapes.operators

import org.openrndr.shape.Segment2D
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour

fun ShapeContour.bulgeSegments(distortion: (index: Int, segment: Segment2D) -> Double): ShapeContour {
    val c = contour {
        moveTo(position(0.0))
        var index = 0
        for (segment in this@bulgeSegments.segments) {
            when {
                segment.linear -> {
                    val q = segment.quadratic

                    val d = distortion(index, segment)

                    curveTo(q.control[0] + segment.normal(0.5) * d, q.end)
                    index++
                }
                segment.control.size == 2 -> {
                    curveTo(segment.control[0], segment.control[1], segment.end)
                }
                segment.control.size == 1 -> {
                    curveTo(segment.control[0], segment.end)
                }
            }
        }
        if (this@bulgeSegments.closed) {
            close()
        }
    }
    return c
}

fun ShapeContour.bulgeSegments(distortion: Double) =
        bulgeSegments { _, _ -> distortion }

fun ShapeContour.bulgeSegments(distortion: List<Double>) =
        bulgeSegments { index, _ -> distortion[index.mod(distortion.size)] }