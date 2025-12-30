package gradients

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Session
import org.openrndr.extra.color.presets.CRIMSON
import org.openrndr.extra.color.presets.DODGER_BLUE
import org.openrndr.extra.color.presets.LIME_GREEN
import org.openrndr.extra.shadestyles.fills.SpreadMethod
import org.openrndr.extra.shadestyles.fills.gradients.gradient
import org.openrndr.extra.shapes.primitives.grid
import org.openrndr.math.Vector2
import org.openrndr.math.asDegrees
import kotlin.math.atan2

/**
 * Demonstrates the creation of a grid-based design with 13x13 cells, each with an elliptic gradient
 * pointing towards the center of the window. The center cell features a circular gradient (by having
 * `radiusX` equal to `radiusY`). The farther a cell is from the center, the higher the aspect ratio
 * of the ellipse is, becoming closer to a line than to a circle near the corners.
 */
fun main() =
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            extend {
                val grid = drawer.bounds.grid(13, 13)
                drawer.stroke = null

                for ((y, row) in grid.withIndex()) {
                    for ((x, cell) in row.withIndex()) {
                        drawer.shadeStyle = gradient<ColorRGBa> {
                            stops[0.0] = ColorRGBa.CRIMSON
                            stops[0.5] = ColorRGBa.DODGER_BLUE
                            stops[1.0] = ColorRGBa.LIME_GREEN

                            spreadMethod = SpreadMethod.REPEAT
                            elliptic {
                                val v = Vector2(x - 6.0, y - 6.0)
                                rotation = atan2(y - 6.0, x - 6.0).asDegrees + 180.0 + seconds * 30.0
                                radiusX = 1.0
                                radiusY = 1.0 / (1.0 + v.length * 0.25)
                            }
                        }
                        drawer.rectangle(cell)
                    }
                }
                // Show the source of the current shader in the console
                // if(frameCount == 10) {
                //     println(drawer.shadeStyle?.fragmentPreamble)
                //     println(drawer.shadeStyle?.fragmentTransform)
                // }
            }
        }
    }

