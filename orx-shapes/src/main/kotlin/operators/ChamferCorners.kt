package org.openrndr.extra.shapes.operators

import org.openrndr.math.Vector2
import org.openrndr.shape.*
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sqrt

private fun Segment.linearSub(l0: Double, l1: Double): Segment {
    return sub(l0 / length, l1 / length)
}

private fun Segment.linearPosition(l: Double): Vector2 {
    return position((l / length).coerceIn(0.0, 1.0))
}

private fun pickLength(leftLength: Double, rightLength: Double, s0: Segment, s1: Segment): Double {
    val p3 = s1.end
    val p2 = s0.end
    val p1 = s0.start

    val det = (p2.x - p1.x) * (p3.y - p1.y) - (p3.x - p1.x) * (p2.y - p1.y)

    return if (det < 0.0) {
        leftLength
    } else {
        rightLength
    }
}

/**
 * Chamfers corners between linear segments
 * @param length the length of the chamfer
 * @param angleThreshold the maximum (smallest) angle between between linear segments
 * @param chamfer the chamfer function to apply
 */
fun ShapeContour.chamferCorners(
        leftLength: Double,
        rightLength: Double = leftLength,
        clip: Boolean = true,
        angleThreshold: Double = 180.0,
        chamfer: ContourBuilder.(p1: Vector2, p2: Vector2, p3: Vector2) -> Unit
) = contour {
    val sourceSegments = if (closed) {
        (this@chamferCorners.segments + this@chamferCorners.segments.first())
    } else {
        this@chamferCorners.segments
    }

    // Prelude
    if ((this@chamferCorners).closed && sourceSegments[sourceSegments.size - 2].linear && sourceSegments.first().linear) {
        val length = pickLength(leftLength, rightLength, sourceSegments.last(), sourceSegments.first())
        if (clip || length <= sourceSegments[0].length / 2) {
            moveTo(sourceSegments[0].linearPosition(length))
        } else {
            moveTo(sourceSegments[0].position(0.0))
        }
    } else {
        moveTo(position(0.0))
    }

    for ((s0, s1) in sourceSegments.zipWithNext()) {
        if (s0.control.size == 1) {
            curveTo(s0.control[0], s0.end)
        } else if (s0.control.size == 2) {
            curveTo(s0.control[0], s0.control[1], s0.end)
        } else if (s0.linear) {
            val length = pickLength(leftLength, rightLength, s0, s1)
            if (s0.linear && s1.linear && (clip || (length <= s0.length / 2 && length <= s1.length / 2))) {
                val p0 = s0.linearPosition(s0.length - length)
                val p1 = s1.linearPosition(length)
                lineTo(p0)
                chamfer(p0, s0.end, p1)
            } else {
                lineTo(s0.end)
            }
        }
    }

    // Postlude
    if (closed) {
        close()
    } else {
        val last = sourceSegments.last()
        when {
            last.linear -> {
                if (clip || length <= last.length / 2) {
                    lineTo(last.linearPosition(length))
                } else {
                    lineTo(last.end)
                }
            }
            last.control.size == 1 -> {
                curveTo(last.control[0], last.end)
            }
            last.control.size == 2 -> {
                curveTo(last.control[0], last.control[1], last.end)
            }
        }
    }
}

fun ShapeContour.bevelCorners(length: Double, angleThreshold: Double = 180.0): ShapeContour =
        chamferCorners(length, length, angleThreshold = angleThreshold) { _, _, p3 ->
            lineTo(p3)
        }

fun ShapeContour.roundCorners(length: Double, angleThreshold: Double = 180.0): ShapeContour =
        chamferCorners(length, length, angleThreshold = angleThreshold) { _, p2, p3 ->
            curveTo(p2, p3)
        }

fun ShapeContour.arcCorners(leftLength: Double, rightLength: Double = leftLength,
                            leftScale: Double = 1.0, rightScale: Double = leftScale,
                            leftLargeArc : Boolean = false, rightLargeArc : Boolean = leftLargeArc,
                            angleThreshold: Double = 180.0): ShapeContour =
        chamferCorners(abs(leftLength), abs(rightLength), angleThreshold = angleThreshold) { p1, p2, p3 ->
            val dx = abs(p3.x - p2.x)
            val dy = abs(p3.y - p2.y)
            val radius = sqrt(dx * dx + dy * dy)
            val det = (p2.x - p1.x) * (p3.y - p1.y) - (p3.x - p1.x) * (p2.y - p1.y)
            val scale = if (det < 0.0) leftScale else rightScale
            val sweep = scale * sign(det)
            val largeArc = if (det < 0.0) leftLargeArc else rightLargeArc
            arcTo(radius * abs(scale) , radius * abs(scale), 90.0, largeArc, sweep > 0.0, p3)
        }