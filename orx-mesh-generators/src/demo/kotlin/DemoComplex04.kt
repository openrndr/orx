import org.openrndr.application
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.shadeStyle
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extras.camera.Orbital
import org.openrndr.extras.meshgenerators.*
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.transform

fun main() {
    application {
        program {
            if (System.getProperty("takeScreenshot") == "true") {
                extend(SingleScreenshot()) {
                    this.outputFile = System.getProperty("screenshotPath")
                }
            }

            extend(Orbital()) {
                this.eye = Vector3(0.0, 15.0, 15.0)
            }

            val m = meshGenerator {
                val sides = 12
                group {
                    cap(sides, 5.0, listOf(
                            Vector2(0.0, 1.0),
                            Vector2(0.5, 1.0),
                            Vector2(0.5, 0.5),
                            Vector2(0.9, 0.5),
                            Vector2(1.0, 0.0))
                    )
                    transform(transform {
                        translate(0.0, 12.0, 0.0)
                    })
                }

                val ridges = 5
                val midLength = 6.0
                val ridgeLength = midLength / ridges
                val ridgeRadius = 5.5


                for (r in 0 until ridges) {
                    group {
                        taperedCylinder(sides, 1, 5.0, ridgeRadius, ridgeLength / 3.0)
                        transform(transform {
                            translate(
                                    0.0,
                                    ridgeLength / 6.0 + r * ridgeLength + 6.0,
                                    0.0
                            )
                            rotate(Vector3.UNIT_X, 270.0)
                        })
                    }
                    group {
                        taperedCylinder(sides, 1, ridgeRadius, ridgeRadius, ridgeLength / 3.0)
                        transform(transform {
                            translate(
                                    0.0,
                                    ridgeLength / 6.0 + ridgeLength / 3.0 + r * ridgeLength + 6.0,
                                    0.0
                            )
                            rotate(Vector3.UNIT_X, 270.0)
                        })
                    }

                    group {
                        taperedCylinder(sides, 1, ridgeRadius, 5.0, ridgeLength / 3.0)
                        transform(transform {
                            translate(
                                    0.0,
                                    ridgeLength / 6.0 + 2 * ridgeLength / 3.0 + r * ridgeLength + 6.0,
                                    0.0
                            )
                            rotate(Vector3.UNIT_X, 270.0)
                        })
                    }
                }
                group {
                    cap(sides, 5.0, listOf(Vector2(0.0, 0.0), Vector2(1.0, 0.0)))
                    transform(transform {
                        translate(0.0, 6.0, 0.0)
                        rotate(Vector3.UNIT_X, 180.0)
                    })
                }
                group {
                    val legCount = 12
                    val baseRadius = 4.5
                    val legRadius = 0.05
                    val legLength = 7.0
                    for (i in 0 until legCount) {
                        group {
                            val dphi = 360.0 / legCount
                            cylinder(sides, 1, legRadius, legLength)
                            transform(transform {
                                rotate(Vector3.UNIT_Y, dphi * i)
                                translate(baseRadius, 0.0, 0.0)
                                //rotate(Vector3.UNIT_Z, -15.0)
                                translate(0.0, legLength / 2.0, 0.0)
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