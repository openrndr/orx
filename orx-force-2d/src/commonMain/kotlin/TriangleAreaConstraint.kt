package org.openrndr.extra.force2d

import org.openrndr.math.Vector2

class TriangleAreaConstraint(val body: Body,
                             var compliance: Double = 0.0, var iterations: Int = 1) : Constraint {

    var lambdas = DoubleArray(body.triangles.size)
    var restAreas = DoubleArray(body.triangles.size) {
        body.triangles[it].signedArea(body.nodes)
    }

    override suspend fun initialize() {

    }
    override suspend fun solve(body: Body, dt: Double) {
        require(body === this.body)
        val nodes = body.nodes
        val triangles = body.triangles

        lambdas.fill(0.0)
        val alpha = compliance / (dt * dt)
        for (iter in 0 until iterations) {
            for (i in body.triangles.indices) {
                val triangle = triangles[i]
                val id0 = triangle.a
                val id1 = triangle.b
                val id2 = triangle.c

                val p0 = nodes[id0].position
                val p1 = nodes[id1].position
                val p2 = nodes[id2].position

                val g0 = Vector2(0.5 * (p1.y - p2.y), 0.5 * (p2.x - p1.x))
                val g1 = Vector2(0.5 * (p2.y - p0.y), 0.5 * (p0.x - p2.x))
                val g2 = Vector2(0.5 * (p0.y - p1.y), 0.5 * (p1.x - p0.x))

                val w0 = nodes[id0].inverseMass
                val w1 = nodes[id1].inverseMass
                val w2 = nodes[id2].inverseMass
                val w = w0 + w1 + w2
                if (w == 0.0) {
                    continue
                }

                val wSum = g0.squaredLength * w0 + g1.squaredLength * w1 + g2.squaredLength * w2

                if (wSum < 1E-6) {
                    continue
                }
                val C = triangle.signedArea(nodes) - restAreas[i]
                val dLambda = (-C - alpha * lambdas[i]) / (wSum + alpha)
                lambdas[i] += dLambda
                nodes[id0].position += g0 * dLambda * w0
                nodes[id1].position += g1 * dLambda * w1
                nodes[id2].position += g2 * dLambda * w2
            }
        }
    }
}

fun Body.triangleAreaConstraint(configure : TriangleAreaConstraint.() -> Unit = {}) {
    constraints.add(TriangleAreaConstraint(this).apply(configure))
}
