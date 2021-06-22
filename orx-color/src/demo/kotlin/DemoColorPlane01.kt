import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extras.camera.Orbital
import org.openrndr.extras.color.spaces.ColorOKLCHa
import org.openrndr.extras.meshgenerators.sphereMesh
import org.openrndr.math.Vector3
import kotlin.math.cos

suspend fun main() {
    application {
        configure {
            width = 800
            height = 800

        }
        program {
            // -- this block is for automation purposes only
            if (System.getProperty("takeScreenshot") == "true") {
                extend(SingleScreenshot()) {
                    this.outputFile = System.getProperty("screenshotPath")
                }
            }

            val mesh = sphereMesh(8, 8, radius = 0.1)

            val instanceData = vertexBuffer(
                vertexFormat {
                    attribute("instanceColor", VertexElementType.VECTOR4_FLOAT32)
                    attribute("instancePosition", VertexElementType.VECTOR3_FLOAT32)
                },
                90 * 100
            )


            extend(Orbital())

            extend {
                drawer.clear(ColorRGBa.WHITE)

                drawer.stroke = null

                drawer.fontMap = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 16.0)

                instanceData.put {
                    for (hue in 0 until 360 step 4) {
                        for (chroma in 0 until 100 step 1) {
                            val lch = ColorOKLCHa(cos(seconds * 0.1) * 0.5 + 0.5, chroma / 100.0, hue.toDouble())
                            val srgb = lch.toRGBa().toSRGB().saturated
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
}