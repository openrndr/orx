package org.openrndr.extra.force2d

/**
 * A constraint that enforces the lengths of links between nodes within a [Body].
 *
 * The [LinkLengthConstraint] ensures that links connecting nodes in the [Body] adhere to their
 * defined rest lengths. It computes corrections over a number of iterations to resolve any
 * deviations in length while considering compliance, which introduces flexibility into the system.
 *
 * @property body the [Body] that this constraint operates on.
 * @property compliance a parameter controlling the flexibility of the links. Higher compliance
 * allows greater deviation from the rest lengths.
 * @property iterations the number of iterations to refine the resolution of the constraint for
 * each time step.
 *
 * This constraint works by computing the deviation of each link's current length from its rest
 * length and applying corrective forces iteratively to the connected nodes. It takes into
 * account the mass of the nodes and compliance to distribute corrections appropriately.
 */
class LinkLengthConstraint(val body: Body, var compliance: Double = 0.0, var iterations: Int = 1) :
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

fun Body.linkLengthConstraint(configure: LinkLengthConstraint.() -> Unit = {}) {
    constraints.add(LinkLengthConstraint(this).apply(configure))
}