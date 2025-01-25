import org.openrndr.application
import org.openrndr.color.rgb
import org.openrndr.draw.*

fun main() = application {
    program {
        val cb = colorBuffer(128, 128, type = ColorType.UINT8)
        val at = arrayTexture(128, 128, 32, type = ColorType.UINT8)
        val vt = volumeTexture(32, 32, 32, type = ColorType.UINT8)
        extend {
            val ss = shadeStyle {
                fragmentTransform = """
                    imageStore(p_image, ivec2(30.0, 30.0), vec4(1.0, 0.0, 0.0, 1.0));
                    imageStore(p_vt, ivec3(2, 2, 2), vec4(1.0, 0.0, 0.0, 1.0));
                    imageStore(p_at, ivec3(2, 2, 2), vec4(1.0, 0.0, 0.0, 1.0));
                """.trimIndent()

                image("at", at.imageBinding(0, ImageAccess.READ_WRITE))
                image("image", cb.imageBinding(0, ImageAccess.READ_WRITE))
                image("vt", vt.imageBinding(0, ImageAccess.READ_WRITE))
            }
            drawer.shadeStyle = ss
            drawer.clear(rgb(0.1))
            drawer.fill = rgb(0.2)
            drawer.rectangle(0.0, 0.0, 100.0, 100.0)
            drawer.image(cb, 0.0, 200.0)
        }
    }
}