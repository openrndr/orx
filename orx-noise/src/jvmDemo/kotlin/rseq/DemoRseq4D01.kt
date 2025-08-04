package rseq

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.isolated
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.meshgenerators.sphereMesh
import org.openrndr.extra.noise.rsequence.rSeq4D
import org.openrndr.math.Vector4

/**
 * Demo that presents a 3D visualization of points distributed using a 4D quasirandom sequence (R4).
 * Each point is represented as a sphere with it position and color derived from the sequence values.
 *
 * This function performs the following tasks:
 * - Configures the application window dimensions to 720x720 pixels.
 * - Initializes a 3D camera for orbital navigation of the scene.
 * - Generates 10,000 points in 4D space using the `rSeq4D` function. The points are scaled
 *   and transformed into 3D positions with an additional w-coordinate for color variation.
 * - Creates a reusable sphere mesh for rendering.
 * - Renders each point as a sphere with its position determined by the 3D coordinates
 *   of the point and its color calculated by shifting the hue of a base color using
 *   the w-coordinate value.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }

    program {
        val sphere = sphereMesh(radius = 0.1)
        val points = (0 until 10000).map {
            (rSeq4D(it) - Vector4(0.5, 0.5, 0.5, 0.0)) * Vector4(10.0, 10.0, 10.0, 1.0)
        }
        extend(Orbital())
        extend {
            for (point in points) {
                drawer.isolated {
                    drawer.translate(point.xyz)
                    drawer.fill = ColorRGBa.RED.toHSVa().shiftHue(point.w * 360.0).toRGBa()
                    drawer.vertexBuffer(sphere, DrawPrimitive.TRIANGLES)
                }
            }
        }
    }
}
