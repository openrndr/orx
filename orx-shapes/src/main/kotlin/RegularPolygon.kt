package org.openrndr.extra.shapes

import org.openrndr.math.Vector2
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour
import kotlin.math.cos
import kotlin.math.sin

fun regularPolygon(sides: Int, center: Vector2 = Vector2.ZERO, radius: Double = 100.0, phase: Double = 0.0): ShapeContour {
    val c = contour {
        val phi = Math.toRadians(phase)
        for (i in 0 until sides) {
            val x = center.x + radius * cos(i.toDouble() / sides * Math.PI * 2 + phi)
            val y = center.y + radius * sin(i.toDouble() / sides * Math.PI * 2 + phi)

            moveOrLineTo(x, y)
        }
        close()
    }
    return c.reversed
}

fun regularPolygonRounded(sides: Int, roundFactor: Double = 0.5, center: Vector2 = Vector2.ZERO, radius: Double = 100.0, phase: Double = 0.0): ShapeContour {
    val c = contour {
        val phi = Math.toRadians(phase)
        for (i in 0 until sides) {
            val x0 = center.x + radius * cos(i.toDouble() / sides * Math.PI * 2 + phi)
            val y0 = center.y + radius * sin(i.toDouble() / sides * Math.PI * 2 + phi)

            val x1 = center.x + radius * cos((i + 1.0) / sides * Math.PI * 2 + phi)
            val y1 = center.y + radius * sin((i + 1.0) / sides * Math.PI * 2 + phi)

            val x2 = center.x + radius * cos((i + 2.0) / sides * Math.PI * 2 + phi)
            val y2 = center.y + radius * sin((i + 2.0) / sides * Math.PI * 2 + phi)

            val f = roundFactor / 2.0

            val dx10 = x1 - x0
            val dy10 = y1 - y0
            val dx21 = x2 - x1
            val dy21 = y2 - y1

            val x3 = x0 + dx10 * f
            val y3 = y0 + dy10 * f

            val x4 = x1 - dx10 * f
            val y4 = y1 - dy10 * f

            val x5 = x1 + dx21 * f
            val y5 = y1 + dy21 * f

            moveOrLineTo(x3, y3)
            lineTo(x4, y4)
            curveTo(x1, y1, x5, y5)
        }
        close()
    }
    return c.reversed
}

fun regularPolygonBeveled(sides: Int, bevelFactor: Double = 0.5, center: Vector2 = Vector2.ZERO, radius: Double = 100.0, phase: Double = 0.0): ShapeContour {
    val c = contour {
        val phi = Math.toRadians(phase)
        for (i in 0 until sides) {
            val x0 = center.x + radius * cos(i.toDouble() / sides * Math.PI * 2 + phi)
            val y0 = center.y + radius * sin(i.toDouble() / sides * Math.PI * 2 + phi)

            val x1 = center.x + radius * cos((i + 1.0) / sides * Math.PI * 2 + phi)
            val y1 = center.y + radius * sin((i + 1.0) / sides * Math.PI * 2 + phi)

            val x2 = center.x + radius * cos((i + 2.0) / sides * Math.PI * 2 + phi)
            val y2 = center.y + radius * sin((i + 2.0) / sides * Math.PI * 2 + phi)

            val f = bevelFactor / 2.0

            val dx10 = x1 - x0
            val dy10 = y1 - y0
            val dx21 = x2 - x1
            val dy21 = y2 - y1

            val x3 = x0 + dx10 * f
            val y3 = y0 + dy10 * f

            val x4 = x1 - dx10 * f
            val y4 = y1 - dy10 * f

            val x5 = x1 + dx21 * f
            val y5 = y1 + dy21 * f

            moveOrLineTo(x3, y3)
            lineTo(x4, y4)
            lineTo(x5, y5)
        }
        close()
    }
    return c.reversed
}
