package gradients

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.color.spaces.OKHSV
import org.openrndr.extra.color.tools.shiftHue
import org.openrndr.extra.shadestyles.fills.FillFit
import org.openrndr.extra.shadestyles.fills.SpreadMethod
import org.openrndr.extra.shadestyles.fills.gradients.gradient
import org.openrndr.extra.shapes.primitives.grid
import org.openrndr.extra.shapes.primitives.placeIn
import org.openrndr.math.Vector2

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            extend {
                val grid = drawer.bounds.grid(3, 3)

                for ((index, row) in grid.withIndex()) {
                    drawer.shadeStyle = gradient<ColorRGBa> {
                        for (i in 0..10) {
                            stops[i / 10.0] = ColorRGBa.RED.shiftHue<OKHSV>(i * 36.0)

                        }
                        spreadMethod = SpreadMethod.PAD
                        this.fillFit = FillFit.entries[index]
                        radial {
                            center = Vector2(0.5, 0.5)
                        }
                    }

                    for ((x, cell) in row.withIndex()) {
                        val paddedCell = cell.offsetEdges(-10.0)
                        when (x) {
                            0 -> drawer.rectangle(paddedCell.sub(0.0..0.5, 0.0..1.0).placeIn(paddedCell))
                            1 -> drawer.rectangle(paddedCell)
                            2 -> drawer.rectangle(paddedCell.sub(0.0..1.0, 0.0..0.5).placeIn(paddedCell))
                        }
                    }
                }
            }
        }
    }
}