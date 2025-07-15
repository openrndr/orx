package org.openrndr.extra.shapes.primitives

import org.openrndr.shape.Rectangle

/**
 * Adjusts the dimensions and position of the rectangle based on the provided parameters.
 * The method calculates the new dimensions and coordinates of the rectangle based on specified
 * values for left, right, top, bottom, width, or height. If conflicting parameters are provided
 * (e.g., both `left` and `right` or `top` and `bottom`), an error is thrown.
 *
 * @param left Optional offset to adjust the left side of the rectangle.
 *        If provided along with `right`, an error is thrown.
 * @param right Optional offset to adjust the right side of the rectangle.
 *        If provided along with `left`, an error is thrown.
 * @param top Optional offset to adjust the top side of the rectangle.
 *        If provided along with `bottom`, an error is thrown.
 * @param bottom Optional offset to adjust the bottom side of the rectangle.
 *        If provided along with `top`, an error is thrown.
 * @param width Optional value to override the width of the rectangle. Ignored if both
 *        `left` and `right` are provided.
 * @param height Optional value to override the height of the rectangle. Ignored if both
 *        `top` and `bottom` are provided.
 * @return A new [Rectangle] with the adjusted dimensions and position.
 */
fun Rectangle.adjacent(left: Double? = null, right: Double? = null, top: Double? = null, bottom: Double? = null,
                       width: Double? = null, height: Double? = null

) : Rectangle {
    val newWidth = when {
        left != null && right != null -> this.width + left + right
        else -> width?: this.width
    }
    val newHeight = when {
        top != null && bottom != null -> this.height + top + bottom
        else -> height?: this.height
    }

    val newX = when {
        left != null && right != null -> error("set either left or right, not both")
        left != null -> corner.x - newWidth - left
        right != null -> corner.x + this.width + right
        else -> corner.x
    }

    val newY = when {
        bottom != null && top != null -> error("set either top or bottom, not both")
        top != null -> corner.y - newHeight - top
        bottom != null -> corner.y + this.height + bottom
        else -> corner.y
    }

    return Rectangle(newX, newY, newWidth, newHeight)
}