import org.openrndr.application
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.shadeStyle
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.dnk3.gltf.loadGltfFromFile
import org.openrndr.extra.camera.Orbital
import org.openrndr.math.Vector3
import java.io.File

fun main() = application {
    program {
        val gltf = loadGltfFromFile(File("demo-data/gltf-models/duck/Duck.gltf"))
        val meshes = gltf.meshes.map {
            it.createDrawCommands(gltf)
        }

        extend(Orbital()) {
            far = 400.0
            lookAt = Vector3(0.0, 50.0, 0.0)
            eye = Vector3(100.0, 200.0, 150.0)
            fov = 45.0
        }

        extend {
            drawer.shadeStyle = shadeStyle {
                fragmentTransform = """
                            x_fill.rgb = vec3(v_viewNormal.z);
                        """.trimIndent()
            }
            for (mesh in meshes) {
                for (primitive in mesh) {
                   if (primitive.indexBuffer == null) {
                        drawer.vertexBuffer(primitive.vertexBuffer, DrawPrimitive.TRIANGLES)
                    } else {
                        drawer.vertexBuffer(primitive.indexBuffer!!, listOf(primitive.vertexBuffer), DrawPrimitive.TRIANGLES)
                    }
                }
            }
        }
    }
}