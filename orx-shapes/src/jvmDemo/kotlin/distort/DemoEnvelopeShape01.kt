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

/**
 * Demonstrates how
 * [`Shape.envelope`](https://orx.openrndr.org/orx-shapes/org.openrndr.extra.shapes.distort/envelope.html)
 * transforms a shape by distorting its contours based on a mapping between
 * a reference rectangle and a target Bézier patch.
 *
 * The program draws four horizontal and vertical lines from the Bézier patch, which inside the patch
 * are actually curved. It also draws the shapes contained in the text "WARP", translated to the mouse position,
 * and mapped from a source Rectangle to a target Bézier patch.
 */
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
            val letters = shapesFromText(face, "WARP", 64.0)

            extend {
                val lettersAtMousePos = letters.map {
                    it.transform(transform {
                        translate(mouse.position)
                    })
                }

                val textBounds = letters.map { it.bounds }.bounds
                // center the textBounds on the window
                val sourceRectangle = textBounds.placeIn(drawer.bounds)

                // Create a bezier patch from a closed shape contour with 4 segments.
                val targetBezierPatch = bezierPatch(c.contour, cos(seconds) * 0.5 + 0.5)

                // Create a bezier patch from 4 corners
//                val t = bezierPatch(
//                    listOf(
//                        Vector2(100.0, 100.0), Vector2(400.0, 100.0),
//                        Vector2(400.0, 600.0), Vector2(200.0, 200.0)
//                    ), cos(seconds) * 0.5 + 0.5
//                )

                val lettersDistorted = lettersAtMousePos.map { it.envelope(sourceRectangle, targetBezierPatch) }

                drawer.fill = null

                drawer.stroke = ColorRGBa.GRAY
                drawer.contour(sourceRectangle.contour)
                drawer.shapes(lettersAtMousePos)

                drawer.stroke = ColorRGBa.PINK
                drawer.contour(c.contour)

                drawer.contour(targetBezierPatch.horizontal(0.0))
                drawer.contour(targetBezierPatch.horizontal(0.33))
                drawer.contour(targetBezierPatch.horizontal(0.66))
                drawer.contour(targetBezierPatch.horizontal(1.0))

                drawer.contour(targetBezierPatch.vertical(0.0))
                drawer.contour(targetBezierPatch.vertical(0.33))
                drawer.contour(targetBezierPatch.vertical(0.66))
                drawer.contour(targetBezierPatch.vertical(1.0))

                drawer.shapes(lettersDistorted)
            }
        }
    }
}