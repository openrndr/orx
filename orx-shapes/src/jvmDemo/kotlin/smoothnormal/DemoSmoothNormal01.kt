package smoothnormal

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.distort.distortUniform
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.extra.shapes.smoothnormal.smoothNormalLinear
import org.openrndr.shape.Rectangle

/**
 * Interactive demonstration of
 * [RectifiedContour.smoothNormalLinear(t, radius)](https://orx.openrndr.org/orx-shapes/org.openrndr.extra.shapes.smoothnormal/smooth-normal-linear.html),
 * an alternative to `RectifiedContour.normal(t)`.
 *
 * The vertical mouse position controls the applied `radius`: 0.0 at the top and 50.0 at the bottom.
 * We choose a maximum radius of 50.0 because that's half the side of the base rectangle. To see what the result
 * would look like when using just `.normal(t)`, move the mouse pointer all the way to the top to make the `radius` 0.0.
 *
 * The program creates 64 segments pointing out from the base rectangle and
 * 10 contours offset outwards.
 *
 * The position of the segments and the curvature of the contours varies depending on the applied radius.
 *
 * See [distortUniform](https://orx.openrndr.org/orx-shapes/org.openrndr.extra.shapes.distort/distort-uniform.html).
 * See [RectifiedContour.smoothNormalAngular](https://orx.openrndr.org/orx-shapes/org.openrndr.extra.shapes.smoothnormal/smooth-normal-angular.html)
 */
fun main() {
    application {
        program {
            val r = Rectangle.fromCenter(drawer.bounds.center, 100.0, 100.0).contour
            val rc = r.rectified()

            extend {
                drawer.fill = null
                drawer.stroke = ColorRGBa.PINK
                drawer.contour(r)

                val radius = 50.0 * mouse.position.y / height

                // Segments pointing outwards from the original rectangle
                for (i in 0 until 64) {
                    val p = rc.position(i / 64.0)
                    val n = rc.smoothNormalLinear(i / 64.0, radius)
                    drawer.lineSegment(p, p + n * 100.0)
                }

                // Offset contours
                for (j in 0 until 10) {
                    val offset = rc.distortUniform(sampleDistance = 1.0, errorTolerance = 0.1) { t, position ->
                        position + rc.smoothNormalLinear(t, radius) * (j * 10.0)
                    }
                    drawer.contour(offset)
                }
            }
        }
    }
}