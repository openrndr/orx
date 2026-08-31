package org.openrndr.extra.force2d

import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import kotlin.math.abs

/**
 * Represents a circular constraint applied to nodes of a physical body.
 *
 * The `NodeCircleConstraint` ensures that nodes of the body remain within or on the
 * boundary of a circle. It uses constraint-solving techniques to enforce this behavior
 * over a set number of iterations with adjustable compliance and strength parameters.
 *
 * @constructor Creates a new `NodeCircleConstraint` with a reference to a physical body.
 * @param body the [Body] whose nodes are subject to this circular constraint.
 *
 * @property iterations the number of iterations performed during constraint solving.
 * Higher iterations can increase accuracy at the cost of computation time.
 * @property compliance a parameter representing the flexibility of the constraint.
 * A higher value allows nodes to move more freely within the constraint boundary.
 * @property strength a multiplier controlling the intensity of the constraint application.
 * Values greater than 1 make the constraint effect stronger, while values less than 1 make it weaker.
 * @property circle a function that determines the circle for a given node. The function outputs a
 * [Circle] object that specifies the center and radius of the constraint for that node.
 * @property lambdas an array used to store lagrange multipliers for constraint force calculations
 * for each node in the body.
 */
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

/**
 * Applies a circular constraint to the nodes of this [Body].
 *
 * The `NodeCircleConstraint` ensures that all nodes of the body remain within or on the boundary
 * of specified circles. The constraint is configured using the provided lambda, which allows users
 * to customize parameters such as strength, compliance, iterations, and the circle definition.
 *
 * @param configure a lambda used to configure the properties of the [NodeCircleConstraint].
 * Within this lambda, the constraint's behavior can be adjusted by setting parameters such as
 * iterations, compliance, strength, and circle definition based on the body's nodes.
 */
fun Body.nodeCircleConstraint(configure: NodeCircleConstraint.() -> Unit) {
    constraints.add(NodeCircleConstraint(this).apply(configure))
}