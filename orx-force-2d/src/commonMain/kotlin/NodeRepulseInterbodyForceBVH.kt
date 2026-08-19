package org.openrndr.extra.force2d

import org.openrndr.extra.bvh.BVHNode2D
import org.openrndr.extra.bvh.findIntersectingPairs


/**
 * Implementation of the [InterbodyForce] interface that applies a repulsive force between the nodes
 * of two physical bodies based on their proximity, utilizing a bounding volume hierarchy (BVH)
 * for improved performance during pair detection.
 *
 * This class models interbody repulsion where nodes from two distinct bodies repel each other based
 * on a defined search radius and strength. The BVH data structure accelerates the process of detecting
 * intersecting nodes.
 *
 * @property searchRadius The maximum distance within which nodes of two bodies interact.
 *                        Nodes beyond this radius have no effect on each other.
 * @property strength The magnitude of the repulsive force applied between interacting nodes.
 * @property bvhs A map used to cache BVH representations for bodies during a simulation frame,
 *                improving performance by avoiding redundant BVH recomputations.
 */
class NodeRepulseInterbodyForceBVH : InterbodyForce {

    var searchRadius = 10.0
    var strength = 1.0

    override suspend fun findOverlappingPairs(bodies: List<Body>): List<Pair<Int, Int>> {
        val pairs = mutableListOf<Pair<Int, Int>>()
        for (i in 0 until bodies.size) {
            for (j in i + 1 until bodies.size) {

                if (bodies[i].bounds.intersects(bodies[j].bounds)) {
                    pairs.add(i to j)
                }
            }
        }
        return pairs
    }

    var bvhs = mutableMapOf<Body, BVHNode2D>()

    override suspend fun initializeFrame() {
        bvhs.clear()

    }

    override suspend fun apply(body: Body, other: Body, dt: Double, substep: Int) {
//        val s0 = body.nodes.mapIndexed { i, node -> BoxedPointOrEdge(node.bounds.offsetEdges(searchRadius), i, 0, i) }
//        val s1 = other.nodes.mapIndexed { i, node -> BoxedPointOrEdge(node.bounds.offsetEdges(searchRadius), i, 1, i) }


        val s0 = bvhs.getOrPut(body) {
            BVHNode2D.fromObjects(body.nodes) { it.bounds.offsetEdges(searchRadius) }
        }

        val s1 = bvhs.getOrPut(other) {
            BVHNode2D.fromObjects(other.nodes) { it.bounds.offsetEdges(searchRadius) }
        }

        val pairs = findIntersectingPairs(s0, s1)

        for ((i, j) in pairs) {
            val n0 = body.nodes[i]
            val n1 = other.nodes[j]
            val delta = n1.position - n0.position
            val d = delta.length - n0.radius - n1.radius
            if (d < searchRadius) {
                val f = strength
                val n = delta.normalized
                body.nodes[i].velocity -= n * f * n0.inverseMass * dt
                other.nodes[j].velocity += n * f * n1.inverseMass * dt
            }
        }

    }
}

/**
 * Adds a repulsive interbody force using a bounding volume hierarchy (BVH) to the simulation.
 *
 * This method configures and applies the [NodeRepulseInterbodyForceBVH] to simulate repulsion
 * between nodes across bodies in the [ForceSimulation]. The force uses a BVH structure
 * to optimize the detection of node pairs within the specified search radius.
 *
 * @param configure A lambda expression used to configure the properties of [NodeRepulseInterbodyForceBVH].
 *                  The configuration can include attributes such as `searchRadius` and `strength` to
 *                  define the nature of the repulsive force.
 */
fun ForceSimulation.nodeRepulseInterbodyForceBVH(configure: NodeRepulseInterbodyForceBVH.() -> Unit) =
    interbodyForces.add(NodeRepulseInterbodyForceBVH().apply(configure))