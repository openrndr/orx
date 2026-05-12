package dcel

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.mesh.dcel.adjust.offset
import org.openrndr.extra.mesh.dcel.convert.faceToShape
import org.openrndr.extra.mesh.dcel.convert.shapeToDcelNoTriangulation
import org.openrndr.extra.mesh.dcel.modify.edgeSetOffset
import org.openrndr.extra.mesh.dcel.navigate.allEdges
import org.openrndr.extra.mesh.dcel.navigate.contains
import org.openrndr.extra.mesh.dcel.navigate.distinctEdges
import org.openrndr.extra.mesh.dcel.navigate.filter
import org.openrndr.extra.mesh.dcel.navigate.filterEdges
import org.openrndr.extra.mesh.dcel.navigate.isBoundary
import org.openrndr.extra.mesh.dcel.navigate.length
import org.openrndr.extra.mesh.dcel.navigate.toShape
import org.openrndr.extra.mesh.dcel.query.edgesForFace
import org.openrndr.extra.mesh.dcel.query.edgesForFaces
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.shapes.primitives.regularPolygon
import org.openrndr.math.Vector3

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val shape = regularPolygon(6, drawer.bounds.center, 10.0).shape
            val dcel = shapeToDcelNoTriangulation(shape, 0.5)

            with(dcel) {
                var faces =  allEdges().offset(-20.0, false)

                for (i in 0 until 4) {
                    var edges = faces.distinctEdges().filter { it.isBoundary }
//                    require(edges.size == 12)
                    faces = edges.offset(-Double.uniform(10.0, 40.0), i % 3 == 0)
                    println(faces.size)
                }

            }

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