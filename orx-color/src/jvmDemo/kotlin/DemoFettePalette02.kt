import org.openrndr.application
import org.openrndr.draw.isolated
import org.openrndr.extra.color.fettepalette.ColorRamp
import org.openrndr.extra.color.fettepalette.ColorRampParameters
import org.openrndr.extra.color.fettepalette.generateColorRamp
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.addTo
import org.openrndr.math.Vector2
import kotlin.random.Random

/**
 * Demonstrates how to create a GUI to control a `Fette Palette` color ramp.
 *
 * The `ColorRampParameters()` method is used to create an object containing
 * the parameters needed by `generateColorRamp()`. We pass this object
 * to the GUI, which creates sliders and other needed to explore
 * the creation of color palettes.
 *
 * The GUI's built-in `random` button can be used to randomize palettes.
 * `shift + click` on `random` for wilder results.
 *
 * The created color ramps contain `baseColors`, `lightColors` and `darkColors`. All three collections
 * are rendered as small colored rectangles.
 *
 * In the center of the window, four colors from those collections are rendered as larger rectangles,
 * using a random base color, a random light color, and two random dark colors.
 * A random seed based in `seconds` is used for an animated effect.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }

    program {
        val gui = GUI()
        val parameters = ColorRampParameters()
        parameters.addTo(gui)

        extend(gui)
        extend {
            val ramp = generateColorRamp(parameters)
            fun rampSquare(ramp: ColorRamp, random: Random, position: Vector2, width: Double) {
                drawer.isolated {
                    drawer.fill = ramp.baseColors.random(random).toRGBa()
                    drawer.stroke = null
                    drawer.rectangle(position, width, width)

                    drawer.fill = ramp.lightColors.random(random).toRGBa()
                    drawer.rectangle(position + Vector2(width / 4.0, width / 4.0), width / 4.0, width / 2.0)

                    val dc = ramp.darkColors.shuffled(random).take(2)

                    drawer.fill = dc[0].toRGBa()
                    drawer.rectangle(position + Vector2(width / 2.0, width / 4.0), width / 4.0, width / 4.0)

                    drawer.fill = dc[1].toRGBa()
                    drawer.rectangle(position + Vector2(width / 2.0, width / 2.0), width / 4.0, width / 4.0)
                }
            }

            drawer.translate(200.0, 0.0)

            drawer.isolated {
                for ((index, c) in ramp.lightColors.withIndex()) {
                    drawer.stroke = null
                    drawer.fill = c
                    drawer.rectangle(20.0, 20.0, 50.0, 50.0)
                    drawer.translate(50.0, 0.0)
                }
            }
            drawer.isolated {
                for ((index, c) in ramp.baseColors.withIndex()) {
                    drawer.stroke = null
                    drawer.fill = c
                    drawer.rectangle(20.0, 70.0, 50.0, 50.0)
                    drawer.translate(50.0, 0.0)
                }
            }
            drawer.isolated {
                for ((index, c) in ramp.darkColors.withIndex()) {
                    drawer.stroke = null
                    drawer.fill = c
                    drawer.rectangle(20.0, 120.0, 50.0, 50.0)
                    drawer.translate(50.0, 0.0)
                }
            }

            val random = Random(seconds.toInt())
            rampSquare(ramp, random, Vector2(20.0, 180.0), 360.0)
        }
    }
}
