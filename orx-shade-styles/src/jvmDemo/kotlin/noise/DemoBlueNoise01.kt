package noise

import org.openrndr.application
import org.openrndr.draw.loadImage
import org.openrndr.extra.camera.Camera2D
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.shaderphrases.noise.simplex13
import org.openrndr.extra.shadestyles.fills.noise.noise
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.transform
import kotlin.math.cos
import kotlin.reflect.KMutableProperty0

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val image = loadImage("demo-data/images/image-001.png")
            extend(Camera2D())
            extend {
                drawer.shadeStyle = noise {
                    phase = seconds * 10.0

                    filterWindow = 5
                    domainWarpFunction =
                        """$simplex13
                            vec3 domainWarp(vec3 p) { float px = simplex13(p*0.01); float py = simplex13(p.yxz*-0.01); return p + 10.25 * vec3(px, py, 0.0); }""".trimIndent()

                    blueNoise {
                        bits = 17
                        bilinear()
                    }

                    blendFunction = """vec4 blend(vec4 o, float n) { float luma = dot(o.rgb, vec3(1.0/3.0));
                        |return vec4(vec3(smoothstep(luma+0.01, luma-0.01, n)), 1.0);
                        |}""".trimMargin()

                }
                drawer.imageFit(image, drawer.bounds)
            }
        }
    }
}

