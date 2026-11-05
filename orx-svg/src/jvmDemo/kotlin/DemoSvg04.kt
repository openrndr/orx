import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.composition.Paint
import org.openrndr.extra.composition.ShapeNode
import org.openrndr.extra.composition.composition
import org.openrndr.extra.composition.fill
import org.openrndr.extra.composition.findGroups
import org.openrndr.extra.composition.findShapes
import org.openrndr.extra.composition.stroke
import org.openrndr.extra.shadestyles.presets.*
import org.openrndr.extra.svg.loadSVG
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import kotlin.math.cos
import kotlin.math.sin

/**
 * Demonstrates changing the paints of a loaded SVG document. The shapes of the group with the id "basics" are filled
 * with presets instead of their colors: a gradient that turns, noise that moves, checkers and a kaleidoscope-like
 * gradient. Presets are objects with properties; changing them every frame does not compile new shaders.
 *
 * The strokes of the paths in the group "paths" get a gradient in user space that slides along.
 */
fun main() = application {
    configure {
        width = 720
        height = 480
    }
    program {
        val composition = loadSVG("orx-svg/src/jvmDemo/resources/svg/shapes.svg")
        fun group(id: String) = composition.root.findGroups().first { it.id == id }

        val turning = LinearGradientPaint(
            stops = listOf(
                ColorStop(0.0, ColorRGBa.fromHex("#ffcc00")),
                ColorStop(0.5, ColorRGBa.fromHex("#ff3366")),
                ColorStop(1.0, ColorRGBa.fromHex("#3322aa"))
            )
        )
        val noise = NoiseSource().apply {
            from = ColorRGBa.fromHex("#0b1a40"); to = ColorRGBa.fromHex("#4c9eff"); scale = 3.0
        }
        val checkers = CheckersSource().apply { xCells = 8; yCells = 8; odd = ColorRGBa.fromHex("#4c9e8a") }
        val rings = RadialGradientPaint(
            radius = 0.3,
            stops = listOf(ColorStop(0.0, ColorRGBa.fromHex("#3d5a99")), ColorStop(1.0, ColorRGBa.WHITE)),
            spread = GradientSpread.REFLECT
        )
        val presets = listOf(turning, noise, checkers, rings)
        group("basics").findShapes().filter { it.style.fill !is Paint.None }.zip(presets).forEach { (shape, preset) ->
            shape.style.fill = Paint.Preset(preset)
        }

        val sliding = LinearGradientPaint(
            start = Vector2(0.0, 0.0), end = Vector2(120.0, 0.0),
            stops = listOf(ColorStop(0.0, ColorRGBa.fromHex("#e8574a")), ColorStop(1.0, ColorRGBa.fromHex("#3d5a99"))),
            spread = GradientSpread.REFLECT
        )
        group("paths").findShapes().filter { it.style.stroke !is Paint.None }.forEach { shape: ShapeNode ->
            shape.style.stroke = Paint.Preset(sliding, PaintUnits.OBJECT)
        }

        extend {
            val t = seconds
            turning.start = Vector2(0.5, 0.5) - Vector2(cos(t), sin(t)) * 0.5
            turning.end = Vector2(0.5, 0.5) + Vector2(cos(t), sin(t)) * 0.5
            noise.offset = Vector3(t * 0.3, 0.0, t * 0.2)
            checkers.xCells = 4 + ((sin(t) * 0.5 + 0.5) * 8).toInt()
            rings.focal = Vector2(0.5 + cos(t * 1.3) * 0.2, 0.5 + sin(t * 1.3) * 0.2)
            sliding.start = Vector2(t * 60.0, 0.0)
            sliding.end = Vector2(t * 60.0 + 120.0, 0.0)

            drawer.clear(ColorRGBa.WHITE)
            drawer.composition(composition)
        }
    }
}
