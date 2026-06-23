package org.openrndr.extra.force2d

class BoundaryNodeRelaxForce : Force {

    var strength = 1.0

    override suspend fun initializeFrame(body: Body) {

    }

    override suspend fun apply(body: Body, dt: Double) {
        for (i in body.boundaryNodes.indices) {
            val nl = body.nodes[body.boundaryNodes[(i - 1).mod(body.boundaryNodes.size)]]
            val nc = body.nodes[body.boundaryNodes[(i).mod(body.boundaryNodes.size)]]
            val nr = body.nodes[body.boundaryNodes[(i + 1).mod(body.boundaryNodes.size)]]

            val mean = (nl.position + nr.position) / 2.0
            nc.velocity += (mean - nc.position) * dt * strength
        }
    }
}

fun Body.boundaryNodeRelaxForce(configure: BoundaryNodeRelaxForce.() -> Unit) =
    forces.add(BoundaryNodeRelaxForce().apply(configure))