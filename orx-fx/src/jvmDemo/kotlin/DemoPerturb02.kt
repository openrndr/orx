import org.openrndr.application
import org.openrndr.draw.WrapMode
import org.openrndr.draw.createEquivalent
import org.openrndr.draw.loadImage
import org.openrndr.extra.fx.distort.Perturb
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.extra.shadestyles.presets.ChainWarp
import org.openrndr.extra.shadestyles.presets.NoiseWarp
import org.openrndr.extra.shadestyles.presets.Ripple
import org.openrndr.extra.shadestyles.presets.Swirl

/**
 * Demonstrates replacing the warp preset of [Perturb]. A warp preset provides a GLSL function
 * `vec2 warp(vec2 uv)` that maps a coordinate to the coordinate to read from, and has annotated
 * properties that the GUI shows as a group inside the Perturb compartment.
 *
 * Press space to cycle between a noise warp, a ripple warp, a swirl warp and a chain of a ripple and a
 * swirl; the GUI replaces the controls of the warp group. Every class of warp compiles a shader the
 * first time it is used; changing the properties of a warp only updates uniforms.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val image = loadImage("demo-data/images/image-001.png")
        image.wrapU = WrapMode.MIRRORED_REPEAT
        image.wrapV = WrapMode.MIRRORED_REPEAT
        val result = image.createEquivalent()
        val effect = Perturb()

        val warps = listOf(NoiseWarp(), Ripple(), Swirl(), ChainWarp(Ripple(), Swirl()))
        var current = 0
        effect.warp = warps[current]

        keyboard.keyDown.listen {
            if (it.name == "spacebar") {
                current = (current + 1) % warps.size
                effect.warp = warps[current]
            }
        }

        val gui = GUI()
        gui.compartmentsCollapsedByDefault = false
        gui.add(effect)
        extend(gui)
        extend {
            effect.apply(image, result)
            drawer.imageFit(result, drawer.bounds)
        }
    }
}
