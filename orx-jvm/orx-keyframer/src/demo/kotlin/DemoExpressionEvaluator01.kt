import org.openrndr.application
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.addTo
import org.openrndr.extra.keyframer.evaluateExpression
import org.openrndr.extra.parameters.TextParameter

fun main() {
    application {
        program {
            val gui = GUI()
            gui.compartmentsCollapsedByDefault = false

            val settings = object {
                @TextParameter("x expression", order = 10)
                var xExpression = "cos(t) * 50.0 + width / 2.0"
                @TextParameter("y expression", order = 20)
                var yExpression = "sin(t) * 50.0 + height / 2.0"
                @TextParameter("radius expression", order = 30 )
                var radiusExpression = "cos(t) * 50.0 + 50.0"
            }.addTo(gui)

            extend(gui)
            extend {
                //gui.visible = mouse.position.x < 200.0

                val expressionContext = mapOf("t" to seconds, "width" to drawer.bounds.width, "height" to drawer.bounds.height)

                fun eval(expression: String) : Double =
                    try { evaluateExpression(expression, expressionContext)  ?: 0.0 } catch (e: Throwable) { 0.0 }

                val x = eval(settings.xExpression)
                val y = eval(settings.yExpression)
                val radius = eval(settings.radiusExpression)

                drawer.circle(x, y, radius)
            }
        }
    }
}