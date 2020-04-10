import org.openrndr.application
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.shadeStyle
import org.openrndr.extras.camera.Orbital
import org.openrndr.extras.meshgenerators.box
import org.openrndr.extras.meshgenerators.group
import org.openrndr.extras.meshgenerators.meshGenerator
import org.openrndr.extras.meshgenerators.sphere
import org.openrndr.math.transforms.transform

fun main() {
    application {
        program {
            extend(Orbital())
            val m = meshGenerator {
                sphere(32, 32, 1.0)
                group {
                    box(4.0, 4.0, 4.0)
                    transform(transform {
                        translate(0.0, -2.0, 0.0)
                    })
                }
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