import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extras.meshgenerators.planeMesh
import org.openrndr.internal.Driver
import org.openrndr.math.Vector3


suspend fun main() = application {
    program {
        val cb = colorBuffer(128, 128)
        extend {
            val ss = shadeStyle {
                fragmentTransform = """
                    imageStore(p_image, ivec2(30.0, 30.0), vec4(1.0, 0.0, 0.0, 1.0));
                """.trimIndent()

                parameter("image", cb.imageBinding(0, ImageAccess.READ_WRITE))
            }
            drawer.shadeStyle = ss
            drawer.clear(ColorRGBa.PINK)
            drawer.rectangle(0.0, 0.0, 100.0, 100.0)
            drawer.image(cb, 0.0, 200.0)
        }
    }
}