package dcel

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.mesh.dcel.adjust.subdivide
import org.openrndr.extra.mesh.dcel.convert.shapeToDcelNoTriangulation
import org.openrndr.extra.mesh.dcel.convert.toDcel
import org.openrndr.extra.mesh.dcel.modify.faceSetSplit
import org.openrndr.extra.mesh.dcel.navigate.allFaces
import org.openrndr.extra.mesh.dcel.navigate.contains
import org.openrndr.extra.mesh.dcel.navigate.toShape
import org.openrndr.extra.mesh.dcel.query.edgesForFace
import org.openrndr.extra.mesh.generate.gridMesh
import org.openrndr.extra.shapes.primitives.Plane
import org.openrndr.math.Polar
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
            val inner = drawer.bounds.offsetEdges(-300.0).contour.contour.reversed
            val shapeWithHole = Shape(listOf(outer, inner))

            val dcel = shapeToDcelNoTriangulation(shapeWithHole, 1.0)



            val steps = 3
            for (i in 0 until steps) {
                val p0 = drawer.bounds.center
                val p1 = p0 + Polar(i * (180.0/steps), 20.0).cartesian
                val p = Plane.fromPoints(p0, p1)
                dcel.faceSetSplit(dcel.allFaces().toSet(), p)
            }


//            with(dcel) {
//                dcel.allFaces().subdivide()
//            }


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
                for (i in dcel.vertices) {
                    drawer.circle(i.position.xy, 4.0)
                }
            }

        }
    }
}