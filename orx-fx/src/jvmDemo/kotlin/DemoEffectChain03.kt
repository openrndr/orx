import org.openrndr.application
import org.openrndr.draw.createEquivalent
import org.openrndr.draw.loadImage
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.extra.shadestyles.presets.*

/**
 * Demonstrates sample stages, masks and film grain in an [EffectChain]. The chromatic aberration is a
 * sampler: it reads the image around every pixel, so it starts a pass and reads the input there. The
 * grade is applied through a mask, a desaturated and darker grade outside a soft circle, and the film
 * grain is aligned to the output pixels. Everything runs in a single pass.
 */
fun main() = application {
    configure {
        width = 960
        height = 640
    }
    program {
        val image = loadImage("demo-data/images/image-001.png")
        val result = image.createEquivalent()

        val aberration = ChromaticAberration().apply { amount = 6.0 }
        val mask = InvertMask(RadialGradientMask().apply { radius = 0.25; softness = 0.3 })
        val masked = Masked(Grade().apply { saturation = 0.2; exposure = -0.7 }, mask)
        val grain = FilmGrain().apply { rate = 24.0 }

        val chain = effectChain {
            sample(aberration)
            color(masked)
            color(grain)
        }

        val gui = GUI()
        gui.compartmentsCollapsedByDefault = false
        gui.add(aberration, "aberration")
        gui.add(masked, "masked grade")
        gui.add(grain, "grain")

        extend(gui)
        extend {
            grain.time = seconds
            chain.apply(image, result)
            drawer.imageFit(result, drawer.bounds)
        }
    }
}
