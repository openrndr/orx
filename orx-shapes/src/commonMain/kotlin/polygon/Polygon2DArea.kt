package org.openrndr.extra.shapes.polygon

import kotlin.math.abs

/**
 * Calculates the area of the polygon using the shoelace formula.
 *
 * The method computes the absolute value of the sum of the cross-products
 * of all adjacent edges of the polygon. It assumes the points are ordered
 * either clockwise or counterclockwise.
 *
 * @return the area enclosed by the polygon as a [Double].
 */
fun Polygon2D.area(): Double {
    var area = 0.0
    for (i in points.indices) {
        val p1 = points[i]
        val p2 = points[(i + 1) % points.size]
        area += p1.x * p2.y - p2.x * p1.y
    }
    return abs(area) / 2.0
}

/**
 * Calculates the total area of a complex polygon, including an outer polygon and subtracting its holes' areas.
 *
 * The area of the complex polygon is determined by calculating the area of the outer polygon and
 * subtracting the sum of the areas of all its inner hole polygons. Negative or zero areas from holes are handled
 * appropriately based on their individual area calculations.
 *
 * @return the net area of the complex polygon as a [Double].
 */
fun ComplexPolygon2D.area(): Double {
    return outer.area() - holes.sumOf { it.area() }
}