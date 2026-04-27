package dcel

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.rgb
import org.openrndr.extra.math.graph.Edge
import org.openrndr.extra.math.graph.Graph
import org.openrndr.extra.math.graph.coloring.colorGraph
import org.openrndr.extra.mesh.dcel.convert.faceToShape
import org.openrndr.extra.mesh.dcel.convert.toDcel
import org.openrndr.extra.mesh.dcel.modify.edgeInsert
import org.openrndr.extra.mesh.dcel.modify.edgeRemove
import org.openrndr.extra.mesh.dcel.query.edgeLoopIndices
import org.openrndr.extra.mesh.dcel.query.edgeObjectsForFace
import org.openrndr.extra.mesh.generate.gridMesh
import org.openrndr.extra.noise.uniform
import kotlin.random.Random

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {

            val grid = gridMesh(drawer.bounds, 20, 20)
            val dcel = grid.toDcel()

            for (r in 0 until 8) {
                for (i in 0 until 40) {
                    val f = dcel.faces.random()
                    if (f.edge != -1) {
                        val es = dcel.edgeLoopIndices(f.edge)
                        val i = Int.uniform(0, es.size)
                        dcel.edgeInsert(es[(i + 0).mod(es.size)], es[(i + 2).mod(es.size)])
                        //yield()
                    }

                }
                for (i in 0 until 40) {
                    dcel.edgeRemove(dcel.halfEdges.random())

                }
            }

            val g = Graph()
            for (i in 0 until dcel.faces.size) {
                if (dcel.faces[i].edge == -1) continue
                val edges = dcel.edgeObjectsForFace(i)
                for (edge in edges) {
                    if (edge.otherEdge == -1) continue
                    val otherFace = dcel.halfEdges[edge.otherEdge].face
                    if (otherFace != -1) {
                        g.edges.add(Edge(i, otherFace))
                    }
                }
            }
            val colors = colorGraph(g, 4)

            val shapes = (0 until dcel.faces.size).map { dcel.faceToShape(it) }


            extend {

                val palette = listOf(rgb("D0DC71"), rgb("8D47B3"), rgb("7BBFC7"), rgb("3E30A2"), rgb("545454"), rgb("68A942")).shuffled(
                    Random(seconds.toInt()))

                val colorCount = colors.distinct().size
                for ((index, shape) in shapes.withIndex()) {
                    drawer.fill = palette[colors[index]]
                    //drawer.fill = ColorRGBa.CRIMSON.shiftHue<OKHSV>(colors.getOrElse(index, { 0 } ) * (360.0 / colorCount))
                    drawer.stroke = ColorRGBa.BLACK
                    drawer.shape(shape)
                }

            }
        }
    }
}