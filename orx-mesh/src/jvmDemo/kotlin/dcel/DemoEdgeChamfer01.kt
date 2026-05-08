package dcel

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.mesh.dcel.convert.faceToShape
import org.openrndr.extra.mesh.dcel.convert.toDcel
import org.openrndr.extra.mesh.dcel.modify.convexFaceSetSubdivide
import org.openrndr.extra.mesh.dcel.modify.convexFaceVertexInsert
import org.openrndr.extra.mesh.dcel.modify.edgeSetChamfer
import org.openrndr.extra.mesh.dcel.modify.vertexChamfer
import org.openrndr.extra.mesh.dcel.query.convexFaceCenter
import org.openrndr.extra.mesh.generate.gridMesh
import org.openrndr.extra.shapes.primitives.grid


fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {

            val grid = drawer.bounds.offsetEdges(-25.0).grid(2,2, gutterX = 25.0, gutterY = 25.0)

            val dcels = grid.flatten().map {
                gridMesh(it, 3, 2).toDcel()
            }

            for ((index, dcel) in dcels.withIndex()) {
                when (index) {
                    0 -> dcel.edgeSetChamfer(setOf(0), 5.0)
                    1 -> dcel.edgeSetChamfer(setOf(5), 5.0)
                    2 -> dcel.edgeSetChamfer(setOf(10), 5.0)
                    3 -> dcel.edgeSetChamfer(setOf(20), 5.0)
                }

            }

            extend {
                drawer.clear(ColorRGBa.PINK)
                for (dcel in dcels) {
                    for (i in dcel.faces.indices) {
                        drawer.shape(dcel.faceToShape(i))
                    }
                }

            }
        }
    }
}