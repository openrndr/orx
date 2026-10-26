import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.extra.color.mixing.PigmentReflectances
import org.openrndr.extra.color.mixing.SpectralColor
import org.openrndr.extra.color.mixing.SpectralGamutMapping
import org.openrndr.extra.color.mixing.linearToReflectance
import org.openrndr.extra.color.mixing.mixSpectral
import org.openrndr.extra.color.mixing.reflectanceToRGBa
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour

/**
 * Compares measured pigment spectra from [PigmentReflectances] with spectra constructed from their RGB colors by
 * [linearToReflectance].
 *
 * The top shows pigments with their measured (solid) and constructed (dashed) reflectance spectra. Both spectra have
 * the same color under D65 light, they are metamers, but their shapes differ. The bottom mixes pairs of pigments in
 * steps, once with the measured spectra and once with the constructed ones: because the spectra differ, so do the
 * mixes.
 */
fun main() = application {
    configure {
        width = 960
        height = 720
    }
    program {
        val font = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 12.0)

        val pigments = listOf(
            "cadmium yellow" to PigmentReflectances.cadmiumYellow,
            "chrome yellow" to PigmentReflectances.chromeYellow,
            "cadmium red" to PigmentReflectances.cadmiumRed,
            "red lead" to PigmentReflectances.redLead,
            "ultramarine" to PigmentReflectances.ultramarineBlueArtificial,
            "cobalt blue" to PigmentReflectances.cobaltBlue,
            "prussian blue" to PigmentReflectances.prussianBlue,
            "viridian" to PigmentReflectances.viridian
        )

        // the constructed spectrum has the same color as the measured one
        fun constructed(measured: DoubleArray) = linearToReflectance(reflectanceToRGBa(measured))

        val pairs = listOf(
            PigmentReflectances.ultramarineBlueArtificial to PigmentReflectances.cadmiumYellow,
            PigmentReflectances.prussianBlue to PigmentReflectances.chromeYellow,
            PigmentReflectances.cobaltBlue to PigmentReflectances.cadmiumRed,
            PigmentReflectances.viridian to PigmentReflectances.redLead
        )
        val pairNames = listOf(
            "ultramarine + cadmium yellow", "prussian blue + chrome yellow",
            "cobalt blue + cadmium red", "viridian + red lead"
        )
        val steps = 24

        fun mixes(a: DoubleArray, b: DoubleArray) = List(steps) { i ->
            val t = i / (steps - 1.0)
            mixSpectral(SpectralColor(a) to 1.0 - t, SpectralColor(b) to t).toRGBa(SpectralGamutMapping.MAP).toSRGB()
        }

        val measuredMixes = pairs.map { (a, b) -> mixes(a, b) }
        val constructedMixes = pairs.map { (a, b) -> mixes(constructed(a), constructed(b)) }

        fun plot(reflectance: DoubleArray, area: Rectangle) = ShapeContour.fromPoints(
            reflectance.mapIndexed { i, r ->
                Vector2(area.x + area.width * i / 37.0, area.y + area.height * (1.0 - r.coerceIn(0.0, 1.0)))
            }, closed = false
        )

        fun dashed(contour: ShapeContour, dash: Double = 4.0): List<ShapeContour> {
            val length = contour.length
            return (0 until (length / (2 * dash)).toInt()).map { i ->
                contour.sub(i * 2 * dash / length, (i * 2 * dash + dash) / length)
            }
        }

        extend {
            drawer.clear(ColorRGBa.fromHex("#F4F1EA"))
            drawer.fontMap = font
            val ink = ColorRGBa.fromHex("#333333")

            // pigments with their measured and constructed spectra
            drawer.fill = ink
            drawer.text("measured (solid) and constructed (dashed) reflectance, 380-750 nm", 30.0, 28.0)
            for ((index, pigment) in pigments.withIndex()) {
                val (name, measured) = pigment
                val column = index % 4
                val row = index / 4
                val x = 30.0 + column * 230.0
                val y = 45.0 + row * 165.0
                val color = reflectanceToRGBa(measured).toSRGB()

                drawer.stroke = null
                drawer.fill = color
                drawer.rectangle(x, y, 40.0, 40.0)
                drawer.fill = ink
                drawer.text(name, x + 50.0, y + 24.0)

                val area = Rectangle(x, y + 50.0, 200.0, 95.0)
                drawer.fill = null
                drawer.stroke = ink.opacify(0.2)
                drawer.strokeWeight = 1.0
                drawer.rectangle(area)

                drawer.stroke = color.shade(0.8)
                drawer.strokeWeight = 2.0
                drawer.contour(plot(measured, area))
                drawer.stroke = ink
                drawer.strokeWeight = 1.0
                drawer.contours(dashed(plot(constructed(measured), area)))
            }

            // mixes with measured and constructed spectra
            drawer.stroke = null
            for ((index, name) in pairNames.withIndex()) {
                val y = 400.0 + index * 78.0
                drawer.fill = ink
                drawer.text(name, 30.0, y)
                drawer.text("measured", 30.0, y + 22.0)
                drawer.text("constructed", 30.0, y + 50.0)
                val w = 780.0 / steps
                for (i in 0 until steps) {
                    drawer.fill = measuredMixes[index][i]
                    drawer.rectangle(150.0 + i * w, y + 8.0, w + 0.5, 24.0)
                    drawer.fill = constructedMixes[index][i]
                    drawer.rectangle(150.0 + i * w, y + 36.0, w + 0.5, 24.0)
                }
            }
        }
    }
}
