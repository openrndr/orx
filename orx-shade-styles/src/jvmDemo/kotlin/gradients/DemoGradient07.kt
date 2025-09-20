package gradients

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.camera.Camera2D
import org.openrndr.extra.shadestyles.fills.FillFit
import org.openrndr.extra.shadestyles.fills.FillUnits
import org.openrndr.extra.shadestyles.fills.SpreadMethod
import org.openrndr.extra.shadestyles.fills.gradients.gradient
import org.openrndr.extra.shapes.primitives.grid
import org.openrndr.math.Vector2

/**
 * A design with 48 vertical bands with gradients. Each one has a unique `quantization`
 * value based on the index of the band. All bands have 2 color `stops`:
 * `WHITE` at the top (position 0.0), and `BLACK` near the bottom (near position 1.0),
 * with the exact value depending on the `quantization` value.
 *
 * Demonstrates how to produce a quantized gradient with a specific number of equal color bands.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        extend(Camera2D())
        extend {
            val grid = drawer.bounds.grid(48, 1)
            drawer.stroke = null

            for ((index, cell) in grid.flatten().withIndex()) {
                drawer.shadeStyle = gradient<ColorRGBa> {
                    quantization = index + 2
                    stops[0.0] = ColorRGBa.WHITE
                    stops[(quantization) / (quantization + 1.0)] = ColorRGBa.BLACK

                    fillUnits = FillUnits.BOUNDS
                    fillFit = FillFit.COVER
                    spreadMethod = SpreadMethod.PAD

                    linear {
                        start = Vector2(0.5, 0.0)
                        end = Vector2(0.5, 1.0)
                    }
                }
                drawer.rectangle(cell)
            }
        }
    }
}