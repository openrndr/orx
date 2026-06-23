package org.openrndr.extra.force2d

import org.openrndr.extra.bvh.sap.BoxedPointOrEdge
import org.openrndr.extra.bvh.sap.findNearest
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.bounds

class SAPCollisionConstraint : CollisionConstraint {

    override suspend fun solve(body: Body, other: Body, dt: Double) {

        val boundaryNodes = body.boundaryNodes
        val nodes = body.nodes

        fun signedDistanceToSegment(p: Vector2, a: Vector2, b: Vector2): Triple<Double, Vector2, Double> {
            val ab = b - a
            val ap = p - a
            val ogt = (ap.dot(ab) / ab.dot(ab))
            val t = ogt.coerceIn(0.0, 1.0)
            val closest = a + ab * t
            val toP = p - closest
            val normal = Vector2(-ab.y, ab.x).normalized
            return Triple(toP.dot(normal), normal, t)
        }

        fun pointInOther(point: Vector2): Boolean {
            var intersections = 0
            for (i in other.boundaryNodes.indices) {
                val p1 = other.nodes[other.boundaryNodes[i]].position
                val p2 = other.nodes[other.boundaryNodes[(i + 1).mod(other.boundaryNodes.size)]].position

                if (((p1.y <= point.y && point.y < p2.y) || (p2.y <= point.y && point.y < p1.y))) {
                    val intersectX = (p2.x - p1.x) * (point.y - p1.y) / (p2.y - p1.y) + p1.x
                    if (point.x < intersectX) {
                        intersections++
                    }
                }
            }
            return intersections % 2 != 0
        }

        fun circleInOther(point: Vector2, radius: Double): Boolean {
            var intersections = 0
            for (i in other.boundaryNodes.indices) {
                val p1 = other.nodes[other.boundaryNodes[i]].position
                val p2 = other.nodes[other.boundaryNodes[(i + 1).mod(other.boundaryNodes.size)]].position

                if (((p1.y <= point.y && point.y < p2.y) || (p2.y <= point.y && point.y < p1.y))) {
                    val intersectX = (p2.x - p1.x) * (point.y - p1.y) / (p2.y - p1.y) + p1.x
                    if (point.x - radius < intersectX) {
                        intersections++
                    }
                }
            }
            return intersections % 2 != 0
        }
        

        val points = if (boundaryNodes.isNotEmpty()) boundaryNodes.mapIndexed { index, it ->
            val p = nodes[it].position
            BoxedPointOrEdge(
                Rectangle.fromCenter(p, 2.0, 2.0), it, 0, index
            )
        } else {
            nodes.mapIndexed { index, it ->
                val p = nodes[index].position
                BoxedPointOrEdge(
                    Rectangle.fromCenter(p, 2.0, 2.0), index, 0, index
                )
            }
        }

        val edges = other.boundaryLinks.mapIndexed { index, it ->
            val e = other.links[it]
            val na = other.nodes[e.source]
            val nb = other.nodes[e.target]
            val r = listOf(na.position, nb.position).bounds.offsetEdges(2.0)
            BoxedPointOrEdge(r, it, 1, index)
        }

        val (nearestArg, nearestDistance) = findNearest(points, edges) { point, edge ->
            val p = nodes[point].position
            val e = other.links[edge]
            val a = other.nodes[e.target].position
            val b = other.nodes[e.source].position
            val (d, _, _) = signedDistanceToSegment(p, a, b)

            if (d < 0) -d else Double.POSITIVE_INFINITY
        }


        for (i in nearestArg.indices) {

            if (nearestArg[i] == -1)
                continue

            val ip = points[i].index
            val np = nodes[ip]
            val ie = nearestArg[i]
            val ne = other.links[ie]
            val na = other.nodes[ne.target]
            val nb = other.nodes[ne.source]

            val p = np.position
            val a = na.position
            val b = nb.position

            val (dist, normal, t) = signedDistanceToSegment(p, a, b)

            val skinWidth = 0.0
            val intersects2 = dist - skinWidth < 0.0 && pointInOther(p)

            if (intersects2) {
                val t = t.coerceIn(0.0, 1.0)
                val C = dist - skinWidth


                val wP = if (body.static) 0.0 else np.inverseMass
                val wV0 = if (other.static) 0.0 else na.inverseMass
                val wV1 = if (other.static) 0.0 else nb.inverseMass

                val w = wP + (1 - t) * (1 - t) * wV0 + t * t * wV1
                if (w < 1E-12)
                    continue

                val s = -C / (w + 0.0)

                np.position += normal * s * wP
                na.position += normal * -s * wV0 * (1 - t)
                nb.position += normal * -s * wV1 * t
            }
        }
    }
}

fun ForceSimulation.sapCollisionConstraint() {
    collisionConstraint = SAPCollisionConstraint()
}