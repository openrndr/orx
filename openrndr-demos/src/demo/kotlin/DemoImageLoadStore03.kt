import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*


suspend fun main() = application {
    program {
        val cb = colorBuffer(128, 128)
        val at = arrayTexture(128, 128, 32)
        val vt = volumeTexture(32, 32, 32)
        extend {
            val ss = shadeStyle {
                fragmentTransform = """
                    imageStore(p_image, ivec2(30.0, 30.0), vec4(1.0, 0.0, 0.0, 1.0));
                    imageStore(p_vt, ivec3(2, 2, 2), vec4(1.0, 0.0, 0.0, 1.0));
                    imageStore(p_at, ivec3(2, 2, 2), vec4(1.0, 0.0, 0.0, 1.0));
                """.trimIndent()

                parameter("at", at.imageBinding(0, ImageAccess.READ_WRITE))
                parameter("image", cb.imageBinding(0, ImageAccess.READ_WRITE))
                parameter("vt", vt.imageBinding(0, ImageAccess.READ_WRITE))
            }
            drawer.shadeStyle = ss
            drawer.clear(ColorRGBa.PINK)
            drawer.rectangle(0.0, 0.0, 100.0, 100.0)
            drawer.image(cb, 0.0, 200.0)
        }
    }
}