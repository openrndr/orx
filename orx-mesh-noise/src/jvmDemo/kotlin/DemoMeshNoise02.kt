import org.openrndr.application
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.isolated
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.mesh.noise.hash
import org.openrndr.extra.objloader.loadOBJMeshData
import org.openrndr.extra.mesh.noise.uniform
import org.openrndr.extra.meshgenerators.sphereMesh
import org.openrndr.math.Vector3
import java.io.File
import kotlin.math.cos
import kotlin.random.Random

/**
 * Demonstrate uniform point on mesh generation using hash functions
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val mesh = loadOBJMeshData(File("demo-data/obj-models/suzanne/Suzanne.obj")).toMeshData()

            val sphere = sphereMesh(radius = 0.01)
            extend(Orbital()) {
                eye = Vector3(0.0, 0.0, 2.0)
            }
            extend {

                val points = mesh.hash((1000 + (cos(seconds)*0.5+0.5)*9000).toInt(), 808, (seconds*10000).toInt())


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