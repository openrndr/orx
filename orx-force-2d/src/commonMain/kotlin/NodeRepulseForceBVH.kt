package org.openrndr.extra.force2d

import org.openrndr.extra.bvh.BVHNode2D
import org.openrndr.extra.bvh.findIntersectingPairs
import org.openrndr.math.smoothstep
import org.openrndr.shape.Rectangle

/**
 * Represents a repulsive force applied on nodes of a [Body] using a bounding volume hierarchy (BVH) for optimization.
 *
 * This force calculates mutual repulsion between nodes within a specified search radius to prevent overlap
 * or maintain specific spatial relationships. The force magnitude decreases smoothly as the distance between
 * nodes approaches the search radius, determined by a smoothstep function.
 *
 * @constructor Creates a [NodeRepulseForceBVH] for the specified [body].
 * @property body The [Body] instance on which the repulsive force acts. This serves as the context for the force calculations.
 * @property searchRadius The radius used to search for neighboring nodes to apply repulsion. Default is 10.0.
 * @property strength The strength of the repulsive force. Default is 1.0.
 * @property bvh A bounding volume hierarchy data structure used to efficiently detect pairs of intersecting nodes.
 *
 * Methods:
 * - [initializeFrame]: Initializes the BVH structure using the current state of the nodes in the [Body].
 * - [apply]: Applies the repulsive force between nodes based on their proximity, updating their velocities accordingly.
 */
class NodeRepulseForceBVH(val body: Body) : Force {
    var searchRadius = 10.0
    var strength = 1.0

    lateinit var bvh: BVHNode2D

    override suspend fun initializeFrame(body: Body) {
        bvh = BVHNode2D.fromObjects(body.nodes) {
            Rectangle.fromCenter(
                it.position,
                it.radius * 2.0 + searchRadius * 2.0,
                it.radius * 2.0 + searchRadius * 2.0
            )
        }
    }

    override suspend fun apply(body: Body, dt: Double) {
        require(this.body === body)
        val pairs = findIntersectingPairs(bvh)

        for ((id0, id1) in pairs) {
            val body0 = body.nodes[id0]
            val body1 = body.nodes[id1]
            val delta = body1.position - body0.position
            val l = delta.length
            val n = delta.normalized

            if (l < searchRadius) {
                val f = smoothstep(searchRadius, searchRadius * 0.9, l) * strength

                body.nodes[id0].velocity -= n * f * body0.inverseMass * dt
                body.nodes[id1].velocity += n * f * body1.inverseMass * dt
            }
        }
    }
}

fun Body.nodeRepulseForceBVH(configure: NodeRepulseForceBVH.() -> Unit) =
    forces.add(NodeRepulseForceBVH(this).apply(configure))