package org.openrndr.extra.shapes.polygon

import org.openrndr.math.Vector2

fun ComplexPolygon2D.containsPoint(point: Vector2): Boolean {
    return outer.containsPoint(point) && holes.none() { it.containsPoint(point) }
}