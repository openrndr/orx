package org.openrndr.extra.force2d

import org.openrndr.math.smoothstep

private fun findNear(
    nodes: List<Node>,
    searchRadius: Double,
    index: MutableList<Int> = nodes.indices.toMutableList(),
    solveCollision: (Int, Int) -> Unit
) {

    index.sortBy { nodes[it].position.x - nodes[it].radius }

    for (i in index.indices) {
        val body = nodes[index[i]]
        for (j in i + 1 until index.size) {
            val body2 = nodes[index[j]]

            if (body.position.x + body.radius + searchRadius < body2.position.x - body2.radius - searchRadius) {
                break
            }

            val tr = searchRadius + body.radius + body2.radius
            if (body.position.squaredDistanceTo(body2.position) < tr * tr) {
                solveCollision(index[i], index[j])
            }
        }
    }
}

/**
 * Represents a repulsive force applied to nodes within a specified search radius.
 *
 * The `NodeRepulseForce` class implements the [Force] interface and applies a repelling force
 * between nodes in a [Body], based on their proximity to each other. Nodes within the
 * `searchRadius` experience a force proportional to their distance, aimed at pushing them apart.
 *
 * @property body The [Body] instance on which the force is applied.
 * @property searchRadius The radius within which nodes repel each other.
 * A larger value increases the distance at which nodes interact.
 * @property strength The magnitude of the repulsive force.
 * A higher value results in stronger repulsion between nodes.
 */
class NodeRepulseForce(val body: Body) : Force {

    private val index = body.nodes.indices.toMutableList()
    var searchRadius = 10.0
    var strength = 1.0

    override suspend fun initializeFrame(body: Body) {

    }

    override suspend fun apply(body: Body, dt: Double) {
        require(this.body === body)

        findNear(body.nodes, searchRadius, index) { id0, id1 ->
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

fun Body.nodeRepulseForce(configure: NodeRepulseForce.() -> Unit) = forces.add(NodeRepulseForce(this).apply(configure))