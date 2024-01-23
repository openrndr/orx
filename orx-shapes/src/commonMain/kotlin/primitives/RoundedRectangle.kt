package org.openrndr.extra.shapes.primitives

import org.openrndr.draw.Drawer
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.contour
import org.openrndr.shape.ShapeContour
import kotlin.math.min

class RoundedRectangle(val corner: Vector2, val width: Double, val height: Double, val radius: Double) {
    constructor(x: Double, y: Double, width: Double, height: Double, radius: Double) : this(
        Vector2(x, y),
        width,
        height,
        radius
    )

    constructor(rectangle: Rectangle, radius: Double) : this(
        rectangle.corner,
        rectangle.width,
        rectangle.height,
        radius
    )

    /** the center of the rounded rectangle */
    val center: Vector2
        get() = corner + Vector2(width / 2, height / 2)

    val x: Double get() = corner.x
    val y: Double get() = corner.y

    /** [ShapeContour] representation of the rounded rectangle */
    val contour
        get() = contour {
            // A higher radius than half the width/height makes it go weird
            val r = min(min(radius, width / 2), height / 2)

            moveTo(x + r, y)
            lineTo(x + width - r, y)

            arcTo(r, r, 90.0, false, true, Vector2(x + width, y + r))
            lineTo(x + width, y + height - r)

            arcTo(r, r, 90.0, false, true, Vector2(x + width - r, y + height))
            lineTo(x + r, y + height)

            arcTo(r, r, 90.0, false, true, Vector2(x, y + height - r))
            lineTo(x, y + r)

            arcTo(r, r, 90.0, false, true, Vector2(x + r, y))
            close()
        }

    val shape
        get() = contour.shape
}

fun Drawer.roundedRectangle(x: Double, y: Double, width: Double, height: Double, radius: Double) =
    contour(RoundedRectangle(x, y, width, height, radius).contour)

fun Drawer.roundedRectangle(position: Vector2, width: Double, height: Double, radius: Double) =
    contour(RoundedRectangle(position, width, height, radius).contour)

fun Drawer.roundedRectangle(roundedRectangle: RoundedRectangle) =
    contour(roundedRectangle.contour)

fun Rectangle.toRounded(radius: Double) = RoundedRectangle(this, radius)