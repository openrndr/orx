import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.isolated
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.hashgrid.filter
import org.openrndr.extra.meshgenerators.sphereMesh
import org.openrndr.extra.noise.uniformRing
import org.openrndr.math.Vector3
import kotlin.random.Random

fun main() = application {
    configure {
        width = 720
        height = 720
        multisample = WindowMultisample.SampleCount(4)
    }
    program {
        val r = Random(0)
        val points = (0 until 10000).map {
            Vector3.uniformRing(0.0, 10.0, r)
        }
        val sphere = sphereMesh(radius = 0.25)
        val filteredPoints = points.filter(0.5)

        extend(Orbital()) {
            eye = Vector3(0.0, 0.0, 15.0)
        }
        extend {
            drawer.shadeStyle = shadeStyle {
                fragmentTransform = """x_fill.rgb *= abs(v_viewNormal.z);"""
            }
            for (point in filteredPoints) {
                drawer.isolated {
                    drawer.translate(point)
                    drawer.vertexBuffer(sphere, DrawPrimitive.TRIANGLES)
                }
            }
        }
    }
}