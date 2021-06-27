import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.dnk3.dsl.*
import org.openrndr.extra.dnk3.renderers.dryRenderer
import org.openrndr.extra.dnk3.tools.addSkybox
import org.openrndr.extra.noise.simplex
import org.openrndr.extras.camera.Orbital
import org.openrndr.extras.meshgenerators.groundPlaneMesh
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.transform
import org.openrndr.shape.path3D

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

            root.node {
                pathMesh {
                    weight = 10.0
                    material = pbrMaterial {
                        color = ColorRGBa.PINK
                    }
                    update {
                        paths = mutableListOf(
                                path3D {
                                    val t = seconds * 0.1
                                    moveTo(Vector3.ZERO)
                                    val control = Vector3.simplex(3032, t).let { it.copy(y = it.y * 0.5 + 0.5) } * 4.0
                                    val target = Vector3.simplex(5077, t).let { it.copy(y = it.y * 0.5 + 0.5) } * 4.0
                                    val end = Vector3.simplex(9041, t).let { it.copy(y = it.y * 0.5 + 0.5) } * 4.0
                                    curveTo(control, target)
                                    continueTo(end)
                                }
                        )
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