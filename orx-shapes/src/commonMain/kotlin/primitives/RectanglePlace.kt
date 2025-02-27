package org.openrndr.extra.shapes.primitives

import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle

/**
 * Places a given rectangle (`item`) within the bounds of the current rectangle (`this`),
 * positioning it based on the specified anchor point.
 *
 * @param item The rectangle to be placed within the current rectangle.
 * @param anchor The relative position of the anchor point within the bounds of the current rectangle.
 *               Defaults to `(0.5, 0.5)` which centers the item within the current rectangle.
 * @return A new rectangle representing the positioned `item` within the current rectangle.
 */
fun Rectangle.place(item: Rectangle, anchor: Vector2 = Vector2(0.5, 0.5), itemAnchor: Vector2 = anchor): Rectangle {
    return Rectangle(
        x = x + (width * anchor.x - item.width * itemAnchor.x),
        y = y + (height * anchor.y - item.height * itemAnchor.y),
        width = item.width,
        height = item.height
    )
}

/**
 * Positions the current rectangle (`this`) within the given `container` rectangle.
 * The placement is determined by aligning the `itemAnchor` of the current rectangle to
 * the `anchor` point within the container rectangle.
 *
 * @param container The rectangle within which the current rectangle will be positioned.
 * @param anchor The relative position of the reference point within the `container` rectangle.
 *               By default, it is set to `(0.5, 0.5)`, which represents the center.
 * @param itemAnchor The relative position of the anchor point within the current rectangle.
 *                   Defaults to the value of `anchor`.
 * @return A new rectangle representing the current rectangle positioned within the container.
 */
fun Rectangle.placeIn(container: Rectangle, anchor: Vector2 = Vector2(0.5, 0.5), itemAnchor: Vector2 = anchor): Rectangle {
    return container.place(this, anchor, itemAnchor)
}