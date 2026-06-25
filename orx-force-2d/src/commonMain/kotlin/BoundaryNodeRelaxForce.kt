package org.openrndr.extra.force2d

/**
 * BoundaryNodeRelaxForce is a type of force that acts on the boundary nodes of a [Body].
 * It aims to relax the boundary nodes by adjusting their velocities based on neighboring node positions.
 * Each boundary node is influenced by the midpoint of its neighboring boundary nodes, providing a smoothing effect.
 *
 * @property strength Controls the intensity of the relaxation force. A higher strength results in greater adjustment of the node velocities.
 *
 * This force operates frame-by-frame:
 * 1. In each frame, the [apply] method computes the velocity correction for each boundary node.
 * 2. The correction is calculated by determining the difference between the node's current position and the midpoint of its neighboring nodes.
 * 3. The computed difference is scaled by the time step `dt` and the `strength` property, then added to the node's velocity.
 *
 * The force assumes a cyclic relationship between boundary nodes, where the first and last nodes are neighbors.
 */
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

/**
 * Adds a [BoundaryNodeRelaxForce] to the [Body]'s list of forces.
 * This force acts on the boundary nodes of the body to relax their positions
 * by adjusting their velocities based on neighboring node positions.
 *
 * @param configure A lambda configuration function for the [BoundaryNodeRelaxForce].
 *                  Use this to set properties such as `strength` to customize force behavior.
 */
fun Body.boundaryNodeRelaxForce(configure: BoundaryNodeRelaxForce.() -> Unit) =
    forces.add(BoundaryNodeRelaxForce().apply(configure))