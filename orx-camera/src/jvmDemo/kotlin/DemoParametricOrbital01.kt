import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.camera.*
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.addTo
import org.openrndr.extra.meshgenerators.boxMesh

/**
 * Demonstrates the use of a `ParametricOrbital` camera.
 * This 3D camera can't be directly interacted with a mouse or a keyboard,
 * but only via a GUI (or via code).
 *
 * The GUI state is saved when closing the program and loaded
 * when running it again.
 *
 * The GUI also allows randomizing, loading and saving
 * its state to a file via the top buttons it displays.
 */
fun main() = application {
    configure {
        multisample = WindowMultisample.SampleCount(8)
    }

    program {
        val gui = GUI()
        val po = ParametricOrbital()
        po.addTo(gui)
        extend(gui)
        extend(po)

        val bm = boxMesh()
        extend {
            drawer.clear(ColorRGBa.PINK)
            drawer.shadeStyle = shadeStyle {
                fragmentTransform = """
                    vec3 n = normalize(v_viewNormal) * 0.5 + 0.5;
                    x_fill = vec4(n, 1.0); 
                """.trimIndent()
            }
            drawer.vertexBuffer(bm, DrawPrimitive.TRIANGLES)
        }
    }
}
