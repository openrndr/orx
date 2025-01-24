import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.meshgenerators.*
import org.openrndr.extra.shapes.primitives.grid
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Rectangle

fun main() = application {
    configure {
        width = 720
        height = 720
        multisample = WindowMultisample.SampleCount(8)
    }
    program {
        val meshes = listOf(
            boxMesh(1.0, 1.0, 1.0),
            sphereMesh(radius = 0.5),
            dodecahedronMesh(0.5),
            cylinderMesh(radius = 0.5, length = 1.0, center = true),
            planeMesh(Vector3.ZERO, Vector3.UNIT_X, Vector3.UNIT_Y),
            capMesh(
                15, 0.5,
                listOf(Vector2.ZERO, Vector2(0.5, 0.2), Vector2.UNIT_X)
            ),
            revolveMesh(5, 0.5)
        )

        val texture = colorBuffer(256, 256)
        val s = texture.shadow
        for (y in 0 until 256) {
            for (x in 0 until 256) {
                s[x, y] = ColorRGBa(x / 256.0, y / 256.0, 0.0, 1.0)
            }
        }
        s.upload()

        val positions = Rectangle.fromCenter(Vector2.ZERO, width * 0.01, height * 0.01)
            .grid(4, 2).flatten().map {
                it.center.vector3(z = -5.0)
            }

        extend {
            drawer.clear(ColorRGBa.PINK)
            drawer.perspective(60.0, width * 1.0 / height, 0.01, 1000.0)
            drawer.depthWrite = true
            drawer.depthTestPass = DepthTestPass.LESS_OR_EQUAL
            drawer.shadeStyle = shadeStyle {
                fragmentTransform = """
                        float light = dot(v_worldNormal, p_light) * 0.5 + 0.5;
                        x_fill = texture(p_texture, va_texCoord0.xy);
                        x_fill.rgb *= light;
                    """.trimIndent()
                parameter("texture", texture)
                parameter("light", Vector3(1.0).normalized)
            }
            meshes.forEachIndexed { i, mesh ->
                drawer.isolated {
                    translate(positions[i])
                    rotate(Vector3.UNIT_Y, seconds * 12)
                    rotate(Vector3.UNIT_X, seconds * 25)
                    vertexBuffer(mesh, DrawPrimitive.TRIANGLES)
                }
            }
        }
    }
}
