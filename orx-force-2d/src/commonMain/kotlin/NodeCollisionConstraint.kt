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

/**
 * Represents a constraint for resolving collisions between nodes within a physical body.
 *
 * A [NodeCollisionConstraint] ensures that nodes within the [Body] do not overlap by applying
 * positional corrections based on their physical properties such as position, radius, and inverse mass.
 * This constraint is solved over successive time steps in a simulation.
 *
 * @constructor Initializes the constraint with the provided [Body], which represents the collection
 * of physical nodes to which the constraint will be applied.
 *
 * @property compliance A parameter controlling the flexibility of the constraint. A higher compliance
 * value allows more deviation from the strict enforcement of the constraint, while a lower value enforces
 * stricter adherence. It influences the degree of positional corrections during collision resolution.
 */
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

/**
 * Adds a node collision constraint to the body, configuring it using the provided lambda.
 *
 * The `nodeCollisionConstraint` method attaches a [NodeCollisionConstraint] to the body.
 * This constraint resolves collisions between nodes of the body by enforcing positional corrections
 * based on their physical properties, such as position, radius, and inverse mass.
 * The constraint ensures that nodes do not overlap during the simulation.
 *
 * @param configure a lambda to configure the properties of the [NodeCollisionConstraint] instance,
 * such as setting parameters like `compliance` to control the flexibility of the collision resolution.
 */
fun Body.nodeCollisionConstraint(configure: (NodeCollisionConstraint.() -> Unit)) =
    constraints.add(NodeCollisionConstraint(this).apply { configure() })