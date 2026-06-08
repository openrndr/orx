package org.openrndr.extra.mesh.rtree

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.convert.faceToPolygon2D
import org.openrndr.extra.mesh.dcel.query.verticesForEdge
import org.openrndr.extra.mesh.dcel.query.verticesForFace
import org.openrndr.extra.rtree.RTree
import org.openrndr.extra.rtree.squaredDistanceToSegment
import org.openrndr.extra.shapes.polygon.signedDistance
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.bounds
import kotlin.math.abs

enum class DcelPrimitive {
    VERTEX, EDGE, FACE
}

class RtreeDcel(val dcel: Dcel, minEntries: Int = 2, maxEntries: Int = 4) {

    typealias Value = Pair<DcelPrimitive, Int>
    private fun Value.bounds(): Rectangle {
        return when (this.first) {
            DcelPrimitive.VERTEX -> {
                val v = dcel.vertices[this.second]
                Rectangle.fromCenter(v.position.xy, 1E-3)
            }
            DcelPrimitive.EDGE -> {
                val vertices = dcel.verticesForEdge(this.second)
                vertices.map { dcel.vertices[it].position.xy }.bounds
            }

            DcelPrimitive.FACE -> {
                val vertices = dcel.verticesForFace(this.second)
                vertices.map { dcel.vertices[it].position.xy }.bounds
            }
        }
    }

    private val rtree = RTree<Value>(minEntries, maxEntries) { it.bounds() }

    fun insert(primitive: DcelPrimitive, primitiveId: Int) = rtree.insert(primitive to primitiveId)
    fun delete(primitive: DcelPrimitive, primitiveId: Int) = rtree.delete(primitive to primitiveId)
    fun findInRange(area: Rectangle): List<Value> = rtree.findInRange(area)

    fun findKNearest(query: Vector2, k: Int): List<Value> {
        return rtree.findKNearest(query, k) { value, q ->

            when(value.first) {
                DcelPrimitive.VERTEX -> {
                    val v = dcel.vertices[value.second].position.xy
                    q.distanceTo(v)
                }
                DcelPrimitive.EDGE -> {
                    val vertices = dcel.verticesForEdge(value.second)
                    val p1 = dcel.vertices[vertices[0]].position.xy
                    val p2 = dcel.vertices[vertices[1]].position.xy
                    squaredDistanceToSegment(q, p1, p2)
                }

                DcelPrimitive.FACE -> {
                    dcel.faceToPolygon2D(value.second).signedDistance(q)
                }
            }
        }
    }
}
