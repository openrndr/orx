import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.TransformTarget
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.objloader.readObjMeshData
import org.openrndr.extra.objloader.loadOBJasVertexBuffer
import org.openrndr.extra.mesh.wireframe
import org.openrndr.math.Vector3
import org.openrndr.shape.Path3D
import java.io.File
import kotlin.math.cos

/**
 * Demonstrates two approaches for loading an OBJ file: as a `VertexBuffer` and as `CompoundMeshData`.
 *
 * A `CompoundMeshData` object contains vertices, texture coordinates, colors, normals, tangents, and bitangents,
 * alongside their associated face indices, grouped into meshes.
 *
 * In this demo `CompoundMeshData.wireframe()` is called to generate a wireframe representation of the loaded mesh.
 *
 * When rendering the wireframe, a shade style is used to displace the lines slightly towards the camera, to ensure
 * the lines do not end up occluded by the mesh rendered as triangles.
 *
 * Finally, the `sub` method is called on the `Path3D` instances to draw only parts of the wireframe, creating
 * an animated effect.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
        multisample = WindowMultisample.SampleCount(4)
    }
    program {
        val vb = loadOBJasVertexBuffer("orx-obj-loader/test-data/non-planar.obj")
        val md = readObjMeshData(File("orx-obj-loader/test-data/non-planar.obj").readLines())

        val paths = md.wireframe().map {
            Path3D.fromPoints(it, true)
        }

        extend(Orbital())
        extend {
            drawer.rotate(Vector3.Companion.UNIT_Y, seconds * 45.0 + 45.0, TransformTarget.MODEL)
            drawer.translate(0.0, 0.0, 9.0, TransformTarget.VIEW)
            drawer.shadeStyle = shadeStyle {
                fragmentTransform = """
                        x_fill.rgb = normalize(v_viewNormal) * 0.5 + vec3(0.5);
                    """.trimIndent()
            }

            drawer.vertexBuffer(vb, DrawPrimitive.TRIANGLES)
            drawer.stroke = ColorRGBa.WHITE
            drawer.strokeWeight = 1.0

            drawer.shadeStyle = shadeStyle {
                vertexTransform = """
                        x_projectionMatrix[3][2] -= 0.001;
                    """.trimIndent()
            }

            drawer.strokeWeight = 1.0
            drawer.paths(paths.mapIndexed { index, it ->
                it.sub(
                    0.0, cos(seconds * 0.5 + index * 0.5) * 0.5 + 0.5
                )
            })
        }
    }
}
