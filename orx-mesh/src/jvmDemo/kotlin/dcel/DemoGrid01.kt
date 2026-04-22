package dcel

import org.openrndr.application
import org.openrndr.draw.isolated
import org.openrndr.extra.mesh.dcel.convert.faceToShape
import org.openrndr.extra.mesh.dcel.convert.shapeToDcelNoTriangulation
import org.openrndr.extra.mesh.dcel.convert.toDcel
import org.openrndr.extra.mesh.dcel.modify.edgeInsert
import org.openrndr.extra.mesh.dcel.modify.edgeRemove
import org.openrndr.extra.mesh.dcel.query.edgeLoopIndices
import org.openrndr.extra.mesh.generate.gridMesh
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.shapes.primitives.grid
import org.openrndr.shape.Circle
import org.openrndr.shape.Shape

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {

            val grid = gridMesh(drawer.bounds, 5, 10)
            val dcel = grid.toDcel()

            dcel.edgeRemove(dcel.halfEdges[30])
            dcel.edgeRemove(dcel.halfEdges[33])
            dcel.edgeRemove(dcel.halfEdges[73])


            for (r in 0 until 4) {
                for (i in 0 until 40) {
                    val f = dcel.faces.random()
                    if (f.edge != -1) {
                        val es = dcel.edgeLoopIndices(f.edge)
                        val i = Int.uniform(0, es.size)
                        dcel.edgeInsert(es[(i + 0).mod(es.size)], es[(i + 2).mod(es.size)])
                    }

                }

                for (i in 0 until 40) {
                    dcel.edgeRemove(dcel.halfEdges.random())
                }
            }
            val shapes = (0 until dcel.faces.size).map { dcel.faceToShape(it) }




            extend {
                drawer.shapes(shapes)
            }
        }
    }
}