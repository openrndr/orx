import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.extra.color.mixing.SpectralColor
import org.openrndr.extra.color.mixing.SpectralGamutMapping
import org.openrndr.extra.color.mixing.mixSpectral
import org.openrndr.extra.color.mixing.spectralGradient
import org.openrndr.extra.color.mixing.spectralPalette
import org.openrndr.math.Vector2
import kotlin.math.sin

/**
 * Demonstrates spectral color mixing, which mixes colors the way paints mix:
 * - blue and yellow mixed in RGB and spectrally;
 * - three paints mixed in every proportion, on a triangle;
 * - a palette and a gradient with three color stops;
 * - the effect of tinting strength: a weaker red changes the mix with yellow less, the strength is animated.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val font = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 13.0)

        val blue = ColorRGBa.fromHex("#002185")
        val yellow = ColorRGBa.fromHex("#FCD200")
        val steps = 48

        // the corners of the triangle are three paints, every cell mixes them in proportion to its position
        val paints = listOf("#FCF046", "#E53166", "#3375DA").map { SpectralColor(ColorRGBa.fromHex(it)) }
        val corners = listOf(Vector2(190.0, 250.0), Vector2(30.0, 527.0), Vector2(350.0, 527.0))
        val rows = 18
        val cells = buildList {
            for (row in 0 until rows) {
                for (column in 0..row) {
                    // barycentric coordinates of the cell center
                    val u = 1.0 - (row + 0.5) / rows
                    val v = (column + 0.5) / rows
                    val w = 1.0 - u - v
                    val center = corners[0] * u + corners[1] * w + corners[2] * v
                    val color = mixSpectral(paints[0] to u, paints[1] to w, paints[2] to v)
                        .toRGBa(SpectralGamutMapping.MAP).toSRGB()
                    add(center to color)
                }
            }
        }
        val cellSize = (corners[2].x - corners[1].x) / rows

        val palette = spectralPalette(ColorRGBa.fromHex("#BB0657"), ColorRGBa.fromHex("#E0E0B3"), 8)
        val gradientStops = arrayOf(
            ColorRGBa.fromHex("#005E72") to 0.0, ColorRGBa.fromHex("#EAD9A7") to 0.5, ColorRGBa.fromHex("#894B54") to 1.0
        )
        val gradient = List(steps) { spectralGradient(it / (steps - 1.0), *gradientStops) }

        fun strip(colors: List<ColorRGBa>, x: Double, y: Double, width: Double, height: Double) {
            val w = width / colors.size
            for ((i, c) in colors.withIndex()) {
                drawer.fill = c.toSRGB()
                drawer.rectangle(x + i * w, y, w + 0.5, height)
            }
        }

        extend {
            drawer.clear(ColorRGBa.fromHex("#F4F1EA"))
            drawer.fontMap = font
            drawer.stroke = null

            fun label(text: String, x: Double, y: Double) {
                drawer.fill = ColorRGBa.fromHex("#333333")
                drawer.text(text, x, y)
            }

            // RGB versus spectral mixing of blue and yellow
            label("blue and yellow mixed in RGB", 30.0, 40.0)
            strip(List(steps) { blue.toLinear().mix(yellow.toLinear(), it / (steps - 1.0)) }, 30.0, 50.0, 660.0, 50.0)
            label("blue and yellow mixed spectrally", 30.0, 130.0)
            strip(List(steps) { mixSpectral(blue to 1.0 - it / (steps - 1.0), yellow to it / (steps - 1.0)) }, 30.0, 140.0, 660.0, 50.0)

            // three paints mixed in every proportion
            label("three paints", 30.0, 230.0)
            for ((center, color) in cells) {
                drawer.fill = color
                drawer.circle(center, cellSize * 0.5)
            }

            // palette and gradient
            label("palette, 8 steps", 390.0, 230.0)
            strip(palette, 390.0, 240.0, 300.0, 50.0)
            label("gradient with three stops", 390.0, 320.0)
            strip(gradient, 390.0, 330.0, 300.0, 50.0)

            // tinting strength
            val strength = 0.6 + 0.4 * sin(seconds)
            val red = SpectralColor(ColorRGBa.fromHex("#ff0000"), strength)
            val yellowPaint = SpectralColor(ColorRGBa.fromHex("#ffff00"))
            label("red and yellow, red tinting", 390.0, 410.0)
            label("strength %.2f".format(strength), 390.0, 426.0)
            strip(List(steps) {
                val t = it / (steps - 1.0)
                mixSpectral(red to 1.0 - t, yellowPaint to t).toRGBa(SpectralGamutMapping.MAP)
            }, 390.0, 436.0, 300.0, 50.0)
            label("red and yellow, tinting strength 1", 390.0, 516.0)
            strip(List(steps) {
                val t = it / (steps - 1.0)
                mixSpectral(SpectralColor(ColorRGBa.fromHex("#ff0000")) to 1.0 - t, yellowPaint to t)
                    .toRGBa(SpectralGamutMapping.MAP)
            }, 390.0, 526.0, 300.0, 50.0)
        }
    }
}
