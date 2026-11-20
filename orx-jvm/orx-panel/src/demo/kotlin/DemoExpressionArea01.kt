import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.expressions.ExpressionException
import org.openrndr.extra.expressions.ExpressionProblem
import org.openrndr.extra.expressions.checkExpression
import org.openrndr.extra.expressions.evaluateExpression
import org.openrndr.extra.expressions.typed.checkTypedExpression
import org.openrndr.extra.expressions.typed.evaluateTypedExpression
import org.openrndr.math.Vector2
import org.openrndr.panel.controlManager
import org.openrndr.panel.elements.*

/**
 * Editors of expressions of orx-expression-evaluator: an untyped one, which gives the radius of the circle, and a
 * typed one, which gives the points that are drawn, both of `t`, the time. The tokens are
 * colored, the problems are underlined, with their messages as tooltips and listed below the editors; what goes wrong
 * while evaluating is marked too.
 */
fun main() = application {
    configure {
        width = 1000
        height = 640
    }
    program {
        extend { drawer.clear(ColorRGBa.fromHex("#16181c")) }

        lateinit var radius: ExpressionArea
        lateinit var points: ExpressionArea
        lateinit var radiusProblems: P
        lateinit var pointsProblems: P

        fun describe(problems: List<ExpressionProblem>) =
            if (problems.isEmpty()) "no problems" else problems.joinToString("\n") { "${it.kind.name.lowercase()}: ${it.message}" }

        val cm = controlManager {
            css("""
                .editors { width: 480px; height: 100%; padding: 10px; background-color: #1d2026 }
                h3 { margin-top: 6px; margin-bottom: 4px }
                text-area { width: 100%; height: 170px; background-color: #121418; margin-bottom: 4px }
                p { font-size: 12px; color: rgba(255, 255, 255, 0.6); margin-bottom: 8px }
            """)
            layout {
                div("editors") {
                    h3 { "radius" }
                    radius = expressionArea {
                        check = { checkExpression(it, setOf("t")) }
                        value = "fun pulse(x) = (sin(x * 3) + 1) / 2\nval r = 40 + pulse(t) * 60\nr"
                    }
                    radiusProblems = p { "" }
                    h3 { "points" }
                    points = expressionArea {
                        check = { checkTypedExpression(it, setOf("t", "r")) }
                        value = "fun ring(k, n) {\n    val a = k / n * 2 * PI + t\n    vec2(cos(a), sin(a)) * r\n}\n(0 until 12).map { ring(it, 12) }"
                    }
                    pointsProblems = p { "" }
                }
            }
        }
        radius.problemsChanged.listen { radiusProblems.replaceText(describe(it)) }
        points.problemsChanged.listen { pointsProblems.replaceText(describe(it)) }
        radiusProblems.replaceText(describe(radius.problems))
        pointsProblems.replaceText(describe(points.problems))
        extend(cm)

        val center = Vector2(750.0, 320.0)
        extend {
            val t = seconds
            // what goes wrong while evaluating is marked, until the expression changes
            val r = try {
                evaluateExpression(radius.value, mapOf("t" to t))
            } catch (e: ExpressionException) {
                if (radius.problems.isEmpty()) radius.report(e)
                null
            } ?: 50.0
            drawer.fill = null
            drawer.stroke = ColorRGBa.fromHex("#5ab0ff")
            drawer.circle(center, r)

            val ps = try {
                evaluateTypedExpression(points.value, { mapOf("t" to t, "r" to r)[it] })
            } catch (e: ExpressionException) {
                if (points.problems.isEmpty()) points.report(e)
                null
            }
            drawer.fill = ColorRGBa.fromHex("#f88379")
            drawer.stroke = null
            (ps as? List<*>)?.filterIsInstance<Vector2>()?.let { drawer.circles(it.map { p -> center + p }, 6.0) }
        }
    }
}
