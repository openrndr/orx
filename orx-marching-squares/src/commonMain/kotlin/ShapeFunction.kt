package org.openrndr.extra.marchingsquares

import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle

/**
 * A scalar field. A contour is found where [ShapeFunction] crosses zero, with negative values
 * considered "inside" and positive (or zero) values considered "outside".
 */
typealias ShapeFunction = (Vector2) -> Double

/**
 * Wraps [shapeFunction] so that it reads as exactly `0.0` on or beyond the border of [area],
 * regardless of its real value there. [findContours] gets closed contours out of an area a
 * shape extends past by forcing its outermost sampled row/column of corners to `0.0` the
 * same way (there, it falls out of how the grid is sampled; here there is no grid to exploit,
 * so it's done directly) -- without this, a shape that isn't fully contained inside [area]
 * produces dangling, non-closed pieces instead of a contour that closes off along the area's
 * own edge.
 */
internal fun clampToBorder(shapeFunction: ShapeFunction, area: Rectangle): ShapeFunction {
    val xMax = area.x + area.width
    val yMax = area.y + area.height
    return { p ->
        if (p.x <= area.x || p.x >= xMax || p.y <= area.y || p.y >= yMax) 0.0 else shapeFunction(p)
    }
}

/** The (approximate, normalized) gradient of [shapeFunction] at [p], via central differences. */
internal fun normal(shapeFunction: ShapeFunction, p: Vector2): Vector2 {
    val epsilon = 0.001
    val dx = shapeFunction(Vector2(p.x + epsilon, p.y)) - shapeFunction(Vector2(p.x - epsilon, p.y))
    val dy = shapeFunction(Vector2(p.x, p.y + epsilon)) - shapeFunction(Vector2(p.x, p.y - epsilon))
    val d = Vector2(dx, dy)
    return if (d.squaredLength > 0.0) d.normalized else Vector2.ZERO
}


/**
 * Whether [shapeFunction] actually changes sign between [p0] and [p1] -- used to check that
 * two adjacent leaves' *shared edge* has a crossing on it, as opposed to each leaf merely
 * being ambiguous somewhere among its own four corners (which can be a *different* corner
 * than the one they share). Without this check, [edgeProcH]/[edgeProcV] would wire together
 * the dual vertices of two leaves that both happen to straddle a boundary, even when neither
 * boundary is the one they share -- which shatters a field with several separate regions
 * (e.g. concentric rings) into many spurious little polygons instead of a handful of correct
 * ones.
 */
internal fun hasSignChange(shapeFunction: ShapeFunction, p0: Vector2, p1: Vector2): Boolean =
    (shapeFunction(p0) < 0.0) != (shapeFunction(p1) < 0.0)


/** Finds the point on segment [a]-[b] where [shapeFunction] crosses zero, via binary search. */
private fun findZero(shapeFunction: ShapeFunction, a: Vector2, b: Vector2): Vector2 {
    if (shapeFunction(a) >= 0.0) {
        return findZero(shapeFunction, b, a)
    }
    var f = 0.5
    var step = 0.25
    var p = a.mix(b, f)
    repeat(10) {
        f = if (shapeFunction(p) < 0.0) f + step else f - step
        step /= 2.0
        p = a.mix(b, f)
    }
    return p
}

internal fun sidePoint(shapeFunction: ShapeFunction, cell: Rectangle, side: Side): Vector2 {
    val xMax = cell.x + cell.width
    val yMax = cell.y + cell.height
    return when (side) {
        Side.LEFT -> findZero(shapeFunction, Vector2(cell.x, cell.y), Vector2(cell.x, yMax))
        Side.RIGHT -> findZero(shapeFunction, Vector2(xMax, cell.y), Vector2(xMax, yMax))
        Side.LOWER -> findZero(shapeFunction, Vector2(cell.x, cell.y), Vector2(xMax, cell.y))
        Side.UPPER -> findZero(shapeFunction, Vector2(cell.x, yMax), Vector2(xMax, yMax))
    }
}
