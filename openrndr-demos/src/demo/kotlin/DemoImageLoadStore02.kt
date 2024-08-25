import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*


fun main() = application {
    program {
        val cb = colorBuffer(128, 128, type = ColorType.UINT8)
        cb.fill(ColorRGBa.BLACK)
        val ss = shadeStyle {
            fragmentTransform = """
                    imageStore(p_image, ivec2(30, 30), vec4(1.0, 0.0, 0.0, 1.0));
                """.trimIndent()

            image("image", cb.imageBinding(0, ImageAccess.WRITE))
        }
        extend {

            drawer.shadeStyle = ss
            drawer.clear(ColorRGBa.PINK)
            drawer.rectangle(0.0, 0.0, 100.0, 100.0)
            drawer.shadeStyle = null
            drawer.image(cb, 0.0, 200.0)
        }
    }
}