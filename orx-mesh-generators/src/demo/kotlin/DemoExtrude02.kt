import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.CullTestPass
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.meshgenerators.*
import org.openrndr.math.Vector3
import org.openrndr.math.catmullRom
import org.openrndr.shape.Circle
import org.openrndr.shape.Shape
import org.openrndr.shape.toPath3D

fun main() {
    application {
        configure {
            width = 800
            height = 800
            multisample = WindowMultisample.SampleCount(8)
        }
        program {
            val m = buildTriangleMesh {
                color = ColorRGBa.PINK

                val path = listOf(
                    Vector3(0.0, 0.0, 0.0),
                    Vector3(-2.0, 2.0, 2.0),
                    Vector3(2.0, -4.0, 4.0),
                    Vector3(0.0, 0.0, 8.0)
                ).catmullRom(0.5, closed = false).toPath3D()


                translate(-5.0, 0.0, 0.0)


                val ring = Shape(listOf(Circle(0.0, 0.0, 0.5).contour, Circle(0.0, 0.0, 0.25).contour.reversed))

                for (i in 0 until 5) {
                    extrudeShapeSteps(
                        ring,
                        path,
                        160,
                        Vector3.UNIT_Y,
                        contourDistanceTolerance = 0.02,
                        pathDistanceTolerance = 0.001
                    )
                    translate(2.0, 0.0, 0.0)
                }


            }

            extend(Orbital()) {
                this.eye = Vector3(0.0, 3.0, 7.0)
                this.lookAt = Vector3(0.0, 2.0, 0.0)
            }

            extend {
                drawer.shadeStyle = shadeStyle {
                    fragmentTransform = """
                        x_fill = va_color;
                        x_fill.rgb *= v_viewNormal.z;
                    """.trimIndent()
                }

                drawer.vertexBuffer(m, DrawPrimitive.TRIANGLES)
            }
        }
    }
}