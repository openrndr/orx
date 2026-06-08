package dcel

import org.openrndr.KeyModifier
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.extra.mesh.dcel.convert.faceToPolygon3D
import org.openrndr.extra.mesh.dcel.Face
import org.openrndr.extra.mesh.dcel.FaceList
import org.openrndr.extra.mesh.dcel.adjust.join
import org.openrndr.extra.mesh.dcel.adjust.offset
import org.openrndr.extra.mesh.dcel.adjust.relax
import org.openrndr.extra.mesh.dcel.adjust.remove
import org.openrndr.extra.mesh.dcel.adjust.subdivide
import org.openrndr.extra.mesh.dcel.convert.shapeToDcelNoTriangulation
import org.openrndr.extra.mesh.dcel.modify.convexFaceVertexInsert
import org.openrndr.extra.mesh.dcel.navigate.allFaces
import org.openrndr.extra.mesh.dcel.navigate.contains
import org.openrndr.extra.mesh.dcel.navigate.distinctEdges
import org.openrndr.extra.mesh.dcel.navigate.filter
import org.openrndr.extra.mesh.dcel.navigate.isBoundary
import org.openrndr.extra.mesh.dcel.navigate.toShape
import org.openrndr.extra.mesh.dcel.query.convexFaceCenter
import org.openrndr.extra.mesh.dcel.saveToCborFile
import org.openrndr.extra.mesh.rtree.RtreeDcelFace2D
import org.openrndr.extra.rtree.RtreePolygon2D
import org.openrndr.extra.shapes.polygon.Polygon2D
import org.openrndr.extra.shapes.polygon.xy
import org.openrndr.extra.shapes.primitives.regularPolygon
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.shape.ShapeContour
import java.io.File

fun main() {
    application {
        configure {
            width = 800
            height = 800
        }
        program {

            extend(ScreenRecorder()) {
                frameClock = false
                contentScale = 1.0
            }


            val selectedFaces = mutableSetOf<Int>()


            val shape = regularPolygon(5, drawer.bounds.center, 60.0).shape
            val dcel = shapeToDcelNoTriangulation(shape, 0.5)
            var rtree = RtreeDcelFace2D(dcel)

            fun updateRtree() {
                rtree = RtreeDcelFace2D(dcel)
                for (f in dcel.allFaces()) {
                    val polygon = dcel.faceToPolygon3D(f).xy
                    rtree.insert(f)
                }
            }

            var op = "offset"

            keyboard.character.listen {
                op = when (it.character) {
                    'o' -> "offset"
                    's' -> "subdivide"
                    'i' -> "insert"
                    'd' -> "delete"
                    'j' -> "join"
                    'r' -> "relax"
                    'x' -> { selectedFaces.clear(); op }
                    'a' -> { selectedFaces.addAll(dcel.allFaces()); op }
                    'w' -> { dcel.saveToCborFile(File("dcel.cbor")); op }
                    else -> op
                }
            }

            val intersectingPolygons = mutableListOf<Polygon2D>()
            mouse.buttonUp.listen {

                updateRtree()
                if (it.modifiers.contains(KeyModifier.SHIFT)) {
                    selectedFaces.addAll(
                    rtree.findContaining(it.position)
                    )

                    return@listen
                }
                if (it.modifiers.contains(KeyModifier.ALT)) {
                    selectedFaces.removeAll(
                        rtree.findContaining(it.position)
                    )
                    return@listen
                }

                val position = it.position.xy0


                when (op) {
                    "offset" -> with(dcel) {
                        val face = allFaces().filter { f: Face ->
                            f.contains(position)
                        }
                        if (face.isNotEmpty())
                        FaceList((selectedFaces + listOf(face.first())).toList())
                        .distinctEdges().filter { it.isBoundary }.offset(
                            -20.0, true,

                            { polygon ->
                                val intersecting = rtree.findIntersecting(polygon)
                                //intersectingPolygons.addAll(intersecting)
                                if (intersecting.isNotEmpty()) {
                                    //intersectingPolygons.add(polygon.xy)
                                }
                                intersecting.isEmpty()
                            }

                        )
                    }

                    "subdivide" -> with(dcel) {
                        val face = allFaces().filter { f: Face ->
                            f.contains(position)
                        }
                        if (face.isNotEmpty())
                            FaceList((selectedFaces + listOf(face.first())).toList())
                            .subdivide()
                    }

                    "insert" -> with(dcel) {
                        allFaces().filter { f: Face ->
                            f.contains(position)
                        }.forEach {
                            convexFaceVertexInsert(it, convexFaceCenter(it))
                        }
                    }

                    "delete" -> with(dcel) {
                        val face = allFaces().filter { f: Face ->
                            f.contains(position)
                        }
                        if (face.isNotEmpty())
                            FaceList((selectedFaces + listOf(face.first())).toList())
                                .remove()
                    }
                    "join" -> with(dcel) {
                        val face = allFaces().filter { f: Face ->
                            f.contains(position)
                        }
                        if (face.isNotEmpty()) {
                            FaceList((selectedFaces + listOf(face.first())).toList())
                                .join()
                        }
                    }
                    "relax" -> with(dcel) {
                        val face = allFaces().filter { f: Face ->
                            f.contains(position)
                        }
                        if (face.isNotEmpty()) {
                            FaceList((selectedFaces + listOf(face.first())).toList())
                                .relax()
                        }
                    }

                }

            }

            extend {
                drawer.clear(ColorRGBa.PINK)
                with(dcel) {
                    for (i in faces.indices) {
                        val face = faces[i]
                        if (face.edge == -1)
                            continue
                        drawer.fill = ColorRGBa.WHITE

                        if (i in selectedFaces) {
                            drawer.fill = ColorRGBa.GRAY
                        }

                        if (face.contains(mouse.position.xy0)) {
                            drawer.fill = ColorRGBa.GREEN
                        }
                        drawer.shape(face.toShape())
                    }
                }
                drawer.fill = ColorRGBa.RED
                for (polygon in intersectingPolygons) {
                    drawer.contour(ShapeContour.fromPoints(polygon, true))
                }
                drawer.fill = ColorRGBa.WHITE
                drawer.fontMap = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 16.0)
                drawer.text("op: $op", 10.0, 40.0)

            }

        }
    }
}