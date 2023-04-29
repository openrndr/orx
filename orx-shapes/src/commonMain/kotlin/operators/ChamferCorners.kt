package org.openrndr.extra.shapes.operators

import org.openrndr.math.Vector2
import org.openrndr.math.mod_
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
        lengths: (index: Int, left: Segment, right: Segment) -> Double,
        expands: (index: Int, left: Segment, right: Segment) -> Double = { _, _, _ -> 0.0 },
        clip: Boolean = true,
        angleThreshold: Double = 180.0,
        chamfer: ContourBuilder.(p1: Vector2, p2: Vector2, p3: Vector2) -> Unit
): ShapeContour {

    if (segments.size <= 1) {
        return this
    }

    return contour {
        val sourceSegments = if (closed) {
            (this@chamferCorners.segments + this@chamferCorners.segments.first())
        } else {
            this@chamferCorners.segments
        }

        var lengthIndex = sourceSegments.size - 1


        sourceSegments.first().let {
            if (it.control.size == 1) {
                moveTo(position(0.0))
            }
            if (it.control.size == 2) {
                moveTo(position(0.0))
            }
            if (it.linear) {
                if (!this@chamferCorners.closed)
                    moveTo(position(0.0))
            }
        }


        lengthIndex = 0
        for ((s0, s1) in sourceSegments.zipWithNext()) {
            lengthIndex++
            if (s0.control.size == 1) {
                moveOrCurveTo(s0.control[0], s0.end)
            } else if (s0.control.size == 2) {
                moveOrCurveTo(s0.control[0], s0.control[1], s0.end)
            } else if (s0.linear) {

                val length = lengths(lengthIndex, s0, s1)

                if (s0.linear && s1.linear && (clip || (length <= s0.length / 2 && length <= s1.length / 2))) {

                    val expand = expands(lengthIndex, s0, s1)

                    val p0 = s0.linearPosition(s0.length - length)
                    val p1 = s1.linearPosition(length)

                    val d = p1 - p0

                    val q0 = p0 - d * expand
                    val q1 = p1 + d * expand

                    moveOrLineTo(q0)
                    chamfer(q0, s0.end, q1)
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
                    lineTo(last.end)
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
}

fun ShapeContour.bevelCorners(length: Double, angleThreshold: Double = 180.0): ShapeContour =
        chamferCorners({ _, _, _ -> length }, angleThreshold = angleThreshold) { _, _, p3 ->
            lineTo(p3)
        }

fun ShapeContour.roundCorners(length: Double, angleThreshold: Double = 180.0): ShapeContour =
        chamferCorners({ _, _, _ -> length }, angleThreshold = angleThreshold) { _, p2, p3 ->
            curveTo(p2, p3)
        }

fun ShapeContour.arcCorners(lengths: List<Double>,
                            expands: List<Double> = listOf(0.0),
                            scales: List<Double> = listOf(1.0),
                            largeArcs: List<Boolean> = mutableListOf(false),
                            angleThreshold: Double = 180.0): ShapeContour {


    val scaleRing = scales.ring()
    val lengthRing = lengths.ring()
    val expandRing = expands.ring()
    val largeArcRing = largeArcs.ring()

    var segmentIndex = 0
    return chamferCorners({ index, _, _ -> lengthRing[index] },
            { index, _, _ -> expandRing[index] },
            angleThreshold = angleThreshold) { p1, p2, p3 ->

        val dx = abs(p3.x - p2.x)
        val dy = abs(p3.y - p2.y)
        val radius = sqrt(dx * dx + dy * dy)
        val det = (p2.x - p1.x) * (p3.y - p1.y) - (p3.x - p1.x) * (p2.y - p1.y)
        val scale = scaleRing[segmentIndex]
        val sweep = scale * sign(det)
        val largeArc = largeArcRing[segmentIndex]
        arcTo(radius * abs(scale), radius * abs(scale), 90.0, largeArc, sweep > 0.0, p3)
        segmentIndex++
    }
}

private class Ring<T>(private val x: List<T>) : List<T> by x {
    override operator fun get(index: Int): T {
        return x[index.mod(x.size)]
    }
}

private fun <T> List<T>.ring(): List<T> {
    return Ring(this)
}
