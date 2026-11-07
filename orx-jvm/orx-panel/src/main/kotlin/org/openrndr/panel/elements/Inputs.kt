package org.openrndr.panel.elements

import org.openrndr.extra.expressions.ExpressionException
import org.openrndr.extra.expressions.evaluateExpression
import kotlin.math.PI

private val percentage = Regex("((?:\\d+\\.?\\d*|\\.\\d+))%")

/**
 * The value of the typed [text]: a number or arithmetic, like `2*pi` or `_ / 2`, with the constants `pi`, `tau` and
 * `e`, and `_` for [current]; a percentage is of [range], from its minimum, or a fraction without one. Null when the
 * text is not a value.
 */
fun evaluateInput(text: String, current: Double, range: ClosedRange<Double>? = null): Double? {
    val expression = percentage.replace(text.trim()) { m ->
        val fraction = m.groupValues[1].toDouble() / 100.0
        if (range == null) "($fraction)" else "(${range.start + fraction * (range.endInclusive - range.start)})"
    }
    return try {
        evaluateExpression(expression, constants = mapOf("_" to current, "pi" to PI, "tau" to 2.0 * PI, "e" to Math.E))
            ?.takeIf { it.isFinite() }
    } catch (_: ExpressionException) {
        null
    } catch (_: RuntimeException) {
        null
    }
}
