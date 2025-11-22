package colorRange

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.extra.color.palettes.rangeTo
import org.openrndr.extra.color.spaces.*

/**
 * This program creates color interpolations from `ColorRGBa.BLUE` to
 * `ColorRGBa.PINK` in 25 steps in multiple color spaces.
 *
 * The window height is adjusted based on the number of interpolations to show.
 *
 * The resulting gradients differ in saturation and brightness and apparently include more
 * `BLUE` or more `PINK` depending on the chosen color space.
 */
fun main() = application {
    val colorA = ColorRGBa.BLUE
    val colorB = ColorRGBa.PINK

    val stepCount = 25

    val allSteps = listOf(
        "RGB" to (colorA..colorB blend stepCount),
        "RGB linear" to (colorA.toLinear()..colorB.toLinear() blend stepCount),
        "HSV" to (colorA..colorB.toHSVa() blend stepCount),
        "Lab" to (colorA.toLABa()..colorB.toLABa() blend stepCount),
        "LCh(ab)" to (colorA.toLCHABa()..colorB.toLCHABa() blend stepCount),
        "OKLab" to (colorA.toOKLABa()..colorB.toOKLABa() blend stepCount),
        "OKLCh" to (colorA.toOKLCHa()..colorB.toOKLCHa() blend stepCount),
        "OKHSV" to (colorA.toOKHSVa()..colorB.toOKHSVa() blend stepCount),
        "OKHSL" to (colorA.toOKHSLa()..colorB.toOKHSLa() blend stepCount),
        "HSLUV" to (colorA.toHSLUVa()..colorB.toHSLUVa() blend stepCount),
        "XSLUV" to (colorA.toXSLUVa()..colorB.toXSLUVa() blend stepCount),
    )

    configure {
        width = 720
        height = 30 + 50 * allSteps.size
    }
    program {
        extend {
            drawer.clear(ColorRGBa.WHITE)
            drawer.stroke = null
            drawer.fontMap = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 16.0)
            drawer.translate(20.0, 20.0)
            for ((label, steps) in allSteps) {
                drawer.fill = ColorRGBa.GRAY.shade(0.25)
                drawer.text(label, 0.0, 24.0)

                for (i in steps.indices) {
                    drawer.fill = steps[i].toSRGB()
                    drawer.rectangle(100.0 + i * 20.0, 0.0, 20.0, 40.0)
                }
                drawer.translate(0.0, 50.0)
            }
        }
    }
}
