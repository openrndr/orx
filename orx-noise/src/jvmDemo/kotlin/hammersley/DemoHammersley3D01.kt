package hammersley

import org.openrndr.application
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.isolated
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.meshgenerators.sphereMesh
import org.openrndr.extra.noise.hammersley.hammersley3D
import org.openrndr.math.Vector3

/**
 * Demo program rendering a 3D visualization of points distributed using the Hammersley sequence in 3D space.
 *
 * A set of 1400 points is generated using the Hammersley sequence.
 * Each point is translated and rendered as a small sphere
 * in 3D space.
 *
 * The rendering uses the Orbital extension, enabling an interactive 3D camera
 * to navigate the scene.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }

    program {
        val sphere = sphereMesh(radius = 0.1)
        extend(Orbital())
        extend {
            val points = (0 until 1400).map {
                (hammersley3D(it, 1400) - Vector3(0.5)) * 10.0
            }
            for (point in points) {
                drawer.isolated {
                    drawer.translate(point)
                    drawer.vertexBuffer(sphere, DrawPrimitive.TRIANGLES)
                }
            }
        }
    }
}
