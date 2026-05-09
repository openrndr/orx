package dcel

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.mesh.dcel.convert.faceToShape
import org.openrndr.extra.mesh.dcel.convert.shapeToDcelNoTriangulation
import org.openrndr.extra.mesh.dcel.modify.edgeSetOffset
import org.openrndr.extra.mesh.dcel.navigate.filterEdges
import org.openrndr.extra.mesh.dcel.navigate.isBoundary
import org.openrndr.extra.mesh.dcel.navigate.length
import org.openrndr.extra.mesh.dcel.query.edgesForFace
import org.openrndr.extra.mesh.dcel.query.edgesForFaces
import org.openrndr.extra.shapes.primitives.regularPolygon

fun main() {
    application {
        program {
            val shape = regularPolygon(6, drawer.bounds.center, 200.0).shape
            val dcel = shapeToDcelNoTriangulation(shape, 0.5)


            with(dcel) {
                var faces = edgeSetOffset(setOf(0,1,2,3,4,5), -20.0, true)

                var edges = edgesForFaces(faces).toList().filterEdges { it.isBoundary }.toSet()
                require(edges.size == 12)
                faces = edgeSetOffset(edges, -20.0, true)
                println(faces.size)

                edges = edgesForFaces(faces)
                    .toList().filterEdges { it.isBoundary }.toSet()
//                edgeSetOffset(edges, -20.0, true)
            }

            extend {
                drawer.clear(ColorRGBa.PINK)
                for (i in dcel.faces.indices) {
                    drawer.shape(dcel.faceToShape(i))
                }
            }
        }
    }
}