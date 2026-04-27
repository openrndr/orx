package dcel

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.mesh.dcel.convert.faceToShape
import org.openrndr.extra.mesh.dcel.convert.shapeToDcelNoTriangulation
import org.openrndr.extra.mesh.dcel.modify.convexFaceVertexInsert

fun main() {
    application {
        program {
            val square = drawer.bounds.offsetEdges(-20.0).shape
            val dcel = shapeToDcelNoTriangulation(square, 0.5)
            dcel.convexFaceVertexInsert(0, drawer.bounds.center.xy0)
            extend {
                drawer.clear(ColorRGBa.PINK)
                drawer.fill = null
                for (i in dcel.faces.indices) {
                    drawer.shape(dcel.faceToShape(i))
                }
            }
        }
    }
}