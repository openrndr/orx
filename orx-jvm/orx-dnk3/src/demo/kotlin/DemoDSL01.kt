import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.dnk3.dsl.*
import org.openrndr.extra.dnk3.renderers.dryRenderer
import org.openrndr.extra.dnk3.tools.addSkybox
import org.openrndr.extras.camera.Orbital
import org.openrndr.extras.meshgenerators.boxMesh
import org.openrndr.extras.meshgenerators.groundPlaneMesh
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.transform

suspend fun main() = application {
    configure {
        width = 1280
        height = 720
    }

    program {
        if (System.getProperty("takeScreenshot") == "true") {
            extend(SingleScreenshot()) {
                this.outputFile = System.getProperty("screenshotPath")
            }
        }
        extend(Orbital()) {
            eye = Vector3(4.0, 4.0, 4.0)
        }

        val renderer = dryRenderer()
        val scene = scene {

            addSkybox("file:demo-data/cubemaps/garage_iem.dds")

            root.hemisphereLight {
                upColor = ColorRGBa.WHITE.shade(0.1)
                downColor = ColorRGBa.BLACK
            }

            root.node {
                transform = transform {
                    translate(0.0, 2.0, 0.0)
                }

                pointLight {
                    constantAttenuation = 0.0
                    quadraticAttenuation = 1.0
                }
            }

            root.node {
                simpleMesh {
                    vertexBuffer = groundPlaneMesh(100.0, 100.0)
                    material = pbrMaterial {
                        color = ColorRGBa.GREEN
                    }
                }
            }

            for (j in -3..3) {
                for (i in -3..3) {
                    root.node {
                        transform = transform {
                            translate(i * 2.0, 1.0, j * 2.0)
                        }
                        update {
                            transform = transform {
                                translate(i * 2.0, 1.0, j * 2.0)
                                rotate(Vector3.UNIT_Z, seconds* 45.0 + i * 20.0 + j * 50.0)
                            }
                        }
                        simpleMesh {
                            vertexBuffer = boxMesh()
                            material = pbrMaterial {
                                color = ColorRGBa.WHITE
                            }
                        }
                    }
                }
            }
        }
        extend {
            drawer.clear(ColorRGBa.BLACK)
            renderer.draw(drawer, scene)
        }
    }
}