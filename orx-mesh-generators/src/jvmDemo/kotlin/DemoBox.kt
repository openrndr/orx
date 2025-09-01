import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.CullTestPass
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.meshgenerators.boxMesh
import org.openrndr.math.Vector3

fun main() = application {
    configure {
        width = 720
        height = 720
        multisample = WindowMultisample.SampleCount(8)
    }
    program {
        val box = boxMesh(1.0, 1.0, 1.0)

        val texture = colorBuffer(256, 256)
        val s = texture.shadow
        for (y in 0 until 256) {
            for (x in 0 until 256) {
                s[x, y] = ColorRGBa(x / 256.0, y / 256.0, 0.0, 1.0)
            }
        }
        s.upload()

        extend(Orbital()) {
            eye = Vector3(1.0, 1.0, 1.0)
        }
        extend {
            drawer.clear(ColorRGBa.PINK)
            drawer.shadeStyle = shadeStyle {
                fragmentTransform = """
                        x_fill = texture(p_texture, va_texCoord0.xy);
                    """.trimIndent()
                parameter("texture", texture)
            }
            drawer.drawStyle.cullTestPass = CullTestPass.FRONT
            drawer.vertexBuffer(box, DrawPrimitive.TRIANGLES)
        }
    }
}