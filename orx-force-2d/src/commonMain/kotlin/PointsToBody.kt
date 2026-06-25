package org.openrndr.extra.force2d

import org.openrndr.math.Vector2

/**
 * Creates a physical body from a list of points, with nodes initialized based on the specified radius,
 * and applies a configuration to the resulting body.
 *
 * @param points a list of [Vector2] points representing the positions of the nodes in the body.
 * @param radius the radius to be assigned to each node in the body. Defaults to 1.0.
 * @param configure a lambda function to configure the properties of the [Body] instance.
 * @return a [Body] composed of nodes created from the provided points.
 */
fun pointsToBody(points : List<Vector2>, radius:Double = 1.0, configure: Body.() -> Unit): Body {
    val nodes = points.map { Node(it, it, Vector2.ZERO, 1.0, radius = radius) }
    return Body(nodes).apply(configure)
}