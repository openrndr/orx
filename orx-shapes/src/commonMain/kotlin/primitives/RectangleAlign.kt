package org.openrndr.extra.shapes.primitives

import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.bounds

/**
 * Aligns a list of rectangles horizontally relative to a specified rectangle.
 *
 * Each rectangle in the list is repositioned so that its horizontal
 * alignment matches the specified anchor point of the target rectangle.
 *
 * @param to The target rectangle to align to.
 * @param anchor A value between 0.0 and 1.0 representing the horizontal position
 *               within the target rectangle. Default is 0.5 (center).
 * @return A new list of rectangles aligned horizontally relative to the target rectangle.
 */
fun List<Rectangle>.alignToHorizontally(to: Rectangle, anchor: Double = 0.5): List<Rectangle> {
    val tox = to.position(anchor, 0.0).x

    return this.map {
        Rectangle.fromAnchor(Vector2(anchor, 0.0), Vector2(tox, it.y), it.width, it.height)
    }
}

/**
 * Aligns the rectangles in the list vertically to a reference rectangle.
 * The vertical position of each rectangle is determined based on the reference rectangle
 * and the specified vertical anchor point.
 *
 * @param to The reference rectangle to which the list of rectangles is vertically aligned.
 * @param anchor A value between 0.0 and 1.0 representing the vertical anchor point.
 *               Defaults to 0.5, which aligns based on the center.
 * @return A new list of rectangles aligned vertically to the specified rectangle.
 */
fun List<Rectangle>.alignToVertically(to: Rectangle, anchor: Double = 0.5): List<Rectangle> {
    val toy = to.position(0.0, anchor).y

    return this.map {
        Rectangle.fromAnchor(Vector2(0.0, anchor), Vector2(it.x, toy), it.width, it.height)
    }
}

/**
 * Distributes the rectangles in the list horizontally within a specified bounding rectangle.
 *
 * Each rectangle is positioned at regular intervals, ensuring equal spacing between them.
 * The method maintains the height and y-coordinate of each rectangle, only adjusting their x-coordinates.
 *
 * @param within The bounding rectangle within which the rectangles are horizontally distributed.
 *               Defaults to the bounding rectangle covering all rectangles in the list.
 * @return A new list of rectangles with updated positions that are evenly distributed horizontally.
 */
fun List<Rectangle>.distributeHorizontally(within: Rectangle = bounds): List<Rectangle> {
    val usedWidth = sumOf { it.width }
    val unusedWidth = within.width - usedWidth
    val betweenWidth = unusedWidth / (size - 1)

    val distributed = mutableListOf<Rectangle>()

    var x = within.x
    for (i in indices) {
        distributed.add(Rectangle(x, this[i].y, this[i].width, this[i].height))
        x += this[i].width + betweenWidth
    }
    return distributed
}

/**
 * Distributes the rectangles in the list vertically within the given bounding rectangle.
 * The rectangles are spaced evenly, ensuring an equal distance between them, while
 * maintaining their original height and width.
 *
 * @param within The bounding rectangle within which the rectangles will be vertically
 * distributed. Defaults to the minimal bounding rectangle containing all rectangles in the list.
 * @return A new list of rectangles that are vertically distributed within the specified bounds.
 */
fun List<Rectangle>.distributeVertically(within: Rectangle = bounds): List<Rectangle> {
    val usedHeight = sumOf { it.height }
    val unusedHeight = within.height - usedHeight
    val betweenHeight = unusedHeight / (size - 1)

    val distributed = mutableListOf<Rectangle>()

    var y = within.y
    for (i in indices) {
        distributed.add(Rectangle(this[i].x, y, this[i].width, this[i].height))
        y += this[i].height + betweenHeight
    }
    return distributed
}

/**
 * Distributes a list of rectangles horizontally within a given container rectangle,
 * maintaining their relative width proportions and adding an optional gutter
 * between them.
 *
 * @param within The container rectangle within which the rectangles will be distributed.
 * The default value is the bounding box of the current list of rectangles.
 * @param gutter The space (in units) to be added between adjacent rectangles. Default is 0.0.
 * @return A new list of rectangles distributed horizontally within the container rectangle.
 */
fun List<Rectangle>.fitHorizontally(within: Rectangle = bounds, gutter: Double = 0.0): List<Rectangle> {
    val gutterlessWidth = within.width - gutter * (size - 1)

    val usedWidth = sumOf { it.width }
    val ratios = map { it.width / usedWidth }

    var x = within.x
    val distributed = mutableListOf<Rectangle>()
    for (i in indices) {
        val w = ratios[i] * gutterlessWidth
        distributed.add(Rectangle(x, this[i].y, w, this[i].height))
        x += w + gutter
    }
    return distributed
}

/**
 * Fits a list of rectangles within a given vertical rectangular area.
 * Each rectangle's height is adjusted proportionally based on its original height
 * relative to the total height of all rectangles in the list. The rectangles
 * are then distributed vertically, with an optional gutter spacing between them.
 *
 * @param within The bounding rectangle within which the list of rectangles should
 *               fit. If not provided, the bounds of the current list of rectangles
 *               will be used.
 * @param gutter The vertical spacing between the rectangles. Default value is 0.0.
 * @return A new list of rectangles that are proportionally resized and vertically
 *         distributed within the specified bounding rectangle.
 */
fun List<Rectangle>.fitVertically(within: Rectangle = bounds, gutter: Double = 0.0): List<Rectangle> {
    val gutterlessHeight = within.height - gutter * (size - 1)

    val usedHeight = sumOf { it.height }
    val ratios = map { it.height / usedHeight }

    var y = within.y
    val distributed = mutableListOf<Rectangle>()
    for (i in indices) {
        val h = ratios[i] * gutterlessHeight
        distributed.add(Rectangle(this[i].x, y, this[i].width, h))
        y += h + gutter
    }
    return distributed
}