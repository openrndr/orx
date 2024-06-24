import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.Shader
import org.openrndr.draw.vertexBuffer
import org.openrndr.draw.vertexFormat
import org.openrndr.resourceText
import org.openrndr.shape.Ellipse

fun main() {
    application {
        program {

            val ellipse = Ellipse(width/2.0, height/2.0, 100.0, 200.0).contour

            val vb = vertexBuffer(vertexFormat {
                position(3)
            }, ellipse.segments.size * 4)

            val shader = Shader.createFromCode(
                    vsCode = resourceText("/shaders/ts-03.vert"),
                    tcsCode = resourceText("/shaders/ts-03.tesc"),
                    tesCode = resourceText("/shaders/ts-03.tese"),
                    gsCode = resourceText("/shaders/ts-03.geom"),
                    fsCode = resourceText("/shaders/ts-03.frag"),
                    name = "x"
            )

            vb.put {
                for (segment in ellipse.segments) {
                    val cubic = segment.cubic
                    write(cubic.start.xy0)
                    write(cubic.control[0].xy0)
                    write(cubic.control[1].xy0)
                    write(cubic.end.xy0)
                }
            }

            extend {
                drawer.clear(ColorRGBa.PINK)
                drawer.translate(width/2.0, height/2.0, 0.0)
                drawer.rotate(seconds*45.0)
                drawer.translate(-width/2.0, -height/2.0, 0.0)

                shader.begin()
                shader.uniform("offset", mouse.position.xy0)
                shader.uniform("view", drawer.view)
                shader.uniform("proj", drawer.projection)
                shader.uniform("model", drawer.model)
                shader.uniform("resolution", ((mouse.position.x / width) * 63 + 1).toInt())
                shader.uniform("weight",((mouse.position.y / height) * 128 + 1) )
                driver.drawVertexBuffer(shader, listOf(vb), DrawPrimitive.PATCHES, 0, vb.vertexCount)
                shader.end()
            }
        }
    }
}