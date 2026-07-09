import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.color.presets.ORANGE
import org.openrndr.extra.dnk3.dsl.*
import org.openrndr.extra.dnk3.renderers.dryRenderer
import org.openrndr.extra.dnk3.tools.addSkybox
import org.openrndr.extra.meshgenerators.groundPlaneMesh
import org.openrndr.extra.meshgenerators.sphereMesh
import org.openrndr.extra.noise.simplex
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.transform
import org.openrndr.shape.path3D
import java.io.File

fun main() = application {
    configure {
        width = 1280
        height = 720
    }

    program {
        extend(Orbital()) {
            eye = Vector3(4.0, 4.0, 4.0)
        }

        val renderer = dryRenderer()
        val sphere = sphereMesh(32, 32, radius = 1.0)
        val scene = scene {
            addSkybox(File("demo-data/cubemaps/garage_iem.dds").absolutePath)

            root.hemisphereLight {
                upColor = ColorRGBa.WHITE.shade(0.1)
                downColor = ColorRGBa.BLACK
            }

            root.node {
                transform = transform {
                    translate(0.0, 10.0, 0.0)
                }

                pointLight {
                    constantAttenuation = 0.0
                    quadraticAttenuation = 0.3
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
                val mat = pbrMaterial {
                    metalness = 0.8
                    roughness = 0.2
                    color = ColorRGBa.ORANGE
                }
                simpleMesh {
                    vertexBuffer = sphere
                    material = mat
                }
                pathMesh {
                    weight = 50.0
                    material = mat
                    update {
                        paths = mutableListOf(
                            path3D {
                                val t = seconds * 0.3

                                // simplex returns values between -1.0 and 1.0. Make sure the y component
                                // (the elevation) is between 0.0 and 4.0
                                val control = Vector3.simplex(3032, t).let {
                                    it.copy(y = it.y * 0.5 + 0.5)
                                } * 4.0
                                val target = Vector3.simplex(5077, t).let {
                                    it.copy(y = it.y * 0.5 + 0.5)
                                } * 4.0
                                val end = Vector3.simplex(9041, t).let {
                                    it.copy(y = it.y * 0.5 + 0.5)
                                } * 4.0

                                moveTo(Vector3.ZERO)
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