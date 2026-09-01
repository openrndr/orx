package distort

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.font.loadFace
import org.openrndr.extra.shapes.bezierpatches.bezierPatch
import org.openrndr.extra.shapes.distort.envelope
import org.openrndr.extra.shapes.primitives.contour
import org.openrndr.extra.shapes.primitives.placeIn
import org.openrndr.extra.shapes.text.shapesFromText
import org.openrndr.math.transforms.transform
import org.openrndr.shape.Circle
import org.openrndr.shape.bounds
import kotlin.math.cos

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            //val c = Ellipse(drawer.bounds.center, 300.0, 300.0).contour.shift(0.25).rectified()

            val c = Circle(drawer.bounds.center, 300.0).contour(4, 45.0 + 180.0)
            val face = loadFace("demo-data/fonts/IBMPlexMono-Regular.ttf")

            val g = shapesFromText(face, "WARP", 64.0)

            extend {

                val s = g.map {
                    it.transform(transform {
                        translate(mouse.position)
                    })
                }

                val b = g.map { it.bounds }.bounds.placeIn(drawer.bounds)

                val t = bezierPatch(c.contour, cos(seconds) * 0.5 +0.5)

//                val t = bezierPatch(
//                    listOf(
//                    Vector2(100.0, 100.0), Vector2(400.0, 100.0),
//                    Vector2(400.0, 600.0), Vector2(200.0, 200.0)), cos(seconds) * 0.5 +0.5)

                val e = s.map { it.envelope(b, t) }


                drawer.fill = null
                drawer.stroke = ColorRGBa.GRAY
                drawer.contour(b.contour)
                drawer.shapes(s)

                drawer.stroke = ColorRGBa.PINK
                drawer.contour(c.contour)
                drawer.contour(t.horizontal(0.0))
                drawer.contour(t.horizontal(0.33))
                drawer.contour(t.horizontal(0.66))

                drawer.contour(t.horizontal(1.0))
                drawer.contour(t.vertical(0.0))
                drawer.contour(t.vertical(0.33))
                drawer.contour(t.vertical(0.66))

                drawer.contour(t.vertical(1.0))

                drawer.shapes(e)
            }
        }
    }
}