package org.openrndr.extra.force2d
import org.openrndr.math.Vector2

class GravityForce : Force {
    var gravity = Vector2.ZERO

    override suspend fun initializeFrame(body: Body) {
    }

    override suspend fun apply(body: Body, dt: Double) {
        for (node in body.nodes) {
            node.velocity += gravity * dt
        }
    }
}

fun Body.gravity(configure: GravityForce.() -> Unit) = forces.add(GravityForce().apply(configure))

fun Body.gravity(gravity: GravityForce) = forces.add(gravity)