package bezierpatch

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.bezierpatches.bezierPatch
import org.openrndr.extra.shapes.bezierpatches.distort
import org.openrndr.extra.shapes.primitives.regularStarRounded
import org.openrndr.math.transforms.transform
import org.openrndr.shape.Circle
import org.openrndr.shape.ShapeContour

/**
 * Shows how to distort [ShapeContour]s using a [bezierPatch]
 *
 * In this case the contours are regular stars and the bezier patch
 * is created using a circular contour with the required 4 segments.
 */
fun main() {
    application {
        configure {
            width = 800
            height = 800
        }
        program {
            val bp = bezierPatch(
                Circle(width / 2.0, height / 2.0, 350.0).contour
            )
            val star = regularStarRounded(
                7, 30.0, 40.0,
                0.5, 0.5
            )

            extend {
                drawer.clear(ColorRGBa.PINK)

                // draw grid
                for (i in 0..50) {
                    drawer.stroke = ColorRGBa.BLACK
                    drawer.contour(bp.horizontal(i / 50.0))
                    drawer.contour(bp.vertical(i / 50.0))
                }

                // draw stars
                drawer.fill = ColorRGBa.PINK
                for (j in 1 until 10) {
                    for (i in 1 until 10) {
                        val starMoved = star.transform(
                            transform {
                                translate(j * width / 10.0, i * height / 10.0)
                            }
                        )
                        drawer.contour(bp.distort(starMoved, drawer.bounds))
                    }
                }
            }
        }
    }
}