package gradients

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.extra.color.presets.BLUE_STEEL
import org.openrndr.extra.shadestyles.fills.FillUnits
import org.openrndr.extra.shadestyles.fills.SpreadMethod
import org.openrndr.extra.shadestyles.fills.gradients.gradient
import org.openrndr.extra.shapes.primitives.grid
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        extend {
            val grid = drawer.bounds.grid(2, 2)
            drawer.stroke = null

            for ((index, cell) in grid.flatten().withIndex()) {
                drawer.shadeStyle = gradient<ColorRGBa> {
                    stops[0.0] = ColorRGBa.RED
                    stops[0.5] = ColorRGBa.PINK
                    stops[1.0] = ColorRGBa.WHITE

                    fillUnits = FillUnits.BOUNDS
                    spreadMethod = SpreadMethod.REPEAT
                    quantization = 8
                    elliptic {
                        radiusX = cos(index / 2.0 * PI + seconds) * 0.45 + 0.5
                        radiusY = sin(index / 2.0 * PI + seconds) * 0.45 + 0.5
                    }
                }

                drawer.rectangle(cell)

            }
        }
    }
}