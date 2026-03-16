import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.expressions.watchingExpression1
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.addTo
import org.openrndr.extra.parameters.TextParameter
import org.openrndr.math.Vector2

/**
 * Spirograph-like demo of orx-expression-evaluator, using [watchingExpression1] to automatically convert an expression
 * String into a function with a parameter "t".
 *
 * The program generates a list with 2000 points and draws a line strip connecting them.
 *
 * Editing the expression may make it temporarily invalid. When this happens, it returns 0.0 (for instance,
 * due to non-matching parenthesis). This is normally not an issue, but the `lineStrip()` method fails
 * when consecutive points are identical, which is the case if all points are Vector2(0.0).
 * Therefore, we wrap it in a try/catch to keep the program running even while the expressions are not valid.
 */
fun main() = application {
    program {
        val gui = GUI()
        gui.compartmentsCollapsedByDefault = false

        val settings = object {
            @TextParameter("x expression", order = 10)
            var xExpression = "cos(t) * cos(t*0.25+1.0) * 230.0"

            @TextParameter("y expression", order = 20)
            var yExpression = "sin(t) * sin(t*0.151) * 230.0"
        }.addTo(gui)

        val xFunction by watchingExpression1(settings::xExpression, "t")
        val yFunction by watchingExpression1(settings::yExpression, "t")

        extend(gui)
        extend {
            drawer.stroke = ColorRGBa.WHITE
            drawer.translate(drawer.bounds.center)
            val points = List(2000) {
                val t = it * 0.1 + seconds
                Vector2(xFunction(t), yFunction(t))
            }
            try {
                drawer.lineStrip(points)
            } catch (_: Exception) {
            }
        }
    }
}
