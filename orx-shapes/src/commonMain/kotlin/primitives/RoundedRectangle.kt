package org.openrndr.extra.shapes.primitives

import org.openrndr.draw.Drawer
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour
import kotlin.jvm.JvmRecord
import kotlin.math.min

/**
 * Rounded rectangle. When `radii` are given (instead of `radius`), they are specified clockwise
 * starting at the top-left. Fewer than 4 values are accepted and repeated cyclically.
 * (e.g., providing 2 values alternates them, providing 1 value applies it to all corners).
 */
@JvmRecord
data class RoundedRectangle(val corner: Vector2, val width: Double, val height: Double, val radii: List<Double>) {

    init {
        require(radii.isNotEmpty()) { "RoundedRectangle can't be constructed with zero radii" }
    }

    constructor(x: Double, y: Double, width: Double, height: Double, radii: List<Double>) :
            this(Vector2(x, y), width, height, radii)

    constructor(rectangle: Rectangle, radii: List<Double>) :
            this(rectangle.corner, rectangle.width, rectangle.height, radii)

    constructor(corner: Vector2, width: Double, height: Double, radius: Double) :
            this(corner, width, height, listOf(radius))

    constructor(x: Double, y: Double, width: Double, height: Double, radius: Double) :
            this(Vector2(x, y), width, height, listOf(radius))

    constructor(rectangle: Rectangle, radius: Double) :
            this(rectangle.corner, rectangle.width, rectangle.height, listOf(radius))

    /** the center of the rounded rectangle */
    val center: Vector2
        get() = corner + Vector2(width / 2, height / 2)

    val x: Double get() = corner.x
    val y: Double get() = corner.y

    /** [ShapeContour] representation of the rounded rectangle */
    val contour
        get() = contour {
            // A higher radius than half the width/height makes it go weird
            val r = List(4) {
                min(min(radii[it % radii.size], width / 2), height / 2)
            }

            moveTo(x + r[0], y)
            lineTo(x + width - r[1], y)

            arcTo(r[1], r[1], 90.0, false, true, Vector2(x + width, y + r[1]))
            lineTo(x + width, y + height - r[2])

            arcTo(r[2], r[2], 90.0, false, true, Vector2(x + width - r[2], y + height))
            lineTo(x + r[3], y + height)

            arcTo(r[3], r[3], 90.0, false, true, Vector2(x, y + height - r[3]))
            lineTo(x, y + r[0])

            arcTo(r[0], r[0], 90.0, false, true, Vector2(x + r[0], y))
            close()        }

    val shape
        get() = contour.shape
}

fun Drawer.roundedRectangle(roundedRectangle: RoundedRectangle) =
    contour(roundedRectangle.contour)

// radii
fun Drawer.roundedRectangle(x: Double, y: Double, width: Double, height: Double, radii: List<Double>) =
    contour(RoundedRectangle(x, y, width, height, radii).contour)

fun Drawer.roundedRectangle(position: Vector2, width: Double, height: Double, radii: List<Double>) =
    contour(RoundedRectangle(position, width, height, radii).contour)

fun Rectangle.toRounded(radii: List<Double>) = RoundedRectangle(this, radii)

// radius
fun Drawer.roundedRectangle(x: Double, y: Double, width: Double, height: Double, radius: Double) =
    contour(RoundedRectangle(x, y, width, height, listOf(radius)).contour)

fun Drawer.roundedRectangle(position: Vector2, width: Double, height: Double, radius: Double) =
    contour(RoundedRectangle(position, width, height, listOf(radius)).contour)

fun Rectangle.toRounded(radius: Double) = RoundedRectangle(this, listOf(radius))