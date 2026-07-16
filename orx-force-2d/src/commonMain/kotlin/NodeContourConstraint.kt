package org.openrndr.extra.force2d

import org.openrndr.extra.bvh.sap.BoxedPointOrEdge
import org.openrndr.extra.bvh.sap.findNearest
import org.openrndr.shape.Rectangle

/**
 * Represents a constraint that enforces nodes within a body to remain near the defined contour of a rectangle.
 *
 * The [NodeContourConstraint] ensures that the nodes of a body stay within a certain distance of the contour
 * of a rectangle by iteratively applying corrections based on proximity calculations. It influences the
 * position of nodes to enforce defined physical rules in relation to the contour, while respecting mass
 * and other physical properties.
 *
 * @constructor Creates a [NodeContourConstraint] for the given physical body, which will affect its nodes.
 * @param body the [Body] whose nodes are constrained by this constraint.
 *
 * @property iterations the number of iterations performed while solving the constraint, influencing accuracy.
 * @property compliance the compliance factor that allows some flexibility in the constraint.
 * @property searchRadius the radius within which nodes will search for the nearest contour point or edge.
 * @property strength the strength of the constraint's enforcement on the body.
 * @property contour the contour of the rectangle which the nodes are constrained to respect.
 * @property lambdas an array of lambda values used internally for constraint resolution.
 */
class NodeContourConstraint(val body: Body) : Constraint {
    var iterations = 1
    var compliance = 0.0
    var searchRadius = 100.0
    var strength = 1.0

    var contour = Rectangle(0.0, 0.0, 100.0, 100.0).contour
    var lambdas = DoubleArray(body.nodes.size)
    override suspend fun initialize() {
    }

    override suspend fun solve(body: Body, dt: Double) {

        lambdas.fill(0.0)
        val alpha = compliance / (dt * dt)
        val nodeRectangles = body.nodes.mapIndexed { index, it ->
            BoxedPointOrEdge(it.bounds.offsetEdges(searchRadius), index, 0, index)
        }

        val segmentRectangles = contour.segments.mapIndexed { index, it ->
            BoxedPointOrEdge(it.bounds, index, 1, index)
        }

        //val pairs = sapFindIntersections(nodeRectangles, segmentRectangles)

        val (nearestEdge, distanceToEdge) = findNearest(nodeRectangles, segmentRectangles) { nodeIdx, edgeIdx ->
            val np = body.nodes[nodeIdx]
            val p = np.position
            val n = contour.segments[edgeIdx].nearest(p).position
            val d = n.distanceTo(p)
            d
        }

        for (i in body.nodes.indices) {
            if (nearestEdge[i] == -1) {
                continue
            }
            val p = body.nodes[i].position
            val nr = contour.segments[nearestEdge[i]].nearest(body.nodes[i].position)
            val q = nr.position

            if (q.distanceTo(p) < 1e-6)
                continue
            val n = (p - q).normalized
            val C = distanceToEdge[i]
            val w = body.nodes[i].inverseMass
            if (w == 0.0) {
                continue
            }
            val dLambda = (-C - alpha * lambdas[i]) / (w + alpha)
            lambdas[i] += dLambda

            body.nodes[i].position += n * dLambda * w * strength

        }

    }
}

/**
 * Applies a node contour constraint to the body and configures it using the provided configuration block.
 *
 * The node contour constraint ensures that the nodes of the body are constrained to remain near
 * the defined contour of a rectangle. This function allows you to customize the constraint by
 * providing a configuration block.
 *
 * @param configure a lambda function used to customize the [NodeContourConstraint] instance being added to the body.
 */
fun Body.nodeContourConstraint(configure: NodeContourConstraint.() -> Unit) {
    constraints.add(NodeContourConstraint(this).apply(configure))
}
