package org.openrndr.extra.force2d

import org.openrndr.math.Vector2

fun pointsToBody(points : List<Vector2>, radius:Double = 1.0, configure: Body.() -> Unit): Body {
    val nodes = points.map { Node(it, it, Vector2.ZERO, 1.0, radius = radius) }
    return Body(nodes).apply(configure)
}