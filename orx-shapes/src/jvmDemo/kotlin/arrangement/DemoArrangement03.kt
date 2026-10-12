package arrangement

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.color.spaces.ColorOKHSVa
import org.openrndr.extra.shapes.arrangement.Arrangement
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import kotlin.random.Random

/**
 * Demonstrates the creation of an arrangement out of ten
 * circles and ten squares. The algorithm creates shapes
 * by finding all intersections between the provided shapes.
 *
 * Then, to give the resulting pieces color, it randomizes
 * the order and assigns them a hue based on the new index,
 * covering 360 degrees of hues.
 *
 * Since the order of the pieces is randomized in every animation
 * frame, a fixed seed value is used to avoid flickering colors.
 * Another option would be to do the randomization outside
 * the animation loop.
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val circles = (0 until 10).map {
                Circle(drawer.bounds.center, (it + 1) * 20.0).shape
            }

            val rectangles = (0 until 10).map {
                Rectangle.fromCenter(drawer.bounds.center, (it + 1) * 40.0, (it + 1) * 40.0).shape
            }

            val a = Arrangement(circles + rectangles)
            backgroundColor = ColorRGBa.WHITE
            extend {
                val faces = a.originFaces.shuffled(Random(5))

                drawer.stroke = ColorRGBa.BLACK
                for ((index, face) in faces.withIndex()) {
                    drawer.fill = ColorOKHSVa(index * 360.0 / faces.size, 0.75, 1.0).toRGBa()
                    drawer.shape(face.shape)
                }
            }
        }
    }
}