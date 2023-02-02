package org.openrndr.extra.marchingsquares

import org.openrndr.math.IntVector2
import org.openrndr.math.Vector2
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Rectangle
import kotlin.math.max
import kotlin.math.min

fun findContours(
    f: (Vector2) -> Double,
    area: Rectangle,
    cellSize: Double,
    useInterpolation: Boolean = true
): List<LineSegment> {
    val segments = mutableListOf<LineSegment>()
    val values = mutableMapOf<IntVector2, Double>()

    for (y in 0 until (area.width / cellSize).toInt()) {
        for (x in 0 until (area.width / cellSize).toInt()) {
            values[IntVector2(x, y)] = f(Vector2(x * cellSize + area.x, y * cellSize + area.y))
        }
    }

    val zero = 0.0
    for (y in 0 until (area.width / cellSize).toInt()) {
        for (x in 0 until (area.width / cellSize).toInt()) {
            val v00 = (values[IntVector2(x, y)] ?: zero)
            val v10 = (values[IntVector2(x + 1, y)] ?: zero)
            val v01 = (values[IntVector2(x, y + 1)] ?: zero)
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
                val l0 = LineSegment(p00.mix(p01, r0), p10.mix(p11, r1))
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
    return segments
}