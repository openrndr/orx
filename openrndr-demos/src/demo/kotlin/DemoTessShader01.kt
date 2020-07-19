import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.Vector3
import org.openrndr.resourceUrl

fun main() {
    application {
        program {
            val vb = vertexBuffer(vertexFormat {
                position(3)
            }, 12)
            val shader = Shader.Companion.createFromUrls(
                    vsUrl = resourceUrl("/shaders/ts-01.vert"),
                    tcsUrl = resourceUrl("/shaders/ts-01.tesc"),
                    tesUrl = resourceUrl("/shaders/ts-01.tese"),
                    fsUrl = resourceUrl("/shaders/ts-01.frag")
            )

            vb.put {
                write(Vector3(0.0, 0.0, 0.0))
                write(Vector3(100.0, 0.0, 0.0))
                write(Vector3(140.0, 200.0, 0.0))
                write(Vector3(200.0, 300.0, 0.0))
                write(Vector3(0.0, 0.0, 0.0))
                write(Vector3(100.0, 0.0, 0.0))
                write(Vector3(140.0, 200.0, 0.0))
                write(Vector3(200.0, 400.0, 0.0))
                write(Vector3(0.0, 0.0, 0.0))
                write(Vector3(100.0, 0.0, 0.0))
                write(Vector3(140.0, 200.0, 0.0))
                write(Vector3(200.0, 500.0, 0.0))
            }

            extend {
                drawer.clear(ColorRGBa.PINK)
                shader.begin()
                shader.uniform("offset", mouse.position.xy0)
                shader.uniform("view", drawer.view)
                shader.uniform("proj", drawer.projection)
                shader.uniform("model", drawer.model)
                driver.drawVertexBuffer(shader, listOf(vb), DrawPrimitive.PATCHES, 0, vb.vertexCount)

                shader.end()
            }
        }
    }
}