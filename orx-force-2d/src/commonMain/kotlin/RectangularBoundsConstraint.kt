package org.openrndr.extra.force2d

import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle

/**
 * Enforces rectangular boundary constraints on a physical body within a simulation.
 *
 * The RectangularBoundsConstraint ensures that the nodes of a [Body] remain within
 * a specified rectangular boundary during simulation. It provides options to customize
 * the boundary dimensions, compliance factor, and the number of iterations for solving
 * constraint violations. Additionally, it supports a bounce factor that modifies
 * the behavior of the nodes when they collide with the boundary limits.
 *
 * @property body the [Body] whose nodes are constrained by the rectangular boundary.
 * @property bounds the rectangular region within which the nodes are constrained.
 * @property bounce the bounce factor to apply upon collision with the boundary, influencing
 *                  the restitution behavior of nodes.
 * @property compliance the compliance factor that softens or hardens the constraint by
 *                      impacting how strongly violations are resolved.
 * @property iterations the number of iterations to use for resolving constraint violations,
 *                      affecting the accuracy and stability of the constraint-solving process.
 */
class RectangularBoundsConstraint(val body: Body) : Constraint {

    var bounds = Rectangle(0.0, 0.0, 100.0, 100.0)
    var bounce = 0.0

    var compliance = 0.0
    var iterations = 1

    private var lambdas = DoubleArray(body.nodes.size)

    override suspend fun initialize() {
    }

    override suspend fun solve(body: Body, dt: Double) {
        val nodes = body.nodes

        val alpha = compliance / (dt * dt)

        val top = bounds.y + bounds.height
        val right = bounds.x + bounds.width
        val bottom = bounds.y
        val left = bounds.x

        lambdas.fill(0.0)
        for (iter in 0 until iterations) {
            for (i in nodes.indices) {

                val w = nodes[i].inverseMass

                if (nodes[i].position.y + nodes[i].radius > top) {
                    val C = top - nodes[i].position.y - nodes[i].radius
                    val dLambda = (-C - alpha * lambdas[i]) / (w + alpha)
                    lambdas[i] += dLambda
                    val normal = Vector2(0.0, -1.0)
                    nodes[i].position += normal * dLambda * w
                }
                if (nodes[i].position.y - nodes[i].radius < bottom) {
                    val C = nodes[i].position.y - bottom - nodes[i].radius
                    val dLambda = (-C - alpha * lambdas[i]) / (w + alpha)
                    lambdas[i] += dLambda
                    val normal = Vector2(0.0, 1.0)
                    nodes[i].position += normal * dLambda * w
                }

                if (nodes[i].position.x + nodes[i].radius > right) {
                    val C = right - nodes[i].position.x - nodes[i].radius
                    val dLambda = (-C - alpha * lambdas[i]) / (w + alpha)
                    lambdas[i] += dLambda
                    val normal = Vector2(-1.0, 0.0)
                    nodes[i].position += normal * dLambda * w

                }
                if (nodes[i].position.x - nodes[i].radius < left) {
                    val C = nodes[i].position.x - left - nodes[i].radius
                    val dLambda = (-C - alpha * lambdas[i]) / (w + alpha)
                    lambdas[i] += dLambda
                    val normal = Vector2(1.0, 0.0)
                    nodes[i].position += normal * dLambda * w
                }
            }
        }
    }
}

/**
 * Adds a rectangular boundary constraint to the body, ensuring that its nodes remain
 * confined within a specified rectangular region during the simulation.
 *
 * This method sets up and applies a [RectangularBoundsConstraint] to the body.
 * The constraint can be custom-configured using the passed lambda, allowing
 * modification of boundary properties such as dimensions, bounce factor, compliance,
 * and iterations.
 *
 * @param configure a lambda receiving a [RectangularBoundsConstraint] as `this`,
 *                  allowing customization of the constraint's properties.
 */
fun Body.rectangularBoundsConstraint(configure: RectangularBoundsConstraint.() -> Unit = {}) =
    constraints.add(RectangularBoundsConstraint(this).apply { configure() })
