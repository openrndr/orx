package org.openrndr.extra.force2d

import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle

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

fun Body.rectangularBoundsConstraint(configure: RectangularBoundsConstraint.() -> Unit = {}) =
    constraints.add(RectangularBoundsConstraint(this).apply { configure() })
