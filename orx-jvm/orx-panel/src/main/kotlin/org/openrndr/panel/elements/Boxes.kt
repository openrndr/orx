package org.openrndr.panel.elements

import org.openrndr.draw.Drawer
import org.openrndr.math.Vector2
import org.openrndr.panel.style.LinearDimension
import org.openrndr.panel.style.borderRadius
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour
import kotlin.math.min

/**
 * The radius of the corners of [this] in pixels, of its [org.openrndr.panel.style.borderRadius]: in pixels, or a
 * percentage of its shorter side; at most half of it
 */
val Element.cornerRadius: Double
    get() {
        val shorter = min(layout.screenWidth, layout.screenHeight)
        val radius = when (val r = computedStyle.borderRadius) {
            is LinearDimension.PX -> r.value
            is LinearDimension.Percent -> r.value / 100.0 * shorter
            else -> 0.0
        }
        return radius.coerceIn(0.0, shorter / 2.0)
    }

/** the outline of [rectangle] with corners rounded by [radius] */
fun roundedRectangle(rectangle: Rectangle, radius: Double): ShapeContour {
    val r = radius.coerceIn(0.0, min(rectangle.width, rectangle.height) / 2.0)
    val (x, y) = rectangle.corner
    val w = rectangle.width
    val h = rectangle.height
    return contour {
        moveTo(x + r, y)
        lineTo(x + w - r, y)
        arcTo(r, r, 0.0, largeArcFlag = false, sweepFlag = true, end = Vector2(x + w, y + r))
        lineTo(x + w, y + h - r)
        arcTo(r, r, 0.0, largeArcFlag = false, sweepFlag = true, end = Vector2(x + w - r, y + h))
        lineTo(x + r, y + h)
        arcTo(r, r, 0.0, largeArcFlag = false, sweepFlag = true, end = Vector2(x, y + h - r))
        lineTo(x, y + r)
        arcTo(r, r, 0.0, largeArcFlag = false, sweepFlag = true, end = Vector2(x + r, y))
        close()
    }
}

/** draws [rectangle] with the fill and stroke of the drawer, with corners rounded by [radius] */
fun Drawer.box(rectangle: Rectangle, radius: Double = 0.0) {
    if (radius <= 0.0) rectangle(rectangle) else contour(roundedRectangle(rectangle, radius))
}
