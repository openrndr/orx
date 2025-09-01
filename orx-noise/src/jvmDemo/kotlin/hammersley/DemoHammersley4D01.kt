package hammersley

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.isolated
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.meshgenerators.sphereMesh
import org.openrndr.extra.noise.hammersley.hammersley4D
import org.openrndr.math.Vector4

/**
 * Demo visualizing a 4D Hammersley point set in a 3D space, with colors determined by the 4th dimension.
 *
 * A total of 10,000 4D points are generated with the `hammersley4D` sequence.
 * These points are mapped to a cubical volume and rendered as small spheres.
 * The color of each sphere is modified based on the 4th dimension of its corresponding point by
 * shifting the hue in HSV color space.
 *
 * This program employs the `Orbital` extension, enabling camera interaction for 3D navigation
 * of the scene.
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
            val points = (0 until 10000).map {
                (hammersley4D(it, 10000) - Vector4(0.5, 0.5, 0.5, 0.0)) * Vector4(10.0, 10.0, 10.0, 1.0)
            }
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
