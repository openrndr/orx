package dcel

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.mesh.dcel.Face
import org.openrndr.extra.mesh.dcel.adjust.offset
import org.openrndr.extra.mesh.dcel.adjust.subdivide
import org.openrndr.extra.mesh.dcel.convert.shapeToDcelNoTriangulation
import org.openrndr.extra.mesh.dcel.modify.convexFaceVertexInsert
import org.openrndr.extra.mesh.dcel.navigate.allFaces
import org.openrndr.extra.mesh.dcel.navigate.contains
import org.openrndr.extra.mesh.dcel.navigate.distinctEdges
import org.openrndr.extra.mesh.dcel.navigate.filter
import org.openrndr.extra.mesh.dcel.navigate.find
import org.openrndr.extra.mesh.dcel.navigate.isBoundary
import org.openrndr.extra.mesh.dcel.navigate.toShape
import org.openrndr.extra.mesh.dcel.query.convexFaceCenter
import org.openrndr.extra.shapes.primitives.regularPolygon

fun main() {
    application {
        configure {
            width = 800
            height = 800
        }
        program {
            val shape = regularPolygon(5, drawer.bounds.center, 60.0).shape
            val dcel = shapeToDcelNoTriangulation(shape, 0.5)

            var op = "offset"

            keyboard.character.listen {
                op = when (it.character) {
                    'o' -> "offset"
                    's' -> "subdivide"
                    'i' -> "insert"
                    else -> op
                }
            }

            mouse.buttonUp.listen {
                val position = it.position.xy0

                when (op) {
                    "offset" -> with(dcel) {
                        val face = allFaces().filter { f: Face ->
                            f.contains(position)
                        }.distinctEdges().filter { it.isBoundary }.offset(-20.0, true)
                    }
                    "subdivide" -> with(dcel) {
                        val face = allFaces().filter { f: Face ->
                            f.contains(position)
                        }.subdivide()
                    }
                    "insert" -> with(dcel) {
                        allFaces().filter { f: Face ->
                            f.contains(position)
                        }.forEach {
                            convexFaceVertexInsert(it,convexFaceCenter(it))
                        }
                    }

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