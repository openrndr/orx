import org.openrndr.application
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.shadeStyle
import org.openrndr.extras.camera.Orbital
import org.openrndr.extras.meshgenerators.*
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.transform
import org.openrndr.shape.Circle

fun main() {
    application {
        program {
            extend(Orbital())
            val m = meshGenerator {

                grid(5,5, 5) { u, v, w ->
                    extrudeShape(Circle(0.0, 0.0, 50.0).shape, 4.0, scale = 0.1)
                    transform(transform{ translate(u*20.0, v*20.0, w * 20.0)} )
                }
                twist(360.0/200.0, 0.0)
                twist(360.0/200.0, 0.0, Vector3.UNIT_X)
                twist(360.0/200.0, 0.0, Vector3.UNIT_Z)
            }
            extend {
                drawer.shadeStyle = shadeStyle {
                    fragmentTransform = """
                        x_fill.rgb *= v_viewNormal.z;
                    """.trimIndent()
                }
                drawer.vertexBuffer(m, DrawPrimitive.TRIANGLES)
            }
        }
    }
}