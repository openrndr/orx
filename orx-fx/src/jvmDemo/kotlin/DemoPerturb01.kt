import org.openrndr.application
import org.openrndr.draw.WrapMode
import org.openrndr.draw.createEquivalent
import org.openrndr.draw.loadImage
import org.openrndr.extra.fx.distort.Perturb
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.math.Vector3

/**
 * Demonstrates how to use the [Perturb] effect to distort
 * a `colorBuffer`, in this case, an image loaded from disk.
 *
 * Notice the use of `createEquivalent()`, which creates a new `colorBuffer`
 * with the same size and properties as a source `colorBuffer`.
 *
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val image = loadImage("demo-data/images/image-001.png")
        image.wrapU = WrapMode.MIRRORED_REPEAT // Try other wrap modes
        image.wrapV = WrapMode.MIRRORED_REPEAT
        val result = image.createEquivalent()
        val effect = Perturb()

        mouse.buttonDown.listen {
            effect.clampEdges = !effect.clampEdges
        }
        extend {
            effect.seed = Vector3.UNIT_Z * seconds * 0.1
            effect.apply(image, result)
            drawer.imageFit(result, drawer.bounds)
        }
    }
}