import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.draw.CullTestPass
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.meshgenerators.buildTriangleMesh
import org.openrndr.extra.meshgenerators.extrudeShape
import org.openrndr.extra.meshgenerators.grid
import org.openrndr.extra.meshgenerators.twist
import org.openrndr.math.Vector3
import org.openrndr.shape.Circle

fun main() = application {
    configure {
        width = 720
        height = 720
        multisample = WindowMultisample.SampleCount(8)
    }
    program {
        extend(Orbital()) {
            this.eye = Vector3(0.0, 30.0, 50.0)
        }
        val m = buildTriangleMesh {
            grid(5, 5, 5) { u, v, w ->
                isolated {
                    translate(u * 20.0, v * 20.0, w * 20.0)
                    extrudeShape(Circle(0.0, 0.0, 50.0).shape, 4.0, scale = 0.1)
                }
            }
            twist(360.0 / 200.0, 0.0)
            twist(360.0 / 200.0, 0.0, Vector3.UNIT_X)
            twist(360.0 / 200.0, 0.0, Vector3.UNIT_Z)
        }

        extend {
            drawer.shadeStyle = shadeStyle {
                fragmentTransform = """
                        x_fill.rgb *= v_viewNormal.z;
                    """.trimIndent()
            }
            drawer.drawStyle.cullTestPass = CullTestPass.FRONT
            drawer.vertexBuffer(m, DrawPrimitive.TRIANGLES)
        }
    }
}
