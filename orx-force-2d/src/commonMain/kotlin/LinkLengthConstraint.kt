package org.openrndr.extra.force2d

class LinkLengthConstraint( val body: Body, var compliance: Double = 0.0, var iterations: Int = 1) :
    Constraint {

    private var lambdas: DoubleArray = DoubleArray(body.links.size)
    var restLengths: DoubleArray = DoubleArray(body.links.size) {
        body.nodes[body.links[it].source].position.distanceTo(body.nodes[body.links[it].target].position)
    }

    override suspend fun initialize() {

    }

    override suspend fun solve(body: Body, dt: Double) {
        require(body === this.body)


        lambdas.fill(0.0)
        val alpha = compliance / (dt * dt)

        for (iter in 0 until iterations) {

            for (i in body.links.indices) {
                val link = body.links[i]
                val id0 = link.source
                val id1 = link.target
                val w0 = body.nodes[id0].inverseMass
                val w1 = body.nodes[id1].inverseMass
                val w = w0 + w1
                if (w == 0.0) {
                    continue
                }
                var grad = (body.nodes[id0].position - body.nodes[id1].position)
                val len = grad.length
                if (len == 0.0) {
                    continue
                }
                grad /= len
                val restLen = restLengths[i]
                val C = len - restLen
                val dLambda = (-C - alpha * lambdas[i]) / (w + alpha)
                lambdas[i] += dLambda
                body.nodes[id0].position += grad * dLambda * w0
                body.nodes[id1].position += grad * -dLambda * w1
            }
        }
    }
}

fun Body.linkLengthConstraint(configure : LinkLengthConstraint.() -> Unit = {}) {
    constraints.add(LinkLengthConstraint(this).apply(configure))
}