package org.openrndr.extra.force2d
import org.openrndr.math.Vector2

/**
 * Represents a constant gravitational force applied to a physical body in a simulation.
 *
 * The `GravityForce` class is a specific implementation of the [Force] interface.
 * It applies a constant acceleration, defined by the `gravity` vector, to all nodes
 * of the body over time. The force is applied during every simulation frame,
 * updating the velocity of the nodes based on the gravitational vector and the elapsed time step.
 *
 * @property gravity the gravitational acceleration vector applied to the body.
 * Default value is [Vector2.ZERO], indicating no gravity unless explicitly configured.
 *
 * The `initializeFrame` method is a placeholder in this implementation and does not perform any initialization logic.
 *
 * The `apply` method applies the gravitational force by calculating the change in velocity
 * for each node in the body. It updates the velocity of each node over the given time step, `dt`.
 */
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

/**
 * Adds a gravitational force to the body and allows configuring its properties.
 *
 * This method attaches a new instance of [GravityForce] to the body, applying a constant
 * gravitational acceleration defined in the configuration block. The force affects all
 * nodes in the body during simulation steps.
 *
 * @param configure a lambda to configure the properties of the [GravityForce] instance,
 * such as setting the gravitational acceleration vector.
 */
fun Body.gravity(configure: GravityForce.() -> Unit) = forces.add(GravityForce().apply(configure))

/**
 * Applies a gravitational force to the body by adding it to the list of forces acting on the body.
 *
 * @param gravity the gravitational force to be applied, represented as a [GravityForce] instance.
 */
fun Body.gravity(gravity: GravityForce) = forces.add(gravity)