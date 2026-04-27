package dcel

import kotlinx.coroutines.yield
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.extra.color.presets.CRIMSON
import org.openrndr.extra.color.spaces.OKHSV
import org.openrndr.extra.color.tools.shiftHue
import org.openrndr.extra.mesh.dcel.convert.faceToShape
import org.openrndr.extra.mesh.dcel.convert.shapeToDcelNoTriangulation
import org.openrndr.extra.mesh.dcel.convert.toDcel
import org.openrndr.extra.mesh.dcel.modify.edgeInsert
import org.openrndr.extra.mesh.dcel.modify.edgeRemove
import org.openrndr.extra.mesh.dcel.query.edgeLoopIndices
import org.openrndr.extra.mesh.generate.gridMesh
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.shapes.primitives.grid
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.launch
import org.openrndr.shape.Circle
import org.openrndr.shape.Shape

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {

            val grid = gridMesh(drawer.bounds, 40, 40)
            val dcel = grid.toDcel()


            extend(ScreenRecorder()) {
                frameRate = 5.0
            }

            launch {

                for (r in 0 until 100) {
                    for (i in 0 until 40) {
                        val f = dcel.faces.random()
                        if (f.edge != -1) {
                            val es = dcel.edgeLoopIndices(f.edge)
                            val i = Int.uniform(0, es.size)
                    //        dcel.edgeInsert(es[(i + 0).mod(es.size)], es[(i + 2).mod(es.size)])
                            //yield()
                        }

                    }

                    yield()
                    for (i in 0 until 40) {
                        dcel.edgeRemove(dcel.halfEdges.random())

                    }
                    yield()
                }
            }




            extend {
                val shapes = (0 until dcel.faces.size).map { dcel.faceToShape(it) }

                for ((index, shape) in shapes.withIndex()) {
                    drawer.fill = ColorRGBa.CRIMSON.shiftHue<OKHSV>(index * 90.0)
                    drawer.stroke = ColorRGBa.BLACK
                    drawer.shape(shape)
                }

            }
        }
    }
}