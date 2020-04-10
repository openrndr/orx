import org.openrndr.application
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.shadeStyle
import org.openrndr.extras.camera.Orbital
import org.openrndr.extras.meshgenerators.*
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.transform

fun main() {
    application {
        program {
            extend(Orbital())
            val m = meshGenerator {
                group {
                    hemisphere(32, 16, 5.0)
                    transform(transform {
                        translate(0.0, 12.0, 0.0)
                    })
                }
                group {
                    cylinder(32, 1, 5.0, 6.0)
                    transform(transform {
                        translate(0.0, 9.0, 0.0)
                        rotate(Vector3.UNIT_X, 90.0)
                    })
                }
                group {
                    hemisphere(32, 16, 5.0)
                    transform(transform {
                        translate(0.0, 6.0, 0.0)
                        rotate(Vector3.UNIT_X, 180.0)
                    })
                }
                group {
                    val legCount = 12
                    val baseRadius = 3.0
                    val legRadius = 0.05
                    val legLength = 4.0
                    for (i in 0 until legCount) {
                        group {
                            val dphi = 360.0 / legCount
                            cylinder(32, 1, legRadius, legLength)
                            transform(transform {
                                rotate(Vector3.UNIT_Y, dphi * i)
                                translate(baseRadius, 0.0, 0.0)
                                rotate(Vector3.UNIT_Z, -15.0)
                                translate(0.0, legLength/2.0, 0.0)
                                rotate(Vector3.UNIT_X, 90.0)
                            })
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