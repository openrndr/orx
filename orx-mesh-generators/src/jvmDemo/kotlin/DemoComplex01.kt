import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.meshgenerators.box
import org.openrndr.extra.meshgenerators.meshGenerator
import org.openrndr.extra.meshgenerators.sphere
import org.openrndr.math.Vector3

fun main() {
    application {
        configure {
            width = 800
            height = 800
            multisample = WindowMultisample.SampleCount(8)
        }
        program {
            val m = meshGenerator {
                color = ColorRGBa.PINK
                sphere(32, 32, 1.0)

                color = ColorRGBa.WHITE
                translate(0.0, -2.0, 0.0)
                box(4.0, 4.0, 4.0)

            }

            extend(Orbital()) {
                this.eye = Vector3(0.0, 3.0, 7.0)
                this.lookAt = Vector3(0.0, 2.0, 0.0)
            }

            extend {
                drawer.shadeStyle = shadeStyle {
                    fragmentTransform = """
                        x_fill = va_color;
                        x_fill.rgb *= v_viewNormal.z;
                    """.trimIndent()
                }
                drawer.vertexBuffer(m, DrawPrimitive.TRIANGLES)
            }
        }
    }
}