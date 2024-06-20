import org.openrndr.application
import org.openrndr.extra.expressions.watchingExpression1
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.addTo
import org.openrndr.extra.parameters.TextParameter

/**
 * Improved version of DemoExpressionEvaluator01, it uses [watchingExpression1] to automatically convert an expression
 * string into a function with a parameter "t".
 */
fun main() {
    application {
        program {
            val gui = GUI()
            gui.compartmentsCollapsedByDefault = false

            // the constants used in our expressions
            val constants = mutableMapOf("width" to drawer.width.toDouble(), "height" to drawer.height.toDouble())

            val settings = object {
                @TextParameter("x expression", order = 10)
                var xExpression = "cos(t) * 50.0 + width / 2.0"

                @TextParameter("y expression", order = 20)
                var yExpression = "sin(t) * 50.0 + height / 2.0"

                @TextParameter("radius expression", order = 30)
                var radiusExpression = "cos(t) * 50.0 + 50.0"
            }.addTo(gui)

            val xFunction by watchingExpression1(settings::xExpression, "t", constants)
            val yFunction by watchingExpression1(settings::yExpression, "t", constants)
            val radiusFunction by watchingExpression1(settings::radiusExpression, "t", constants)

            extend(gui)
            extend {
                val x = xFunction(seconds)
                val y = yFunction(seconds)
                val radius = radiusFunction(seconds)
                drawer.circle(x, y, radius)
            }
        }
    }
}