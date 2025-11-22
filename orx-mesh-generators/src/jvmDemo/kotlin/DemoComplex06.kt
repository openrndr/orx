import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.rgb
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.meshgenerators.*
import org.openrndr.extra.noise.simplex
import org.openrndr.math.Vector3

/**
 * Generates a grid of grids of 3D boxes using `buildTriangleMesh` and
 * renders them using an interactive orbital camera.
 *
 * The cubes ar colorized using a shade style that sets colors based
 * on vertex positions in space, converting XYZ coordinates into RGB colors.
 *
 */
fun main() = application {
    configure {
        width = 720
        height = 720
        multisample = WindowMultisample.SampleCount(8)
    }
    program {
        extend(Orbital()) {
            this.eye = Vector3(3.0, 3.0, 10.0)
            this.fov = 60.0
        }
        val m = buildTriangleMesh {
            grid(5, 5) { u, v ->
                isolated {
                    grid(3, 3, 3, GridCoordinates.UNIPOLAR) { x, y, z ->
                        val pos0 = Vector3(u, v, 0.0) * 10.0
                        val pos1 = Vector3(x, y, z) * 2.0
                        val pos2 = pos0 + pos1 + Vector3(
                            y * 0.12 + z * 0.3,
                            x * 0.14 + z * 0.15,
                            x * 0.16 + y * 0.17
                        )
                        // Drop some boxes
                        if (simplex(0, pos1 * 0.5 + pos0 * 0.05) > 0) {
                            translate(pos2)
                            color = rgb(x, y, z)
                            box(1.2, 1.2, 1.2)
                        }
                    }
                }
            }
        }

        extend {
            drawer.shadeStyle = shadeStyle {
                fragmentTransform = """
                        x_fill = va_color;
                        vec3 s = sin(v_worldPosition.xyz * 2.5);
                        x_fill.rgb += s * 0.1 - 0.1;
                    """.trimIndent()
            }
            drawer.vertexBuffer(m, DrawPrimitive.TRIANGLES)
        }
    }
}
