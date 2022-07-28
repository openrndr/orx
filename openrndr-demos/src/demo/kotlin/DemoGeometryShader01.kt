import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.Shader
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.meshgenerators.boxMesh
import org.openrndr.resourceText
import org.openrndr.resourceUrl

fun main() {
    application {
        program {
            val vb = boxMesh()
            val shader = Shader.createFromCode(
                vsCode = resourceText("/shaders/gs-01.vert"),
                gsCode = resourceText("/shaders/gs-01.geom"),
                fsCode = resourceText("/shaders/gs-01.frag"),
                name = "x"
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