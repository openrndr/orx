import org.openrndr.application
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.isolated
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.mesh.loadOBJMeshData
import org.openrndr.extra.mesh.noise.uniform
import org.openrndr.extra.meshgenerators.sphereMesh
import org.openrndr.math.Vector3
import java.io.File
import kotlin.random.Random

/**
 * Demonstrate uniform point on mesh generation
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