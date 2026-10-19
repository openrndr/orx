import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.color.presets.BLUE_STEEL
import org.openrndr.extra.color.presets.LIGHT_CORAL
import org.openrndr.extra.color.presets.NAVAJO_WHITE
import org.openrndr.extra.dnk3.Shadows
import org.openrndr.extra.dnk3.dsl.*
import org.openrndr.extra.dnk3.renderers.dryRenderer
import org.openrndr.extra.meshgenerators.boxMesh
import org.openrndr.extra.meshgenerators.groundPlaneMesh
import org.openrndr.extra.noise.simplex
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.transform

/**
 * This demo includes some tips on how to set up spotlights to produce shadows.
 * The main idea is to not set a `direction` for them, but use a transformation
 * with `tranlate` and `rotate` instead.
 */
fun main() = application {
    configure {
        width = 1280
        height = 720
    }

    program {
        val renderer = dryRenderer()
        val scene = scene {
            root.hemisphereLight {
                upColor = ColorRGBa.NAVAJO_WHITE.shade(0.3)
                downColor = ColorRGBa.PINK.shade(0.1)
            }

            root.node {
                transform = transform {
                    translate(0.0, 40.0, 15.0)
                    rotate(Vector3.UNIT_X, -60.0)
                }
                spotLight {
                    innerAngle = 5.0
                    outerAngle = 25.0
                    // do not set direction. Instead, apply a `rotate` transformation
                    //direction = -Vector3.UNIT_Y
                    shadows = Shadows.PCF(2048)
                }
            }

            root.node {
                transform = transform {
                    translate(0.0, 40.0, -15.0)
                    rotate(Vector3.UNIT_X, -120.0)
                }
                directionalLight {
                    // do not set direction. Instead, apply a `rotate` transformation
                    //direction = -Vector3.UNIT_Y
                    shadows = Shadows.PCF(2048)
                }
            }

            root.node {
                simpleMesh {
                    vertexBuffer = groundPlaneMesh(100.0, 100.0)
                    material = pbrMaterial {
                        color = ColorRGBa.LIGHT_CORAL
                    }
                }
            }

            val mat = pbrMaterial {
                doubleSided = true
                color = ColorRGBa.PINK
            }
            val cube = boxMesh()
            for (j in -3..3) {
                for (i in -3..3) {
                    root.node {
                        update {
                            val y = 4.0 + 3.0 * simplex(111, i * 0.5, j * 0.5, seconds * 0.2)
                            transform = transform {
                                translate(i * 1.0, y, j * 1.0)
                                rotate(Vector3.UNIT_Z, seconds * 45.0 + i * 20.0 + j * 50.0)
                            }
                        }
                        simpleMesh {
                            vertexBuffer = cube
                            material = mat
                        }
                    }
                }
            }
        }
        extend(Orbital()) {
            eye = Vector3(2.0, 4.0, 6.0)
        }
        extend {
            drawer.clear(ColorRGBa.BLUE_STEEL)
            renderer.draw(drawer, scene)
        }
    }
}