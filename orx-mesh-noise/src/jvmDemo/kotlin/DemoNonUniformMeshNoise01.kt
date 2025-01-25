import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.isolated
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.mesh.IIndexedPolygon
import org.openrndr.extra.mesh.IVertexData
import org.openrndr.extra.mesh.noise.nonuniform
import org.openrndr.extra.mesh.noise.nonuniformHammersley
import org.openrndr.extra.mesh.noise.nonuniformRSeq
import org.openrndr.extra.objloader.loadOBJMeshData
import org.openrndr.extra.mesh.noise.uniform
import org.openrndr.extra.meshgenerators.normals.estimateNormals
import org.openrndr.extra.meshgenerators.sphereMesh
import org.openrndr.math.Spherical
import org.openrndr.math.Vector3
import java.io.File
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random


/**
 * The program demonstrates the loading of a 3D model, estimating its normals,
 * sampling points based on non-uniform distribution, and rendering points as spheres.
 *
 * Key functionalities include:
 * - Loading a 3D model from an OBJ file.
 * - Estimating per-vertex normals for the mesh.
 * - Generating and rendering a sphere mesh for sampled points.
 * - Using a lighting direction vector to bias the point sampling distribution.
 * - Extending the program with an orbital camera for interactive navigation.
 * - Applying shading to simulate lighting effects based on vertex normals.
 *
 * The rendering of spheres is performed by iterating over the sampled points and isolating each in the transformation matrix.
 * This setup allows customization for complex rendering pipelines.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
        multisample = WindowMultisample.SampleCount(8)
    }
    program {
        val mesh = loadOBJMeshData(File("demo-data/obj-models/suzanne/Suzanne.obj")).toMeshData().estimateNormals()

        val sphere = sphereMesh(radius = 0.0125)
        extend(Orbital()) {
            eye = Vector3(0.0, 0.0, 8.0)
            fov = 25.0
        }
        val v = Vector3(1.0, 1.0, 1.0).normalized

        val points = mesh.nonuniformRSeq(
            10000,
            false,
            Random((seconds * 0).toInt())
        ) { vertexData: IVertexData, polygon: IIndexedPolygon, vertexIndex: Int ->
            vertexData.normals[polygon.normals[vertexIndex]].dot(v).coerceIn(0.1, 1.0).pow(2.0)
        }

        extend {
            drawer.shadeStyle = shadeStyle {
                fragmentTransform = "x_fill = vec4( (v_viewNormal * 0.5 + 0.5), 1.0);"
            }
            for (point in points) drawer.isolated {
                drawer.translate(point)
                drawer.vertexBuffer(sphere, DrawPrimitive.TRIANGLES)
            }
        }
    }
}