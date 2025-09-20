package image

import org.openrndr.application
import org.openrndr.draw.loadImage
import org.openrndr.extra.shadestyles.fills.image.imageFill

/**
 * A minimal demonstration of the `imageFill` shade style, used to texture
 * shapes using a loaded image (or generated color buffer).
 *
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val img = loadImage("demo-data/images/image-001.png")
        extend {
            drawer.shadeStyle = imageFill {
                image = img
            }
            drawer.circle(drawer.bounds.center, 200.0)
        }
    }
}

