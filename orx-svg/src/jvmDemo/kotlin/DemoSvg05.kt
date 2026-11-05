import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.composition.composition
import org.openrndr.extra.svg.loadSVG
import org.openrndr.extra.svg.parseSVG
import org.openrndr.extra.svg.toSVG

/**
 * Demonstrates writing a loaded SVG document back to SVG: the composition is written with `toSVG`, including the
 * gradients it uses, and the result is parsed again. The original is drawn on the left, the document that was
 * written and read back on the right; they look the same.
 */
fun main() = application {
    configure {
        width = 720
        height = 260
    }
    program {
        val original = loadSVG("orx-svg/src/jvmDemo/resources/svg/gradients.svg")
        val written = original.toSVG()
        println(written.lines().take(3).joinToString("\n"))
        val reread = parseSVG(written)

        extend {
            drawer.clear(ColorRGBa.WHITE)
            drawer.scale(0.5)
            drawer.composition(original)
            drawer.translate(720.0, 0.0)
            drawer.composition(reread)
        }
    }
}
