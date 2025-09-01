package org.openrndr.extra.shapes.primitives

import org.openrndr.shape.Rectangle

/**
 * Creates a new [Rectangle] by modifying its dimensions and position based on the provided parameters.
 * The method adjusts the position and size of the rectangle depending on which of the optional parameters
 * are supplied. Any omitted parameters are calculated to maintain the rectangle's overall layout.
 *
 * Some quick recipes:
 *  * Take a 40x50 rectangle from the center: `Rectangle(0.0, 0.0, 100.0, 100.0).take(width=40.0, height=50.0)`
 *
 *. * Take a 20x30 rectangle from the top left: `Rectangle(0.0, 0.0, 100.0, 100.0).take(left=0.0, top=0.0, width=20, height=30.0)`
 *
 *  * Take a 10x30 rectangle from the bottom right: `Rectangle(0.0, 0.0, 100.0, 100.0).take(bottom=0.0, right=0.0, width=20, height=30.0)`
 *
 * @param left The amount to shift the rectangle's left edge inward. If null, the left edge remains unchanged.
 * @param top The amount to shift the rectangle's top edge inward. If null, the top edge remains unchanged.
 * @param right The amount to shift the rectangle's right edge inward. If null, the right edge remains unchanged.
 * @param bottom The amount to shift the rectangle's bottom edge inward. If null, the bottom edge remains unchanged.
 * @param width The new width for the rectangle. If null, the width is adjusted based on left and right shifts.
 * @param height The new height for the rectangle. If null, the height is adjusted based on top and bottom shifts.
 * @return A new [Rectangle] instance with the updated dimensions and position.
 */
fun Rectangle.take(
    left: Double? = null,
    top: Double? = null,
    right: Double? = null,
    bottom: Double? =null,
    width: Double? = null,
    height: Double? = null
) :Rectangle {
    val newWidth = when {
        width != null -> width
        else -> this.width - (left ?: 0.0) - (right ?: 0.0)
    }

    val newHeight = when {
        height != null -> height
        else -> this.height - (top ?: 0.0) - (bottom ?: 0.0)
    }

    val newX = when {
        left != null -> corner.x + left
        right != null -> corner.x + this.width - right - newWidth
        else -> corner.x + (this.width - newWidth) / 2.0
    }

    val newY = when {
        top != null -> corner.y + top
        bottom != null -> corner.y + this.height - bottom - newHeight
        else -> corner.y + (this.height - newHeight) / 2.0
    }
    return Rectangle(newX, newY, newWidth, newHeight)
}