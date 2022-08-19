import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.meshgenerators.*
import org.openrndr.math.Vector3

fun main() {
    application {
        configure {
            width = 800
            height = 800
            multisample = WindowMultisample.SampleCount(8)
        }
        program {
            extend(Orbital()) {
                this.eye = Vector3(0.0, 10.0, 20.0)
                this.lookAt = Vector3(0.0, 5.0, 0.0)
            }
            val m = buildTriangleMesh {
                isolated {
                    translate(0.0, 12.0, 0.0)
                    hemisphere(32, 16, 5.0)
                }

                isolated {
                    translate(0.0, 9.0, 0.0)
                    rotate(Vector3.UNIT_X, 90.0)
                    cylinder(32, 1, 5.0, 6.0, center = true)
                }
                isolated {
                    translate(0.0, 6.0, 0.0)
                    rotate(Vector3.UNIT_X, 180.0)
                    hemisphere(32, 16, 5.0)
                }
                isolated {
                    val legCount = 12
                    val baseRadius = 3.0
                    val legRadius = 0.05
                    val legLength = 4.0
                    for (i in 0 until legCount) {
                        isolated {
                            val dphi = 360.0 / legCount
                            rotate(Vector3.UNIT_Y, dphi * i)
                            translate(baseRadius, 0.0, 0.0)
                            rotate(Vector3.UNIT_Z, -15.0)
                            translate(0.0, legLength / 2.0, 0.0)
                            rotate(Vector3.UNIT_X, 90.0)
                            cylinder(32, 1, legRadius, legLength, center = true)
                        }
                    }
                }
            }
            extend {
                drawer.shadeStyle = shadeStyle {
                    fragmentTransform = """
                        x_fill.rgb *= v_viewNormal.z;
                    """.trimIndent()
                }
                drawer.vertexBuffer(m, DrawPrimitive.TRIANGLES)
            }
        }
    }
}