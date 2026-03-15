import org.openrndr.application
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.dnk3.gltf.loadGltfFromFile
import org.openrndr.math.Vector3
import java.io.File

/**
 * Demonstrates how to collect the 3D meshes found in a gltf file and render them
 * in the program window.
 *
 * The default settings of an Orbital camera would render the meshes too close,
 * therefore the `far`, `lookAt`, `eye` and `fov` properties are adjusted to provide
 * a better view of the models.
 *
 * Meshes can provide (or not) an `indexBuffer`. The program how to render both types of mesh.
 */
fun main() = application {
    program {
        val gltf = loadGltfFromFile(File("demo-data/gltf-models/duck/Duck.gltf"))

        // Get the meshes from the loaded file.
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
            // A minimal shader to simulate directional light coming from the camera.
            drawer.shadeStyle = shadeStyle {
                fragmentTransform = "x_fill.rgb = vec3(v_viewNormal.z);"
            }
            for (mesh in meshes) {
                for (primitive in mesh) {
                    if (primitive.indexBuffer == null) {
                        drawer.vertexBuffer(primitive.vertexBuffer, DrawPrimitive.TRIANGLES)
                    } else {
                        drawer.vertexBuffer(
                            primitive.indexBuffer!!,
                            listOf(primitive.vertexBuffer),
                            DrawPrimitive.TRIANGLES
                        )
                    }
                }
            }
        }
    }
}