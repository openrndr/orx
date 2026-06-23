package org.openrndr.extra.force2d

import org.openrndr.extra.bvh.BVHNode2D
import org.openrndr.extra.bvh.findIntersectingPairs
import org.openrndr.math.smoothstep
import org.openrndr.shape.Rectangle


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