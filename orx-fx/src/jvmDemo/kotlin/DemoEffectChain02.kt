import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.colorBuffer
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.shadestyles.presets.*
import org.openrndr.math.Vector3

/**
 * Demonstrates an [EffectChain] that generates its image: a noise source, read through a
 * kaleidoscope warp, mapped to a colormap, with a checkerboard multiplied over it and a vignette.
 * All stages are fused into a single pass; the input of the chain is not read.
 *
 * Stages apply in order, so the kaleidoscope after the noise source warps the coordinates the
 * noise is read at, and the checkerboard layer, which comes after the warp, is not warped.
 */
fun main() = application {
    configure {
        width = 960
        height = 640
    }
    program {
        val noise = NoiseSource().apply { scale = 3.0 }
        val kaleidoscope = Kaleidoscope().apply { segments = 8 }
        val colormap = Colormap().apply { colormap = ColormapType.SPECTRAL }
        val checkers = CheckersSource().apply { even = ColorRGBa.WHITE; odd = ColorRGBa.GRAY.shade(0.8); xCells = 12; yCells = 8 }
        val layer = Layer(checkers, LayerBlend(LayerBlendMode.MULTIPLY))
        val vignette = Vignette()

        val chain = effectChain {
            source(noise)
            warp(kaleidoscope)
            color(colormap)
            color(layer)
            color(vignette)
        }
        val unused = colorBuffer(width, height)
        val result = colorBuffer(width, height)

        val gui = GUI()
        gui.compartmentsCollapsedByDefault = false
        gui.add(noise, "noise")
        gui.add(kaleidoscope, "kaleidoscope")
        gui.add(colormap, "colormap")
        gui.add(layer, "layer")
        gui.add(vignette, "vignette")

        extend(gui)
        extend {
            noise.offset = Vector3(0.0, 0.0, seconds * 0.1)
            kaleidoscope.rotation = seconds * 10.0
            chain.apply(unused, result)
            drawer.image(result)
        }
    }
}
