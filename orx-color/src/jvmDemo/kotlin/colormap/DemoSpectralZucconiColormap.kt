package colormap

import org.openrndr.application
import org.openrndr.extra.color.colormaps.spectralZucconi6
import org.openrndr.extra.noise.fastFloor
import kotlin.math.sin

/**
 * This program demonstrates the `spectralZucconi6()` function, which
 * takes a normalized value and returns a `ColorRGBa` using the
 * accurate spectral colormap developed by Alan Zucconi.
 *
 * It draws a varying number of vertical bands (between 16 and 48)
 * filled with various hues.
 */
fun main() = application {
    configure {
        width = 720
        height = 360
    }
    program {
        extend {
            drawer.stroke = null
            val stripeCount = 32 + (sin(seconds) * 16.0).fastFloor()
            val bandWidth = width / stripeCount.toDouble()
            repeat(stripeCount) { i ->
                drawer.fill = spectralZucconi6(i / stripeCount.toDouble())
                drawer.rectangle(
                    x = i * bandWidth,
                    y = 0.0,
                    width = bandWidth,
                    height = height.toDouble(),
                )
            }
        }
    }
}
