package org.openrndr.extra.force2d

import org.openrndr.extra.bvh.sap.BoxedPointOrEdge

/**
 * Processes a collection of points and edges in a bipartite structure using a sweep-line algorithm
 * to detect and handle intersections. The method categorizes objects into active points and edges,
 * iteratively updates the active lists, and invokes the intersection callback when overlaps are detected.
 *
 * @param points a list of `BoxedPointOrEdge` instances representing points in the coordinate space.
 * Objects with type `0` are considered points.
 * @param edges a list of `BoxedPointOrEdge` instances representing edges in the coordinate space.
 * Objects with type `1` are considered edges.
 * @param onIntersect a callback function invoked with the indices of intersecting objects
 * when a point and an edge intersect. The first parameter is the index of the intersecting point,
 * and the second parameter is the index of the intersecting edge.
 */

fun sortAndSweepBipartite(
    points: List<BoxedPointOrEdge>,
    edges: List<BoxedPointOrEdge>, onIntersect: (Int, Int) -> Unit
) {

    val tagged = (points + edges).sortedBy { it.bounds.x }

    val activePoints = mutableListOf<BoxedPointOrEdge>()
    val activeEdges = mutableListOf<BoxedPointOrEdge>()

    for (body in tagged) {
        activePoints.removeAll { (it.bounds.x + it.bounds.width) < body.bounds.x }
        activeEdges.removeAll { (it.bounds.x + it.bounds.width) < body.bounds.x }

        if (body.type == 0) {
            for (edge in activeEdges) {
                if (body.bounds.intersects(edge.bounds)) {
                    onIntersect(body.index, edge.index)
                }
            }
            activePoints.add(body)
        } else {
            for (point in activePoints) {
                if (point.bounds.intersects(body.bounds)) {
                    onIntersect(point.index, body.index)

                }
            }
            activeEdges.add(body)
        }
    }
}


class NodeRepulseInterbodyForce : InterbodyForce {

    var searchRadius = 10.0
    var strength = 1.0

    override suspend fun initializeFrame() {

    }

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

    override suspend fun apply(body: Body, other: Body, dt: Double, substep: Int) {
        val s0 = body.nodes.mapIndexed { i, node -> BoxedPointOrEdge(node.bounds.offsetEdges(searchRadius), i, 0, i) }
        val s1 = other.nodes.mapIndexed { i, node -> BoxedPointOrEdge(node.bounds.offsetEdges(searchRadius), i, 1, i) }

        sortAndSweepBipartite(s0, s1) { i, j ->
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
 * Adds a `NodeRepulseInterbodyForce` to the list of interbody forces in the simulation.
 *
 * The `NodeRepulseInterbodyForce` applies a repulsion force between nodes of bodies within
 * a configured search radius, encouraging them to remain apart. The strength and behavior
 * of the force can be customized via the provided configuration block.
 *
 * @param configure A lambda to configure the `NodeRepulseInterbodyForce` instance.
 * Use this block to customize properties such as `searchRadius` and `strength` for the force.
 */
fun ForceSimulation.nodeRepulseInterbodyForce(configure: NodeRepulseInterbodyForce.()->Unit) =
    interbodyForces.add(NodeRepulseInterbodyForce().apply(configure))