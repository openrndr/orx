import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.computeStyle
import org.openrndr.draw.execute
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.meshgenerators.dodecahedronMesh
import org.openrndr.math.IntVector3
import org.openrndr.math.Vector3

/**
 * This program demonstrates
 * - How to use a compute shader to deform a 3D shape (a vertex buffer).
 *   We create a dodecahedron vertex buffer and displace its vertices
 *   along their normals, exploding the object into 12 pieces with
 *   3 triangles each (3 triangles to form a pentagon).
 *
 * Use the mouse for panning, rotating and zooming.
 *
 * Output: vertexBuffer -> TRIANGLES
 */

fun main() = application {
    program {
        val vb = dodecahedronMesh(2.0)

        //vb.saveOBJ("/tmp/dodecahedron.obj") // study with Blender3D

        println(vb.vertexFormat.toString().replace("), ", "),\n  ").replace("[", "[\n  ").replace("]", "\n]"))
        println("Vertex count: ${vb.vertexCount}")

        // Create Compute Shaders
        val updateCS = computeStyle {
            computeTransform = """
                // The id of the element being currently processed
                uint id = gl_GlobalInvocationID.x;
                        
                b_vb.vertex[id].position += b_vb.vertex[id].normal * 0.01;                
            """.trimIndent()
            workGroupSize = IntVector3(64, 1, 1)
        }

        // Debugging: print the mesh data
        val shadow = vb.shadow
        shadow.download()
        val reader = shadow.reader()
        reader.rewind()
        repeat(vb.vertexCount) {
            println(it)
            // Notice how we read Vector4's instead of Vector3 or Vector2
            // because the data has been padded to align 16-byte boundaries.
            val pos = reader.readVector4()
            val nrm = reader.readVector4()
            val uv = reader.readVector4()
            println("  pos:  ${pos.xyz}")
            println("  nrm:  ${nrm.xyz}")
            println("  uv:   ${uv.xy}")
        }

        val cam = Orbital()
        cam.eye = Vector3.UNIT_Z * 5.0

        extend(cam)
        extend {
            updateCS.buffer("vb", vb.shaderStorageBufferView())
            updateCS.execute(vb.vertexCount)

            drawer.clear(ColorRGBa.GRAY)
            drawer.fill = ColorRGBa.WHITE
            drawer.shadeStyle = shadeStyle {
                fragmentTransform = "x_fill.rgb = va_normal.xyz * 0.5 + 0.5;"
            }
            drawer.vertexBuffer(vb, DrawPrimitive.TRIANGLES)
        }
    }
}
