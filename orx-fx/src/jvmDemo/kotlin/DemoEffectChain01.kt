import org.openrndr.application
import org.openrndr.draw.createEquivalent
import org.openrndr.draw.loadImage
import org.openrndr.extra.fx.blur.HashBlur
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.extra.shadestyles.presets.*

/**
 * Demonstrates an [EffectChain]: warp, color and filter stages applied in order. The ripple and the
 * grade are fused into one pass, the hash blur is a pass of its own, the tint and the vignette are
 * fused into a third pass.
 *
 * Every stage is added to the GUI; press space to toggle between the chain and the image.
 */
fun main() = application {
    configure {
        width = 960
        height = 640
    }
    program {
        val image = loadImage("demo-data/images/image-001.png")
        val result = image.createEquivalent()

        val ripple = Ripple()
        val grade = Grade().apply { contrast = 1.2; saturation = 1.3 }
        val blur = HashBlur().apply { radius = 2.0 }
        val tint = Tint().apply { amount = 0.5 }
        val vignette = Vignette()

        val chain = effectChain {
            warp(ripple)
            color(grade)
            filter(blur)
            color(tint)
            color(vignette)
        }

        val gui = GUI()
        gui.compartmentsCollapsedByDefault = false
        gui.add(ripple, "ripple")
        gui.add(grade, "grade")
        gui.add(blur, "blur")
        gui.add(tint, "tint")
        gui.add(vignette, "vignette")

        var enabled = true
        keyboard.keyDown.listen {
            if (it.name == "spacebar") enabled = !enabled
        }

        extend(gui)
        extend {
            ripple.phase = seconds * 2.0
            blur.time = seconds
            if (enabled) {
                chain.apply(image, result)
                drawer.imageFit(result, drawer.bounds)
            } else {
                drawer.imageFit(image, drawer.bounds)
            }
        }
    }
}
