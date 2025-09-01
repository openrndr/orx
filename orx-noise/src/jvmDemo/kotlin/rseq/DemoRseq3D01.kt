package rseq

import org.openrndr.application
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.isolated
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.meshgenerators.sphereMesh
import org.openrndr.extra.noise.rsequence.rSeq3D
import org.openrndr.math.Vector3

/**
 * This demo renders a 3D visualization of points distributed using the R3 quasirandom sequence. Each point is
 * represented as a sphere and positioned in 3D space based on the quasirandom sequence values.
 *
 * The visualization setup includes:
 * - Usage of an orbital camera for interactive 3D navigation.
 * - Creation of a reusable sphere mesh with a specified radius.
 * - Generation of quasirandom points in 3D space using the `rSeq3D` function.
 * - Transformation and rendering of each point as a sphere using vertex buffers.
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
                (rSeq3D(it) - Vector3(0.5)) * 10.0
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
