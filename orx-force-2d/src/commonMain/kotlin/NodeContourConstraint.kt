package org.openrndr.extra.force2d

import org.openrndr.extra.bvh.sap.BoxedPointOrEdge
import org.openrndr.extra.bvh.sap.findNearest
import org.openrndr.shape.Rectangle

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

fun Body.nodeContourConstraint(configure: NodeContourConstraint.() -> Unit) {
    constraints.add(NodeContourConstraint(this).apply(configure))
}
