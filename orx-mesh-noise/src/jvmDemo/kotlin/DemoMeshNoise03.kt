import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.isolated
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.objloader.loadOBJMeshData
import org.openrndr.extra.mesh.noise.uniformPoints
import org.openrndr.extra.meshgenerators.cylinderMesh
import org.openrndr.extra.meshgenerators.normals.estimateNormals
import org.openrndr.extra.meshgenerators.tangents.estimateTangents
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.buildTransform
import java.io.File
import kotlin.math.cos
import kotlin.random.Random

/**
 * This demo loads a 3D model from an OBJ file, processes the mesh data to estimate normals and tangents, and generates
 * a set of uniformly distributed pose points. These pose points determine the transformations applied to individual
 * objects rendered in the viewport.
 *
 * It extends the rendering with an orbital camera for navigation and shaders for custom visual
 * effects. Cylinders represent transformed objects, with their scale animations based on time-dependent
 * trigonometric functions.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
        multisample = WindowMultisample.SampleCount(8)
    }
    program {
        val mesh = loadOBJMeshData(File("demo-data/obj-models/suzanne/Suzanne.obj")).toMeshData().triangulate()
            .estimateNormals().estimateTangents()
        val poses = mesh.uniformPoints(10000, Random(0)).map { it.pose() }

        val cylinder = cylinderMesh(radius = 0.01, length = 0.2)
        extend(Orbital()) {
            eye = Vector3(0.0, 0.0, 2.0)
        }
        extend {
            drawer.shadeStyle = shadeStyle {
                fragmentTransform = "x_fill = vec4(v_viewNormal*0.5+0.5, 1.0);"
            }
            for (pose in poses) {
                drawer.isolated {
                    drawer.model = buildTransform {
                        multiply(pose)
                        scale(1.0, 1.0, cos(pose.c3r0 * 10.0 + seconds) * 0.5 + 0.5)
                    }
                    drawer.vertexBuffer(cylinder, DrawPrimitive.TRIANGLES)
                }
            }
        }
    }
}