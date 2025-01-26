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
