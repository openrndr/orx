import org.openrndr.application
import org.openrndr.draw.createEquivalent
import org.openrndr.draw.loadImage
import org.openrndr.extra.fx.blur.ApproximateGaussianBlur
import org.openrndr.extra.imageFit.imageFit

fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val image = loadImage("demo-data/images/image-001.png")
        val blurred = image.createEquivalent()
        val blur = ApproximateGaussianBlur()

        var enableWrap = false

        mouse.buttonDown.listen {
            enableWrap = !enableWrap
        }
        extend {
            blur.wrapU = enableWrap
            blur.wrapV = enableWrap
            blur.sigma = 15.0
            blur.window = 15
            blur.apply(image, blurred)
            drawer.imageFit(blurred, drawer.bounds)
        }
    }
}