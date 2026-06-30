package org.openrndr.extra.force2d.quadtree

import org.openrndr.extra.force2d.Node
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3

internal class QuadTreeNode(
        var xmin: Double, var ymin: Double,
        var xmax: Double, var ymax: Double
    ) {
        var centerOfMass: Vector2 = Vector2.ZERO
        var totalStrength: Double = 0.0
        var children: Array<QuadTreeNode?>? = null
        var node: Node? = null

        fun add(forceNode: Node, strengthValue: Double = 1.0) {
            if (node == null && children == null) {
                node = forceNode
                totalStrength = strengthValue
                centerOfMass = forceNode.position
                return
            }

            if (children == null) {
                children = arrayOfNulls(4)
                val existingNode = node!!
                val existingStrength = totalStrength // This should be retrieved/passed if we want per-node strength
                // Re-add existing node to children
                node = null
                totalStrength = 0.0
                centerOfMass = Vector2.ZERO
                addToChildren(existingNode, existingStrength)
            }

            addToChildren(forceNode, strengthValue)
            updateCenterOfMass(forceNode, strengthValue)
        }

        private fun addToChildren(forceNode: Node, strengthValue: Double) {
            val xm = (xmin + xmax) / 2.0
            val ym = (ymin + ymax) / 2.0

            val i = if (forceNode.position.x >= xm) 1 else 0
            val j = if (forceNode.position.y >= ym) 1 else 0
            val idx = i + (j shl 1)

            if (children!![idx] == null) {
                val nxMin = if (i == 1) xm else xmin
                val nxMax = if (i == 1) xmax else xm
                val nyMin = if (j == 1) ym else ymin
                val nyMax = if (j == 1) ymax else ym
                children!![idx] = QuadTreeNode(nxMin, nyMin, nxMax, nyMax)
            }
            children!![idx]!!.add(forceNode, strengthValue)
        }

        private fun updateCenterOfMass(forceNode: Node, strengthValue: Double) {
            val newTotalStrength = totalStrength + strengthValue
            if (newTotalStrength != 0.0) {
                centerOfMass = (centerOfMass * totalStrength + forceNode.position * strengthValue) / newTotalStrength
            } else {
                // If total strength is 0, we just use the last position to avoid NaN, 
                // though Barnes-Hut with 0 strength nodes is unusual.
                centerOfMass = forceNode.position 
            }
            totalStrength = newTotalStrength
        }
    }
