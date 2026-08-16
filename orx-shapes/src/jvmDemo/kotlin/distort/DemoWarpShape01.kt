package distort

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.font.loadFace
import org.openrndr.extra.shapes.distort.warp
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.extra.shapes.text.shapesFromText
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.transforms.transform
import org.openrndr.shape.Circle
import org.openrndr.shape.Ellipse
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Rectangle

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {

            extend(ScreenRecorder()) {
                frameClock = false
            }

            val c = Ellipse(drawer.bounds.center, 300.0, 200.0).contour.rectified()

            val face = loadFace("demo-data/fonts/IBMPlexMono-Regular.ttf", 64.0, 1.0)

            val g = shapesFromText(face, "WARP")

            extend {

                val s = g.map {
                    it.transform(transform {
                        translate(mouse.position)
                    })
                }

                val b = drawer.bounds.horizontal(0.5).contour.rectified()
                val w = s.map { it.warp(b, c) }


                drawer.fill = null
                drawer.stroke = ColorRGBa.GRAY
                drawer.contour(b.contour)
                drawer.shapes(s)

                drawer.stroke = ColorRGBa.PINK
                drawer.contour(c.contour)
                drawer.shapes(w)
            }
        }
    }
}