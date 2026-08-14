import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.expressions.evaluateExpression
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.GUIAppearance
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.TextParameter
import org.openrndr.panel.style.defaultStyles

/**
 * Demonstrates the use of `evaluateExpression` to process strings containing mathematical expressions.
 *
 * In this demo, expressions are evaluated on every animation frame. Notice how the `t`, `width`,
 * and `height` values are passed to the evaluator, allowing these variables to be used directly
 * within the expressions.
 *
 * Evaluating expressions enables dynamic behavior adjustments at runtime, eliminating the need
 * to recompile the program.
 */
fun main() = application {
    program {
        val gui = GUI(
            GUIAppearance(ColorRGBa.PINK.shade(0.2).opacify(0.9)),
            defaultStyles(controlFontSize = 18.0)
        )
        gui.compartmentsCollapsedByDefault = false

        val settings = @Description("Settings") object {
            @TextParameter("x expression", order = 10)
            var xExpression = "cos(t) * 50.0 + width / 2.0"

            @TextParameter("y expression", order = 20)
            var yExpression = "sin(t) * 50.0 + height / 2.0"

            @TextParameter("radius expression", order = 30)
            var radiusExpression = "cos(t) * 50.0 + 50.0"
        }
        gui.add(settings)

        extend(gui)
        extend {
            //gui.visible = mouse.position.x < 200.0

            val expressionContext =
                mapOf("t" to seconds, "width" to drawer.bounds.width, "height" to drawer.bounds.height)

            fun eval(expression: String): Double =
                try {
                    evaluateExpression(expression, expressionContext) ?: 0.0
                } catch (e: Throwable) {
                    0.0
                }

            val x = eval(settings.xExpression)
            val y = eval(settings.yExpression)
            val radius = eval(settings.radiusExpression)
            drawer.circle(x, y, radius)
        }
    }
}
