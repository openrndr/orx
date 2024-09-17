import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DepthTestPass
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.mesh.loadOBJasVertexBuffer
import org.openrndr.math.Vector3

fun main() = application {
    program {
        val mesh = loadOBJasVertexBuffer("demo-data/obj-models/suzanne/Suzanne.obj")

        extend {
            drawer.perspective(60.0, width * 1.0 / height, 0.01, 1000.0)
            drawer.depthWrite = true
            drawer.depthTestPass = DepthTestPass.LESS_OR_EQUAL
            drawer.fill = ColorRGBa.PINK
            drawer.shadeStyle = shadeStyle {
                fragmentTransform = """
                        vec3 lightDir = normalize(vec3(0.3, 1.0, 0.5));
                        float l = dot(va_normal, lightDir) * 0.4 + 0.5;
                        x_fill.rgb *= l; 
                    """.trimIndent()
            }
            drawer.translate(0.0, 0.0, -2.0)
            drawer.rotate(Vector3.UNIT_X, -seconds * 2 + 30)
            drawer.rotate(Vector3.UNIT_Y, -seconds * 15 + 20)
            drawer.vertexBuffer(mesh, DrawPrimitive.TRIANGLES)
        }
    }
}