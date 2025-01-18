import org.openrndr.application
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.isolated
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.objloader.loadOBJMeshData
import org.openrndr.extra.mesh.noise.uniform
import org.openrndr.extra.meshgenerators.sphereMesh
import org.openrndr.math.Vector3
import java.io.File
import kotlin.random.Random


/**
 * This demo creates a 3D visualization program using the OPENRNDR framework.
 * It demonstrates loading an OBJ model, generating uniform points on the surface
 * of the mesh, and rendering these points as small spheres using a custom shader.
 *
 * The following key processes are performed:
 * - Loading mesh data from an OBJ file.
 * - Generating a list of uniformly distributed points on the mesh surface.
 * - Rendering the generated points with small spheres.
 * - Using an "Orbital" extension for interactive camera control.
 * - Applying a shader effect to visualize surface normals.
 *
 * The application runs with a window size of 720x720 pixels and positions the camera
 * in front of the scene using the "Orbital" extension.
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val mesh = loadOBJMeshData(File("demo-data/obj-models/suzanne/Suzanne.obj")).toMeshData()
            val points = mesh.uniform(1000, Random(0))

            val sphere = sphereMesh(radius = 0.1)
            extend(Orbital()) {
                eye = Vector3(0.0, 0.0, 2.0)
            }
            extend {
                drawer.shadeStyle = shadeStyle {
                    fragmentTransform = "x_fill = vec4(v_viewNormal*0.5+0.5, 1.0);"
                }
                for (point in points) {
                    drawer.isolated {
                        drawer.translate(point)
                        drawer.vertexBuffer(sphere, DrawPrimitive.TRIANGLES)
                    }
                }
            }
        }
    }
}