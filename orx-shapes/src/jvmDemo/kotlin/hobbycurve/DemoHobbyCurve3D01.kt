package hobbycurve

import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.extra.noise.scatter
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.shapes.hobbycurve.hobbyCurve
import org.openrndr.math.Vector3
import kotlin.math.cos
import kotlin.random.Random

/**
 * Demonstrates how to use the 3D implementation of the `hobbyCurve` method, to draw a smooth curve passing
 * through various 3D points in space.
 *
 * The program first creates a random set of 2D points at least 200 pixels away from the window borders.
 *
 * Then, on every animation frame, it recreates a 3D hobby curve by giving depth to each 2D point.
 * The same seed is used for randomness, so the same depths are assigned on every animation frame, although
 * varying tensions are applied to each segment, based on cosines of the current time in seconds.
 *
 * Commenting out the camera rotation (`camera.rotate`) reveals how the segment tensions change over time.
 *
 * The last few lines of the program enable a rotating 3D camera and draw the 3D path.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
        multisample = WindowMultisample.SampleCount(4)
    }
    program {
        val pts = drawer.bounds.scatter(30.0, distanceToEdge = 200.0, random = Random(3000))

        extend {
            drawer.stroke = ColorRGBa.PINK
            drawer.strokeWeight = 4.0
            drawer.fill = null

            val r = Random(3000)
            val hobby3D = hobbyCurve(
                pts.map { it.xy0 + Vector3(0.0, 0.0, Double.uniform(-360.0, 360.0, r)) },
                true,
                tensions = { chordIndex, inAngle, outAngle ->
                    Pair(
                        cos(seconds + chordIndex * 0.1) * 0.5 + 0.5,
                        cos(seconds + (1.0 + chordIndex) * 0.1) * 0.5 + 0.5
                    )
                })

            drawer.isolated {
                drawer.ortho(0.0, width.toDouble(), height.toDouble(), 0.0, -4000.0, 4000.0)
                drawer.translate(width / 2.0, height / 2.0)
                drawer.rotate(Vector3.UNIT_Y, seconds * 16.0)
                drawer.translate(-width / 2.0, -height / 2.0)
                drawer.path(hobby3D)
            }
        }
    }
}