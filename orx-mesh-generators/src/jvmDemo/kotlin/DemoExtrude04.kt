import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.meshgenerators.buildTriangleMesh
import org.openrndr.extra.meshgenerators.extrudeContourSteps
import org.openrndr.extra.noise.Random
import org.openrndr.math.Vector3
import org.openrndr.shape.Circle
import org.openrndr.shape.Path3D
import org.openrndr.shape.Segment3D

/**
 * Extruded Bézier tubes grown on a morphing Bézier surface.
 *
 */
fun main() = application {
    configure {
        width = 720
        height = 720
        multisample = WindowMultisample.SampleCount(8)
    }
    program {
        val crossSection = Circle(0.0, 0.0, 0.2).contour

        extend(Orbital()) {
            this.eye = Vector3(0.0, 3.0, 7.0)
            this.lookAt = Vector3(0.0, 0.0, 0.0)
        }

        extend {
            drawer.shadeStyle = shadeStyle {
                fragmentTransform = """
                        x_fill = va_color;
                        x_fill.rgb *= v_viewNormal.z;
                    """.trimIndent()
            }

            val m = buildTriangleMesh {
                val beziers = List(4) { curveId ->
                    val n = List(12) {
                        Random.simplex(it * 7.387, curveId * 5.531 + seconds * 0.05) * 10.0
                    }
                    Segment3D(
                        Vector3(n[0], n[1], n[2]),
                        Vector3(n[3], n[4], n[5]),
                        Vector3(n[6], n[7], n[8]),
                        Vector3(n[9], n[10], n[11])
                    )
                }

                for (i in 0 until 20) {
                    val t = i / (20.0 - 1.0)
                    val path = Path3D(
                        listOf(
                            Segment3D(
                                beziers[0].position(t),
                                beziers[1].position(t),
                                beziers[2].position(t),
                                beziers[3].position(t)
                            )
                        ), false
                    )
                    color = if (i % 2 == 0) ColorRGBa.PINK else ColorRGBa.WHITE.shade(0.1)
                    extrudeContourSteps(
                        crossSection,
                        path,
                        120,
                        Vector3.UNIT_Y,
                        contourDistanceTolerance = 0.05,
                        pathDistanceTolerance = 0.05
                    )
                }
            }

            drawer.vertexBuffer(m, DrawPrimitive.TRIANGLES)

            // Remember to free the memory! Otherwise, the computer will quickly run out of RAM.
            m.destroy()
        }
    }
}
