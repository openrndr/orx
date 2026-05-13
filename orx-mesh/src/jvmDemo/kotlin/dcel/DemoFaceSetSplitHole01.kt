package dcel

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.mesh.dcel.convert.shapeToDcelNoTriangulation
import org.openrndr.extra.mesh.dcel.convert.toDcel
import org.openrndr.extra.mesh.dcel.modify.faceSetSplit
import org.openrndr.extra.mesh.dcel.navigate.allFaces
import org.openrndr.extra.mesh.dcel.navigate.contains
import org.openrndr.extra.mesh.dcel.navigate.toShape
import org.openrndr.extra.mesh.generate.gridMesh
import org.openrndr.extra.shapes.primitives.Plane
import org.openrndr.math.Vector3
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import kotlin.math.sqrt

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {

            val outer = drawer.bounds.offsetEdges(-100.0).contour
            val inner = drawer.bounds.offsetEdges(-200.0).contour.contour.reversed
            val shapeWithHole = Shape(listOf(outer, inner))

            val dcel = shapeToDcelNoTriangulation(shapeWithHole, 1.0)
            val faces2 = dcel.faceSetSplit(dcel.allFaces().toSet(), Plane(Vector3(1.0, 1.0, 0.0).normalized, width*0.5*sqrt(2.0)))
            val faces3 = dcel.faceSetSplit(dcel.allFaces().toSet(), Plane(Vector3(-1.0, 1.0, 0.0).normalized, -width*0.0*sqrt(2.0)))

            extend {
                drawer.clear(ColorRGBa.PINK)
                with(dcel) {
                    for (i in faces.indices) {
                        val face = faces[i]
                        if (face.contains(mouse.position.xy0)) {
                            drawer.fill = ColorRGBa.GREEN
                        } else {
                            drawer.fill = ColorRGBa.WHITE
                        }
                        drawer.shape(face.toShape())
                    }
                }
            }

        }
    }
}