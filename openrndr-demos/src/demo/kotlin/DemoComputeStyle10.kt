import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.computeStyle
import org.openrndr.draw.execute
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.color.presets.WHEAT
import org.openrndr.extra.meshgenerators.dodecahedronMesh
import org.openrndr.math.IntVector3
import org.openrndr.math.Vector3

/**
 * This program is a variation of compute09.kt. It draws `vb`
 * multiple times, each with a unique translation and rotation.
 *
 * For each item drawn, the instance number is sent to the shade style
 * as a float uniform (named `p_i`) to shade them with unique hues.
 * The interpolated normal varying is used to set the color, and
 * this color rotated using `p_i` as the rotation angle.
 *
 * Use the mouse for panning, rotating and zooming.
 *
 * Output: vertexBuffer -> TRIANGLES
 */

fun main() = application {
    program {
        val vb = dodecahedronMesh(2.0)

        // Create Compute Shaders
        val updateCS = computeStyle {
            computeTransform = """
                // The id of the element being currently processed
                uint id = gl_GlobalInvocationID.x;
                        
                b_vb.vertex[id].position += b_vb.vertex[id].normal * 0.01;                
            """.trimIndent()
            workGroupSize = IntVector3(64, 1, 1)
        }

        val cam = Orbital()
        cam.eye = Vector3.UNIT_Z * 5.0

        val style = shadeStyle {
            // From https://github.com/dmnsgn/glsl-rotate
            fragmentPreamble = """
                mat4 rotation3d(vec3 axis, float angle) {
                  axis = normalize(axis);
                  float s = sin(angle);
                  float c = cos(angle);
                  float oc = 1.0 - c;

                  return mat4(
                    oc * axis.x * axis.x + c,           oc * axis.x * axis.y - axis.z * s,  oc * axis.z * axis.x + axis.y * s,  0.0,
                    oc * axis.x * axis.y + axis.z * s,  oc * axis.y * axis.y + c,           oc * axis.y * axis.z - axis.x * s,  0.0,
                    oc * axis.z * axis.x - axis.y * s,  oc * axis.y * axis.z + axis.x * s,  oc * axis.z * axis.z + c,           0.0,
                    0.0,                                0.0,                                0.0,                                1.0
                  );
                }

                vec3 rotate(vec3 v, vec3 axis, float angle) {
                  return (rotation3d(axis, angle) * vec4(v, 1.0)).xyz;
                }                
            """.trimIndent()
            fragmentTransform = """
                x_fill.rgb = rotate(va_normal.xyz * 0.5 + 0.5,
                  normalize(vec3(1.0)), p_i);                
           """.trimIndent()
        }

        extend(cam)
        extend {
            updateCS.buffer("vb", vb.shaderStorageBufferView())
            updateCS.execute(vb.vertexCount)

            drawer.clear(ColorRGBa.WHEAT.shade(0.2))
            drawer.fill = ColorRGBa.WHITE
            drawer.shadeStyle = style
            repeat(10) {
                style.parameter("i", it * 0.3)
                drawer.translate(1.0, 0.0, 0.0)
                drawer.rotate(Vector3.UNIT_Z, 5.0)
                drawer.vertexBuffer(vb, DrawPrimitive.TRIANGLES)
            }
        }
    }
}
