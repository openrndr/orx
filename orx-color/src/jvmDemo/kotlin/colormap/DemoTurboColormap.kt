package colormap

import org.openrndr.application
import org.openrndr.extra.color.colormaps.turboColormap
import org.openrndr.extra.noise.fastFloor
import kotlin.math.sin

/**
 * This program demonstrates the `turboColormap()` function, which
 * takes a normalized value and returns a `ColorRGBa` using the
 * Turbo colormap developed by Google.
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
            repeat(stripeCount) { i ->
                drawer.fill = turboColormap(i / stripeCount.toDouble())
                drawer.rectangle(
                    x = i * width / stripeCount.toDouble(),
                    y = 0.0,
                    width = width / stripeCount.toDouble(),
                    height = height.toDouble(),
                )
            }
        }
    }
}
