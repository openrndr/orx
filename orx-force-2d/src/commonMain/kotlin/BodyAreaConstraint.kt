package org.openrndr.extra.force2d

import org.openrndr.math.Vector2

class BodyAreaConstraint(val body: Body, var compliance: Double = 0.0, var iterations: Int = 1) : Constraint {

    var restArea: Double = area()

    private var lambda = 0.0
    override suspend fun initialize() {
    }
    fun area(): Double {
        val boundaryLinks = body.boundaryLinks
        val nodes = body.nodes
        val links = body.links
        var a = 0.0
        for (link in boundaryLinks) {
            val na = nodes[links[link].source]
            val nb = nodes[links[link].target]
            val pa = na.position
            val pb = nb.position
            a += pa.x * pb.y - pa.y * pb.x

        }
        return a / 2.0
    }

    override suspend fun solve(body: Body, dt: Double) {
        require(body === this.body)
        val boundaryNodes = body.boundaryNodes
        val nodes = body.nodes

        lambda = 0.0
        for (iter in 0 until iterations) {
            // here we assume that outerlinks is ordered
            val alpha = compliance / (dt * dt)

            val area = area()
            val C = area - restArea


            var weightedSum = 0.0
            val grads = mutableListOf<Vector2>()
            for (i in 0 until boundaryNodes.size) {
                val i0 = boundaryNodes[(i - 1).mod(boundaryNodes.size)]
                val i1 = boundaryNodes[i]
                val i2 = boundaryNodes[(i + 1).mod(boundaryNodes.size)]

                val p0 = nodes[i0].position
                val p2 = nodes[i2].position


                val edge = p2 - p0
                val grad = -Vector2(-edge.y, edge.x) * 0.5

                weightedSum += grad.dot(grad) * nodes[i1].inverseMass
                grads.add(grad)

            }
            val dLambda = (-C - alpha * lambda) / (weightedSum + alpha)
            lambda += dLambda

            for (i in boundaryNodes.indices) {
                val n = nodes[boundaryNodes[i]]
                n.position += grads[i] * dLambda * n.inverseMass
            }

        }
    }
}

fun Body.bodyAreaConstraint(configure : BodyAreaConstraint.() -> Unit = {}) {
    constraints.add(BodyAreaConstraint(this).apply(configure))
}