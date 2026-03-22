import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.color.mixing.mixSpectral
import org.openrndr.extra.color.spaces.OKHSV
import org.openrndr.extra.color.spaces.OKLab
import org.openrndr.extra.color.tools.saturate
import org.openrndr.extra.color.tools.shadeLuminosity
import org.openrndr.extra.color.tools.shiftHue

/**
 * Demonstrates the use of `mixSpectral()` to blend two colors spectrally by interpolating their reflectance spectra
 * and returning the resulting color. This method uses spectral upsampling, Saunderson correction, and concentration
 * factors to compute the resulting color in the RGB color space.
 *
 * A plain `mix()` of the same two colors is shown at the bottom for comparison.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        extend {
            drawer.stroke = null

            val a = ColorRGBa.BLUE.shiftHue<OKHSV>(0.0).saturate<OKHSV>(0.3).shadeLuminosity<OKLab>(0.3)
            val b = ColorRGBa.BLUE.shiftHue<OKHSV>(60.0).saturate<OKHSV>(0.8)

            for (i in 0 until 60) {
                val c1 = mixSpectral(a, b, i / 59.0, 0.0, 0.0).toSRGB()
                drawer.fill = c1
                drawer.rectangle(i * width / 60.0, 0.0, width / 60.0, 0.5 * height)

                val c2 = a.mix(b, i / 59.0)
                drawer.fill = c2
                drawer.rectangle(i * width / 60.0, 0.5 * height, width / 60.0, 0.5 * height)
            }
        }
    }
}
