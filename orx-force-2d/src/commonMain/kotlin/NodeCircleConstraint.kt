package org.openrndr.extra.force2d

import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import kotlin.math.abs

class NodeCircleConstraint(val body: Body) : Constraint {
    var iterations = 1
    var compliance = 0.0
    var strength = 1.0

    var circle: (Node) -> Circle = { Circle(Vector2.ZERO, 200.0)}
    var lambdas = DoubleArray(body.nodes.size)
    override suspend fun initialize() {
    }

    override suspend fun solve(body: Body, dt: Double) {

        val alpha = compliance / (dt * dt)
        lambdas.fill(0.0)

        for (iter in 0 until iterations) {
            for ((index, node) in body.nodes.withIndex()) {
                val circle = circle(node)

                val delta = node.position - circle.center
                val length = delta.length

                if (length < 1E-12) {
                    continue
                }

                val w = node.inverseMass
                if (w == 0.0) {
                    continue
                }

                val C = length - circle.radius
                if (abs(C) < 1E-12) {
                    continue
                }

                val n = delta / length
                val dLambda = (-C - alpha * lambdas[index]) / (w + alpha)
                lambdas[index] += dLambda

                node.position += n * dLambda * w * strength
            }
        }
    }
}

fun Body.nodeCircleConstraint(configure: NodeCircleConstraint.() -> Unit) {
    constraints.add(NodeCircleConstraint(this).apply(configure))
}