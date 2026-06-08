package dcel

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.mesh.dcel.convert.toDcel
import org.openrndr.extra.mesh.dcel.navigate.allEdges
import org.openrndr.extra.mesh.dcel.query.verticesForEdge
import org.openrndr.extra.mesh.generate.gridMesh
import org.openrndr.extra.mesh.rtree.RtreeDcelEdge2D

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val grid = gridMesh(drawer.bounds, 10, 10)
            val dcel = grid.toDcel()
            val rtree = RtreeDcelEdge2D(dcel)
            dcel.allEdges().forEach {
                rtree.insert(it)
            }
            extend {
                drawer.clear(ColorRGBa.PINK)
                dcel.allEdges().map {
                    val vertices = dcel.verticesForEdge(it)
                    drawer.lineSegment(dcel.vertices[vertices[0]].position, dcel.vertices[vertices[1]].position)
                }
                drawer.stroke = ColorRGBa.WHITE
                drawer.strokeWeight = 2.0
                val edges = rtree.findKNearest(mouse.position, 8)
                require(edges.size == 8)
                edges.forEach {
                    val vertices = dcel.verticesForEdge(it)
                    drawer.lineSegment(dcel.vertices[vertices[0]].position, dcel.vertices[vertices[1]].position)
                }

            }

        }
    }
}