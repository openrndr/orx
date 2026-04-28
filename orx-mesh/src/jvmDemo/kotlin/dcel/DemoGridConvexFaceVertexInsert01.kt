package dcel

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.mesh.dcel.convert.faceToShape
import org.openrndr.extra.mesh.dcel.convert.toDcel
import org.openrndr.extra.mesh.dcel.modify.convexFaceVertexInsert
import org.openrndr.extra.mesh.dcel.modify.vertexChamfer
import org.openrndr.extra.mesh.dcel.query.convexFaceCenter
import org.openrndr.extra.mesh.generate.gridMesh
import org.openrndr.extra.shapes.primitives.grid

// 4v 4e 1f
// 6v 6e 1f
// 6v 7e 2f
// 8v 9e 2f
// 9v 10e 2f

// 9v 12e 4f
// 6v 7e 2f
// 8v 12e 6f
// 4v 5ee 2f

// e-v + 2 -h = f?
//


fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {

            val grid = drawer.bounds.offsetEdges(-25.0).grid(2,2, gutterX = 25.0, gutterY = 25.0)

            val dcels = grid.flatten().map {
                gridMesh(it, 3, 3).toDcel()
            }

            for ((index, dcel) in dcels.withIndex()) {
                when (index) {
                    0 -> dcel.convexFaceVertexInsert(0, dcel.convexFaceCenter(0))
                    1 -> dcel.convexFaceVertexInsert(1, dcel.convexFaceCenter(1))
                    2 -> dcel.convexFaceVertexInsert(4, dcel.convexFaceCenter(4))
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