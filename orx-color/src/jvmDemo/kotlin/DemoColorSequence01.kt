import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.isolated
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.color.palettes.ColorSequence
import org.openrndr.extra.color.presets.MEDIUM_AQUAMARINE
import org.openrndr.extra.color.presets.ORANGE
import org.openrndr.extra.color.spaces.toOKLABa
import org.openrndr.extra.meshgenerators.sphereMesh
import org.openrndr.math.Vector3

/**
 * A demo that demonstrates 3D objects with custom shading and color gradients.
 *
 * The application setup involves:
 * - Configuring the application window dimensions.
 * - Creating a color gradient using `ColorSequence` and converting it to a `ColorBuffer` for shading purposes.
 * - Defining a 3D sphere mesh with specified resolution.
 *
 * The rendering process includes:
 * - Setting up an orbital camera extension to provide an interactive 3D view.
 * - Applying a custom fragment shader with a palette-based shading style.
 * - Rendering a grid of 3D spheres, each transformed and rotated to create a dynamic pattern.
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val cs = ColorSequence(
                listOf(
                    0.0 to ColorRGBa.PINK,
                    0.25 to ColorRGBa.ORANGE.toOKLABa(),
                    0.27 to ColorRGBa.WHITE.toOKLABa(),
                    0.32 to ColorRGBa.BLUE,
                    1.0 to ColorRGBa.MEDIUM_AQUAMARINE
                )
            )
            val palette = cs.toColorBuffer(drawer, 256, 16)
            val sphere = sphereMesh(sides = 48, segments = 48)

            extend(Orbital()) {
                fov = 50.0
                eye = Vector3(0.0, 0.0, 13.0)
            }
            extend {
                drawer.shadeStyle = shadeStyle {
                    fragmentTransform = """
                       float d = normalize(va_normal).z;
                       x_fill = texture(p_palette, vec2(1.0-d, 0.0));
                   """.trimIndent()
                    parameter("palette", palette)
                }
                for (j in -2..2) {
                    for (i in -2..2) {
                        drawer.isolated {
                            drawer.translate(i * 2.0, j * 2.0, 0.0)
                            drawer.rotate(Vector3.UNIT_Y, j * 30.0)
                            drawer.rotate(Vector3.UNIT_X, i * 30.0)
                            drawer.vertexBuffer(sphere, DrawPrimitive.TRIANGLES)
                        }
                    }
                }
            }
        }
    }
}