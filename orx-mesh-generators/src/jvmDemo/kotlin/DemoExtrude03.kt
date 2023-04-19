import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.meshgenerators.buildTriangleMesh
import org.openrndr.extra.meshgenerators.extrudeContourAdaptive
import org.openrndr.math.Polar
import org.openrndr.math.Vector3
import org.openrndr.math.asDegrees
import org.openrndr.math.asRadians
import org.openrndr.shape.Circle
import org.openrndr.shape.Path3D
import kotlin.math.PI
import kotlin.math.exp

fun main() {
    application {
        configure {
            width = 800
            height = 800
            multisample = WindowMultisample.SampleCount(8)
        }
        program {
            fun spiralPath(a: Double, k: Double, cycles: Double, steps: Int, direction:Double = 1.0): Path3D {
                val points = (0 until steps).map {

                    val theta = ((PI * 2.0 * cycles) / steps) * it
                    val radius = a * exp(k * theta)

                    val c = Polar(theta.asDegrees, radius).cartesian
                    c.xy0
                }
                return Path3D.fromPoints(points, false)
            }

            val spiral = buildTriangleMesh {
                for (i in -1..1 step 2) {
                    val p = spiralPath(0.2 * i, 0.25, 4.0, 400)

                    extrudeContourAdaptive(
                        Circle(0.0, 0.0, 0.1).contour,
                        p,
                        Vector3.UNIT_Z,
                        contourDistanceTolerance = 0.02,
                        pathDistanceTolerance = 0.001
                    )
                }

                isolated {
                    color = ColorRGBa.YELLOW
                    rotate(Vector3.UNIT_X, 90.0)

                    //rotate(Vector3.UNIT_Y, 45.0)
                    for (j in 0 until 1) {
                        for (i in -1..1 step 2) {

                            val rotationDegrees = j * 180.0 / 1.0
                            val rotation = rotationDegrees.asRadians
                            val scale = exp(rotation * 0.25)

                            val p = spiralPath(0.2 * i * scale, 0.25, 4.0, 400)

                            extrudeContourAdaptive(
                                Circle(0.0, 0.0, 0.1).contour,
                                p,
                                Vector3.UNIT_Z,
                                contourDistanceTolerance = 0.02,
                                pathDistanceTolerance = 0.001
                            )
                        }
                        rotate(Vector3.UNIT_Y, 180.0 / 1.0)
                    }
                }



            }

            extend(Orbital())
            extend {
                drawer.shadeStyle = shadeStyle {
                    fragmentTransform = """
                        x_fill = va_color;
                        x_fill.rgb *= v_viewNormal.z;
                    """.trimIndent()
                }

                drawer.rotate(Vector3.UNIT_X, seconds*20.0)
                drawer.vertexBuffer(spiral, DrawPrimitive.TRIANGLES)

            }
        }
    }
}