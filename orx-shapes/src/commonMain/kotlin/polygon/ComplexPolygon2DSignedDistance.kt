package org.openrndr.extra.shapes.polygon

import org.openrndr.math.Vector2

fun ComplexPolygon2D.signedDistance(point: Vector2): Double {

    val outerDistance = outer.distance(point)
    var minDistance = outerDistance

    for (hole in holes) {
        val holeDistance = hole.distance(point)
        if (holeDistance < minDistance) {
            minDistance = holeDistance
        }
    }
    if (containsPoint(point)) return -minDistance
    else return minDistance
}