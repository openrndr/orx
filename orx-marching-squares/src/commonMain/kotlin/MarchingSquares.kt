package org.openrndr.extra.marchingsquares

import org.openrndr.math.IntVector2
import org.openrndr.math.Vector2
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour
import kotlin.math.max
import kotlin.math.min


/**
 * Find contours for a function [f] using the marching squares algorithm. A contour is found when f(x) crosses zero.
 * @param f the function
 * @param area a rectangular area in which the function should be evaluated
 * @param cellSize the size of the cells, smaller size gives higher resolution
 * @param useInterpolation intersection points will be interpolated if true, default true
 * @return a list of [ShapeContour] instances
 */
fun findContours(
    f: (Vector2) -> Double,
    area: Rectangle,
    cellSize: Double,
    useInterpolation: Boolean = true
): List<ShapeContour> {
    val segments = mutableListOf<LineSegment>()
    val values = mutableMapOf<IntVector2, Double>()
    val segmentsMap = mutableMapOf<Vector2, MutableList<LineSegment>>()

    for (y in 0 until (area.height / cellSize).toInt()) {
        for (x in 0 until (area.width / cellSize).toInt()) {
            values[IntVector2(x, y)] = f(Vector2(x * cellSize + area.x, y * cellSize + area.y))
        }
    }

    val zero = 0.0
    for (y in 0 until (area.height / cellSize).toInt()) {
        for (x in 0 until (area.width / cellSize).toInt()) {

            // Here we check if we are at a right or top border. This is to ensure we create closed contours
            // later on in the process.
            val v00 = if (x == 0 || y == 0) zero else (values[IntVector2(x, y)] ?: zero)
            val v10 = if (y == 0) zero else (values[IntVector2(x + 1, y)] ?: zero)
            val v01 = if (x == 0) zero else (values[IntVector2(x, y + 1)] ?: zero)
            val v11 = (values[IntVector2(x + 1, y + 1)] ?: zero)

            val p00 = Vector2(x.toDouble(), y.toDouble()) * cellSize + area.corner
            val p10 = Vector2((x + 1).toDouble(), y.toDouble()) * cellSize + area.corner
            val p01 = Vector2(x.toDouble(), (y + 1).toDouble()) * cellSize + area.corner
            val p11 = Vector2((x + 1).toDouble(), (y + 1).toDouble()) * cellSize + area.corner

            val index = (if (v00 >= 0.0) 1 else 0) +
                    (if (v10 >= 0.0) 2 else 0) +
                    (if (v01 >= 0.0) 4 else 0) +
                    (if (v11 >= 0.0) 8 else 0)

            fun blend(v1: Double, v2: Double): Double {
                if (useInterpolation) {
                    require(v1 == v1 && v2 == v2)
                    val f1 = min(v1, v2)
                    val f2 = max(v1, v2)
                    val v = (-f1) / (f2 - f1)

                    require(v == v)
                    require(v in 0.0..1.0)

                    return if (f1 == v1) {
                        v
                    } else {
                        1.0 - v
                    }
                } else {
                    return 0.5
                }
            }

            fun emitLine(
                p00: Vector2, p01: Vector2, v00: Double, v01: Double,
                p10: Vector2, p11: Vector2, v10: Double, v11: Double
            ) {
                val r0 = blend(v00, v01)
                val r1 = blend(v10, v11)

                val v0 = p00.mix(p01, r0)
                val v1 = p10.mix(p11, r1)
                val l0 = LineSegment(v0, v1)
                segmentsMap.getOrPut(v1) { mutableListOf() }.add(l0)
                segmentsMap.getOrPut(v0) { mutableListOf() }.add(l0)
                segments.add(l0)
            }

            when (index) {
                0, 15 -> {}
                1, 15 xor 1 -> {
                    emitLine(p00, p01, v00, v01, p00, p10, v00, v10)
                }

                2, 15 xor 2 -> {
                    emitLine(p00, p10, v00, v10, p10, p11, v10, v11)
                }

                3, 15 xor 3 -> {
                    emitLine(p00, p01, v00, v01, p10, p11, v10, v11)
                }

                4, 15 xor 4 -> {
                    emitLine(p00, p01, v00, v01, p01, p11, v01, v11)
                }

                5, 15 xor 5 -> {
                    emitLine(p00, p10, v00, v10, p01, p11, v01, v11)
                }

                6, 15 xor 6 -> {
                    emitLine(p00, p01, v00, v01, p00, p10, v00, v10)
                    emitLine(p01, p11, v01, v11, p10, p11, v10, v11)
                }

                7, 15 xor 7 -> {
                    emitLine(p01, p11, v01, v11, p10, p11, v10, v11)
                }
            }
        }
    }

    val processedSegments = mutableSetOf<LineSegment>()
    val contours = mutableListOf<ShapeContour>()
    for (segment in segments) {
        if (segment in processedSegments) {
            continue
        } else {
            val collected = mutableListOf<Vector2>()
            var current: LineSegment? = segment
            var closed = true
            var lastVertex = Vector2.INFINITY
            do {
                current!!
                if (lastVertex.squaredDistanceTo(current.start) > 1E-5) {
                    collected.add(current.start)
                }
                lastVertex = current.start
                processedSegments.add(current)
                if (segmentsMap[current.start]!!.size < 2) {
                    closed = false
                }
                val hold = current
                current = segmentsMap[current.start]?.firstOrNull { it !in processedSegments }
                if (current == null) {
                    current = segmentsMap[hold.end]?.firstOrNull { it !in processedSegments }
                }
            } while (current != segment && current != null)

            contours.add(ShapeContour.fromPoints(collected, closed = closed))
        }
    }
    return contours
}