package image

import org.openrndr.application
import org.openrndr.draw.loadImage
import org.openrndr.extra.shadestyles.fills.image.imageFill
import org.openrndr.math.transforms.transform
import kotlin.math.cos

/**
 * Demonstrates the use of the `imageFill` shade style, applied to 10 concentric
 * circles. The rotation of each circle depends on the cosine of time, with
 * a varying time offset applied per circle, for a fun wavy effect.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val img = loadImage("demo-data/images/image-001.png")
        extend {
            for (i in 0 until 10) {
                drawer.shadeStyle = imageFill {
                    image = img
                    fillTransform = transform {
                        translate(0.5, 0.5)
                        rotate(cos(i * 0.5 + seconds * 10.0) * 10.0)
                        scale(1.0 - i * 0.05)
                        translate(-0.5, -0.5)
                    }
                }
                //drawer.stroke = null
                drawer.circle(drawer.bounds.center, 360.0 - i * 18.0)
            }
        }
    }
}

