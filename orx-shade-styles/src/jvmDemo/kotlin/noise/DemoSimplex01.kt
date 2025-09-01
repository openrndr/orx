package noise

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.camera.Camera2D
import org.openrndr.extra.color.presets.PEACH_PUFF
import org.openrndr.extra.color.spaces.RGB
import org.openrndr.extra.shadestyles.fills.gradients.gradient
import org.openrndr.extra.shadestyles.fills.noise.noise
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.transform
import kotlin.math.cos

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            extend(Camera2D())
            extend {
                drawer.shadeStyle = noise {
                    phase = seconds * 0.01
                    simplex {

                    }
                    domainWarpFunction =
                        """vec3 domainWarp(vec3 p) {  float px = simplex13(p*4.0); float py = simplex13(p.yxz*-4.0); return p + 0.25 * vec3(px, py, px*py); }"""

                    anisotropicFbm {
                        octaves = 10
                        decay = 0.4
                        lacunarity = transform {
                            translate(0.1, cos(seconds) * 0.2, 0.0)
                            rotate(Vector3.UNIT_X, seconds)
                            scale(1.89, 6.32, 2.1)
                            rotate(Vector3.UNIT_X, seconds * 10.0)
                        }
                        warpFactor = cos(seconds) * 0.5 + 0.5
                    }
                } + gradient<RGB> {
                    stops[0.0] = ColorRGBa.PINK
                    stops[0.25] = ColorRGBa.BLACK
                    stops[0.5] = ColorRGBa.CYAN.shade(0.5)
                    stops[0.75] = ColorRGBa.BLACK
                    stops[1.0] = ColorRGBa.PEACH_PUFF
                    luma {

                    }
                }
                drawer.circle(drawer.bounds.center, 300.0)
            }
        }
    }
}

