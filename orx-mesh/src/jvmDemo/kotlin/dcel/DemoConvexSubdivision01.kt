package dcel

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.color.spaces.OKHSV
import org.openrndr.extra.color.tools.shiftHue
import org.openrndr.extra.mesh.dcel.convert.faceToShape
import org.openrndr.extra.mesh.dcel.convert.shapeToDcelNoTriangulation
import org.openrndr.extra.mesh.dcel.modify.convexFaceSetSubdivide
import org.openrndr.extra.shapes.primitives.regularPolygon

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {


            val shape = regularPolygon(7, drawer.bounds.center, 200.0).shape
            val dcel = shapeToDcelNoTriangulation(shape, 0.5)


            dcel.convexFaceSetSubdivide(setOf(0))

            dcel.convexFaceSetSubdivide(setOf(0))

            dcel.convexFaceSetSubdivide(setOf(1))
            dcel.convexFaceSetSubdivide(setOf(9))

            val shapes = (0 until dcel.faces.size).map { dcel.faceToShape(it) }





            extend {
                for ((index, shape) in shapes.withIndex()) {
                    drawer.fill = ColorRGBa.PINK.shiftHue<OKHSV>(index * 45.0)
                    drawer.stroke = ColorRGBa.BLACK
                    drawer.shape(shape)
                }

            }
        }
    }
}