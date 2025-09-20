package frames

import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.isolated
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.meshgenerators.cylinderMesh
import org.openrndr.extra.noise.uniformRing
import org.openrndr.extra.shapes.frames.frames
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.math.Vector3
import org.openrndr.shape.path3D
import kotlin.random.Random

/**
 * Demonstrates how to create a 3D path and attach cylinders to it at regular intervals with the correct orientation.
 *
 * - The path is constructed using the `path3D` builder.
 * - A rectified copy is created to be able to sample it at equal-length intervals.
 * - We call the `frames` method on the rectified contour to generate a list with 100 transformation matrices which
 *   make it possible to attach oriented 3D objects at specific locations in the curve.
 * - We finally use the transformation matrices to draw cylinders along the 3D path.
 *
 * The orbital camera extension enables interactive 3D view manipulation.
 *
 * A fixed random seed is used to make sure this demo outputs a specific output. We can delete the
 * `random` arguments to get a unique result each time the program runs.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
        multisample = WindowMultisample.SampleCount(4)
    }
    program {
        val random = Random(0)
        val cylinder = cylinderMesh(radius = 0.5, length = 0.1)
        val p = path3D {
            moveTo(0.0, 0.0, 0.0)
            curveTo(
                Vector3.uniformRing(0.1, 1.0, random = random) * 10.0,
                Vector3.uniformRing(0.1, 1.0, random = random) * 10.0,
                Vector3.uniformRing(0.1, 1.0, random = random) * 10.0
            )
            for (i in 0 until 10) {
                continueTo(
                    Vector3.uniformRing(0.1, 1.0, random = random) * 10.0,
                    Vector3.uniformRing(0.1, 1.0, random = random) * 10.0
                )
            }
        }
        val pr = p.rectified(0.01, 100.0)


        val frames = pr.frames((0 until 100).map { it / 100.0 }, Vector3.UNIT_Y, analyticalDirections = false)
        extend(Orbital())
        extend {
            drawer.shadeStyle = shadeStyle {
                fragmentTransform = """
                        x_fill.rgb = vec3(abs(v_viewNormal.z)*0.9+ 0.1);
                    """.trimIndent()
            }

            drawer.stroke = ColorRGBa.PINK
            drawer.path(p)

            for (frame in frames) {
                drawer.isolated {
                    drawer.model = frame
                    drawer.vertexBuffer(cylinder, DrawPrimitive.TRIANGLES)
                }
            }
        }
    }
}
