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
                    whiteNoise {
                        bilinear()
                    }
                    blendFunction = """vec4 blend(vec4 o, float n) { 
                        |   float luma = dot(o.rgb, vec3(1.0/3.0));
                        |   return vec4(vec3(smoothstep(luma+0.01, luma-0.01, n)), 1.0);
                        |}""".trimMargin()
                }
                drawer.imageFit(image, drawer.bounds)
            }
        }
    }
}

