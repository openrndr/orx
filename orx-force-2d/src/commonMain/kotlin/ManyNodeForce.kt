package org.openrndr.extra.force2d

import org.openrndr.extra.force2d.quadtree.QuadTreeNode
import org.openrndr.math.Vector2
import org.openrndr.math.Vector2.Axis
import org.openrndr.shape.Rectangle
import kotlin.math.sqrt
import kotlin.random.Random

private fun Rectangle.outerSquare(): Rectangle {
    return if (this.majorAxis == Axis.X) {
        Rectangle(x, y, width, width)
    } else {
        Rectangle(x, y, height, height)
    }
}


class ManyNodeForce(val body: Body): Force {

    var theta = 1.0
    val random = Random(0)
    lateinit private var quadTree: QuadTreeNode

    var strength: (Node) -> Double = { -0.0 }

    private var strengths = DoubleArray(body.nodes.size)


    override suspend fun initializeFrame(body: Body) {
        body.updateBounds()
        val bounds = body.bounds.outerSquare()
        quadTree = QuadTreeNode(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height)
        for (i in body.nodes.indices) {
            strengths[i] = strength(body.nodes[i])
        }

        for ((index, node) in body.nodes.withIndex()) {
            quadTree.add(node, strengths[index])
        }

    }

    private fun applyForce(node: Node, delta: Vector2, distance: Double, distance2: Double, strength: Double, alpha: Double) {
        node.velocity += delta * (strength * alpha / (distance2 * distance))
    }
    private fun accumulateForce(node: Node, octNode: QuadTreeNode, theta2: Double, distanceMin2: Double, distanceMax2: Double, alpha: Double) {
        val delta = octNode.centerOfMass - node.position
        val d2 = delta.dot(delta)
        val w = octNode.xmax - octNode.xmin

        if (w * w / theta2 < d2) {
            if (d2 < distanceMax2) {
                if (d2 < distanceMin2) {
                    applyForce(node, delta, sqrt(d2), distanceMin2, octNode.totalStrength, alpha)
                } else {
                    applyForce(node, delta, sqrt(d2), d2, octNode.totalStrength, alpha)
                }
            }
        } else {
            if (octNode.children != null) {
                for (child in octNode.children!!) {
                    if (child != null) {
                        accumulateForce(node, child, theta2, distanceMin2, distanceMax2, alpha)
                    }
                }
            } else if (octNode.node != null && octNode.node != node) {
                if (d2 < distanceMax2) {
                    var dist2 = d2
                    var dist = sqrt(d2)
                    if (d2 < 0.00001) { // Jitter for coincident nodes
                        val rx = (random.nextDouble() - 0.5) * 0.01
                        val ry = (random.nextDouble() - 0.5) * 0.01
                        val jitteredDelta = Vector2(rx, ry)
                        dist2 = jitteredDelta.dot(jitteredDelta)
                        dist = sqrt(dist2)
                        applyForce(node, jitteredDelta, dist, maxOf(dist2, distanceMin2), octNode.totalStrength, alpha)
                    } else {
                        applyForce(node, delta, dist, maxOf(dist2, distanceMin2), octNode.totalStrength, alpha)
                    }
                }
            }
        }
    }

    override suspend fun apply(body: Body, dt: Double) {

        val theta2 = theta * theta
        for (i in body.nodes.indices) {
            val node = body.nodes[i]
            accumulateForce(node, quadTree, theta2, 0.0, 360.0*360.0, 1.0)
        }

    }
}

fun Body.manyNodeForce(configure: ManyNodeForce.() -> Unit) {
    forces.add(ManyNodeForce(this).apply(configure))
}