import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.color.spaces.ColorOKLCHa
import org.openrndr.extra.meshgenerators.sphereMesh
import org.openrndr.math.Vector3
import kotlin.math.cos

/**
 * Visualizes a plane of ColorOKLCH colors as small 3D spheres
 * inside a 3D box. The plane represents all available hues and chromas.
 * The luminosity used to create the colors is modulated over time
 * with a slow sine wave.
 * Instanced rendering is used to render 90 x 100 colored spheres,
 * each with a unique position based on the RGB components of the color.
 *
 * Since the OKLCH color space is larger than the RGB space, some
 * spheres would be outside the 3D box, but they are
 * actually clipped to the walls.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val mesh = sphereMesh(8, 8, radius = 0.1)

        val instanceData = vertexBuffer(
            vertexFormat {
                attribute("instanceColor", VertexElementType.VECTOR4_FLOAT32)
                attribute("instancePosition", VertexElementType.VECTOR3_FLOAT32)
            },
            90 * 100
        )
        println(extensions.size)
        extend(Orbital())

        extend {
            drawer.clear(ColorRGBa.WHITE)
            drawer.stroke = null
            drawer.fontMap = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 16.0)

            instanceData.put {
                for (hue in 0 until 360 step 4) {
                    for (chroma in 0 until 100 step 1) {
                        val lch = ColorOKLCHa(cos(seconds * 0.1) * 0.5 + 0.5, chroma / 100.0, hue.toDouble())
                        val srgb = lch.toRGBa().toSRGB().clip()
                        write(srgb)
                        write(Vector3((srgb.r - 0.5) * 10.0, (srgb.g - 0.5) * 10.0, (srgb.b - 0.5) * 10.0))
                    }
                }
            }
            drawer.isolated {
                drawer.shadeStyle = shadeStyle {

                    vertexTransform = """
                            x_position += i_instancePosition;
                        """.trimIndent()
                    fragmentTransform = """
                            x_fill = vi_instanceColor;
                        """.trimIndent()
                }

                drawer.vertexBufferInstances(listOf(mesh), listOf(instanceData), DrawPrimitive.TRIANGLES, 90 * 100)
            }

            // Draw the edges of a 3D cube
            drawer.stroke = ColorRGBa.BLACK.opacify(0.25)
            drawer.strokeWeight = 10.0
            drawer.lineSegments(
                listOf(
                    Vector3(-5.0, -5.0, -5.0), Vector3(5.0, -5.0, -5.0),
                    Vector3(-5.0, -5.0, 5.0), Vector3(5.0, -5.0, 5.0),
                    Vector3(-5.0, 5.0, -5.0), Vector3(5.0, 5.0, -5.0),
                    Vector3(-5.0, 5.0, 5.0), Vector3(5.0, 5.0, 5.0),

                    Vector3(-5.0, -5.0, -5.0), Vector3(-5.0, 5.0, -5.0),
                    Vector3(5.0, -5.0, -5.0), Vector3(5.0, 5.0, -5.0),
                    Vector3(-5.0, -5.0, 5.0), Vector3(-5.0, 5.0, 5.0),
                    Vector3(5.0, -5.0, 5.0), Vector3(5.0, 5.0, 5.0),

                    Vector3(-5.0, -5.0, -5.0), Vector3(-5.0, -5.0, 5.0),
                    Vector3(5.0, -5.0, -5.0), Vector3(5.0, -5.0, 5.0),
                    Vector3(-5.0, 5.0, -5.0), Vector3(-5.0, 5.0, 5.0),
                    Vector3(5.0, 5.0, -5.0), Vector3(5.0, 5.0, 5.0),

                    )
            )
        }
    }
}
