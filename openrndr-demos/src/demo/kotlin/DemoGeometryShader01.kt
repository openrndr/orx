import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.Shader
import org.openrndr.extras.camera.Orbital
import org.openrndr.extras.meshgenerators.boxMesh
import org.openrndr.resourceUrl

fun main() {
    application {
        program {
            val vb = boxMesh()
            val shader = Shader.Companion.createFromUrls(
                vsUrl = resourceUrl("/shaders/gs-01.vert"),
                gsUrl = resourceUrl("/shaders/gs-01.geom"),
                fsUrl = resourceUrl("/shaders/gs-01.frag")
            )
            extend(Orbital())
            extend {
                drawer.clear(ColorRGBa.PINK)
                shader.begin()
                shader.uniform("offset", mouse.position.xy0)
                shader.uniform("view", drawer.view)
                shader.uniform("proj", drawer.projection)
                shader.uniform("model", drawer.model)
                driver.drawVertexBuffer(shader, listOf(vb), DrawPrimitive.TRIANGLES, 0, vb.vertexCount)
                shader.end()
            }
        }
    }
}