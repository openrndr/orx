package org.openrndr.extra.force2d

fun findCollisions(nodes: List<Node>, solveCollision: (Int, Int) -> Unit) {
    for ((index, node) in nodes.withIndex()) {
        nodes[index].id = index
    }
    val sorted = nodes.sortedBy { it.position.x - it.radius }

    for (i in sorted.indices) {

        val body = sorted[i]
        for (j in i + 1 until sorted.size) {

            val body2 = sorted[j]

            if (body.position.x + body.radius < body2.position.x - body2.radius) {
                break
            }

            if (body.position.squaredDistanceTo(body2.position) < (body.radius + body2.radius) * (body.radius + body2.radius)) {
                solveCollision(body.id, body2.id)
            }
        }
    }
}

class NodeCollisionConstraint(val body: Body) : Constraint {

    var compliance = 0.0

    override suspend fun initialize() {
    }

    override suspend fun solve(body: Body, dt: Double) {
        val alpha = compliance / (dt * dt)
        findCollisions(body.nodes, { id0, id1 ->
            val C =
                body.nodes[id0].position.distanceTo(body.nodes[id1].position) - body.nodes[id0].radius - body.nodes[id1].radius
            val w = body.nodes[id0].inverseMass + body.nodes[id1].inverseMass

            val w0 = body.nodes[id0].inverseMass
            val w1 = body.nodes[id1].inverseMass

            val normal = (body.nodes[id0].position - body.nodes[id1].position).normalized
            val dLambda = (-C - alpha * 0.0) / (w + alpha)

            body.nodes[id0].position += normal * dLambda * w0
            body.nodes[id1].position -= normal * dLambda * w1
        })
    }
}

fun Body.nodeCollisionConstraint(configure: (NodeCollisionConstraint.() -> Unit)) =
    constraints.add(NodeCollisionConstraint(this).apply { configure() })