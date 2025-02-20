package org.openrndr.extra.shapes.primitives

import org.openrndr.math.map
import org.openrndr.shape.Box

/**
 * Splits the current Box into two smaller Boxes at the specified x-coordinate if the coordinate
 * resides within the box's x-axis range.
 *
 * @param x The x-coordinate at which to split the box.
 * @return A list of Boxes. If the x-coordinate is within the range, it returns two boxes resulting
 *         from the split. Otherwise, it returns a single-element list containing the current box.
 */
fun Box.splitAtX(x: Double): List<Box> {
    return if (x in xRange) {
        val u = x.map(this.corner.x, this.corner.x + width, 0.0, 1.0)
        listOf(sub(0.0, 0.0, 0.0, u, 1.0, 1.0), sub(u, 0.0, 0.0, 1.0, 1.0, 1.0))
    } else {
        listOf(this)
    }
}

/**
 * Splits the current Box object into two separate boxes along the given Y-coordinate.
 * If the Y-coordinate falls within the vertical range of the box, the method produces
 * two new boxes divided at the specified Y-coordinate. If the Y-coordinate is outside
 * the vertical range, the method returns the original box unchanged.
 *
 * @param y The Y-coordinate at which the box should be split.
 * @return A list of Box objects, containing either two new boxes split at the specified
 * Y-coordinate, or the original box if the Y-coordinate is outside the vertical range.
 */
fun Box.splitAtY(y: Double): List<Box> {
    return if (y in yRange) {
        val v = y.map(this.corner.y, this.corner.y + height, 0.0, 1.0)
        listOf(sub(0.0, 0.0, 0.0,  1.0, v, 1.0), sub(0.0, v, 0.0,  1.0, 1.0, 1.0))
    } else {
        listOf(this)
    }
}

/**
 * Splits the current Box into two smaller boxes along the z-axis at the specified position.
 * If the provided z position lies outside the z-range of the current Box, the method returns
 * a list containing only the original Box.
 *
 * @param z The z-coordinate at which the Box should be split.
 * @return A list of two Boxes resulting from the split operation if the z-coordinate is within range,
 * or a singleton list containing the original Box if the split cannot be performed.
 */
fun Box.splitAtZ(z: Double): List<Box> {
    return if (z in zRange) {
        val w = z.map(this.corner.z, this.corner.z + depth, 0.0, 1.0)
        listOf(sub(0.0, 0.0, 0.0,  1.0, 1.0, w), sub(0.0, 0.0, w, 1.0, 1.0, 1.0))
    } else {
        listOf(this)
    }
}

/**
 * Subtracts the given Box from the current Box and returns the remaining parts
 * of the current Box that do not intersect with the given Box.
 *
 * The method splits the current Box along the edges of the other Box in all three dimensions (x, y, z)
 * to effectively remove the intersecting portion. Non-intersecting parts are returned as a list of Boxes.
 * If there is no intersection, the method returns the current Box as a single-element list.
 *
 * @param other The Box to subtract from the current Box.
 * @return A list of Boxes representing the remaining parts of the current Box after subtraction.
 */
fun Box.subtract(other: Box): List<Box> {
    return if (this.intersects(other)) {
        var items = listOf(this)
        items = items.flatMap { it.splitAtX(other.corner.x) }
        items = items.flatMap { it.splitAtY(other.corner.y) }
        items = items.flatMap { it.splitAtZ(other.corner.z) }
        items = items.flatMap { it.splitAtX(other.corner.x + other.width) }
        items = items.flatMap { it.splitAtY(other.corner.y + other.height) }
        items = items.flatMap { it.splitAtZ(other.corner.z + other.depth) }
        items = items.filter { it.volume > 0 && !it.intersects(other.offsetSides(-1E-5)) }
        items
    } else {
        listOf(this)
    }
}

/**
 * Subtracts a given Box from each Box in the list and returns a list of the resulting Boxes.
 *
 * This method applies the subtraction operation for a given Box (`other`) to every Box in the current list.
 * The subtraction operation removes the overlapping region between the current Box and `other`, and returns
 * the non-intersecting parts as a list of Boxes.
 *
 * @param other The Box to subtract from each Box in the list.
 * @return A new list of Boxes where every Box represents the remaining parts after subtracting `other`.
 */
fun List<Box>.subtract(other : Box) : List<Box> {
    return this.flatMap { it.subtract(other) }
}