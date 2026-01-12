package rectify

import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.isolated
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.meshgenerators.sphereMesh
import org.openrndr.extra.noise.uniformRing
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.math.Vector3
import org.openrndr.shape.path3D

/**
 * Demonstrates the 3D equivalents of `ShapeContour` and `RectifiedContour`:
 * `Path3D` and `RectifiedPath3D`.
 *
 * The program creates a `Path3D` starting at the 3D origin and then adds ten 3D segments
 * with a position and two control points each. The random points are picked from
 * a 3D space, in the space between a sphere of radius 1.0 and a sphere of radius 10.0.
 * The segments in this 3D path are not of equal length.
 *
 * The 3D path is then rectified, and sampled at 500 equally spaced locations, and a
 * small sphere drawn at those locations.
 *
 * Try commenting out `.rectified(0.01, 100.0)` to observe the difference it makes.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
        multisample = WindowMultisample.SampleCount(4)
    }
    program {
        val p = path3D {
            moveTo(0.0, 0.0, 0.0)
            for (i in 0 until 10) {
                curveTo(
                    Vector3.uniformRing(0.1, 1.0) * 10.0,
                    Vector3.uniformRing(0.1, 1.0) * 10.0,
                    Vector3.uniformRing(0.1, 1.0) * 10.0
                )
            }
        }
        val pr = p.rectified(0.01, 100.0)
        val sphere = sphereMesh(radius = 0.1)
        extend(Orbital())
        extend {
            drawer.stroke = ColorRGBa.PINK
            for (i in 0 until 500) {
                drawer.isolated {
                    drawer.translate(pr.position(i / 499.0))
                    drawer.vertexBuffer(sphere, DrawPrimitive.TRIANGLES)
                }
            }
            drawer.path(p)
        }
    }
}
