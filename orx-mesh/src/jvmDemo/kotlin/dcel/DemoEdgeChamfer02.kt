package dcel

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.mesh.dcel.convert.faceToShape
import org.openrndr.extra.mesh.dcel.convert.toDcel
import org.openrndr.extra.mesh.dcel.modify.edgeSetChamfer
import org.openrndr.extra.mesh.generate.gridMesh
import org.openrndr.shape.Rectangle

fun main() {
    application {
        program {
            val grid = gridMesh(Rectangle(0.0, 0.0, 100.0, 100.0), 2, 1)
            val dcel = grid.toDcel()

            // Find the shared edge between face 0 and face 1
            val sharedEdgeIdx = dcel.halfEdges.indexOfFirst { e ->
                e.face == 0 && e.otherEdge != -1 && dcel.halfEdges[e.otherEdge].face == 1
            }

            val initialFaceCount = dcel.faces.size
            val newFaceIds = dcel.edgeSetChamfer(setOf(sharedEdgeIdx), 10.0)


            // The new face should be a quad (4 edges)
            val newFaceId = newFaceIds.first()
            val edges = dcel.halfEdges.filter { it.face == newFaceId }

            extend {
                drawer.clear(ColorRGBa.PINK)
                    for (i in dcel.faces.indices) {
                        drawer.shape(dcel.faceToShape(i))
                    }

            }
        }
    }
}