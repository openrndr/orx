import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.composition.Paint
import org.openrndr.extra.composition.composition
import org.openrndr.extra.composition.drawComposition
import org.openrndr.extra.shadestyles.presets.*
import org.openrndr.extra.svg.toSVG
import org.openrndr.math.Matrix44
import org.openrndr.math.transforms.scale
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Rectangle

/**
 * Demonstrates paints in a composition: shapes are filled and stroked with gradients, which are written to SVG, and
 * with presets, such as noise and checkers, which are animated by changing their properties.
 *
 * The composition is written to SVG and printed: gradients become linearGradient and radialGradient elements, presets
 * are written as their fallback color.
 */
fun main() = application {
    configure {
        width = 720
        height = 480
    }
    program {
        val noise = NoiseSource().apply { from = ColorRGBa.fromHex("#102040"); to = ColorRGBa.fromHex("#80e0ff") }
        val checkers = CheckersSource().apply { xCells = 6; yCells = 6; odd = ColorRGBa.fromHex("#ff4080") }

        // presets read in object units are scaled so the noise has a pleasant size
        val noiseScale = Matrix44.scale(300.0, 300.0, 1.0)

        val composition = drawComposition {
            strokeWeight = 12.0
            stroke = null

            fillPaint = Paint.LinearGradient(
                Vector2(0.0, 0.0), Vector2(1.0, 1.0),
                listOf(ColorStop(0.0, ColorRGBa.fromHex("#ffcc00")), ColorStop(1.0, ColorRGBa.fromHex("#ff3366")))
            )
            circle(Vector2(140.0, 140.0), 100.0)

            fillPaint = Paint.RadialGradient(
                Vector2(0.5, 0.5), 0.5, focal = Vector2(0.3, 0.3),
                stops = listOf(ColorStop(0.0, ColorRGBa.WHITE), ColorStop(1.0, ColorRGBa.fromHex("#3322aa"))),
                spread = GradientSpread.REFLECT
            )
            strokePaint = Paint.LinearGradient(
                Vector2(260.0, 0.0), Vector2(460.0, 0.0),
                listOf(ColorStop(0.0, ColorRGBa.BLACK), ColorStop(1.0, ColorRGBa.fromHex("#22aa44"))),
                units = PaintUnits.OBJECT
            )
            stroke = ColorRGBa.BLACK
            rectangle(Rectangle(270.0, 50.0, 180.0, 180.0))
            stroke = null
            strokePaint = null

            // presets as paints, their properties can change while the composition is drawn
            fillPaint = Paint.Preset(noise, PaintUnits.OBJECT, noiseScale, ColorRGBa.fromHex("#408090"))
            circle(Vector2(590.0, 140.0), 100.0)

            fillPaint = Paint.Preset(checkers, fallback = ColorRGBa.GRAY)
            rectangle(Rectangle(40.0, 280.0, 640.0, 160.0))
        }

        println(composition.toSVG())

        extend {
            noise.offset = Vector3(0.0, 0.0, seconds * 0.2)
            checkers.xCells = 6 + (seconds % 6).toInt()
            drawer.clear(ColorRGBa.WHITE)
            drawer.composition(composition)
        }
    }
}
