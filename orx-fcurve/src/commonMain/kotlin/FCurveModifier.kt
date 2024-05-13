package org.openrndr.extra.fcurve

import org.openrndr.extra.expressions.FunctionExtensions
import org.openrndr.extra.expressions.compileFunction1
import org.openrndr.math.Vector2

/**
 * Modify an [fcurve] string using a [modifiers] string
 */
fun modifyFCurve(
    fcurve: String,
    modifiers: String,
    constants: Map<String, Double> = emptyMap(),
    functions: FunctionExtensions = FunctionExtensions.EMPTY
): String {
    val parts = fCurveCommands(fcurve)
    val mparts = parts.reversed().toMutableList()

    @Suppress("RegExpRedundantEscape")
    val modifier = Regex("([xy])=\\{([^{}]+)\\}")

    val modifierExpressions = modifier.findAll(modifiers).map { it.groupValues[1] to it.groupValues[2] }.toMap()

    val xModifierExpression = modifierExpressions["x"]
    val xModifier =
        if (xModifierExpression != null) compileFunction1(xModifierExpression, "x", constants, functions) else {
            { x: Double -> x }
        }

    val yModifierExpression = modifierExpressions["y"]
    val yModifier =
        if (yModifierExpression != null) compileFunction1(yModifierExpression, "y", constants, functions) else {
            { y: Double -> y }
        }

    fun popToken(): String = mparts.removeLast()
    fun popNumber(): Double = mparts.removeLast().toDoubleOrNull() ?: error("not a number")

    fun String.numberOrFactorOf(percentageOf: (Double) -> Double): Double {
        return if (endsWith("%")) {
            val f = (dropLast(1).toDoubleOrNull() ?: error("'$this' is not a percentage")) / 100.0
            percentageOf(f)
        } else {
            toDoubleOrNull() ?: error("'$this' is not a number")
        }
    }

    fun String.numberOrPercentageOf(percentageOf: () -> Double): Double {
        return numberOrFactorOf { f -> f * percentageOf() }
    }

    var cursor = Vector2.ZERO
    var modified = ""
    fun emit(command: String, vararg ops: Double, relative: Boolean, x: Double, y: Double) {
        modified = modified + " " + command + " " + ops.joinToString(" ")
        cursor = if (relative) {
            Vector2(x + cursor.x, y + cursor.y)
        } else {
            Vector2(x + cursor.x, y)
        }
    }

    while (mparts.isNotEmpty()) {
        val command = mparts.removeLast()

        when (command) {

            /**
             * Handle move cursor command
             */
            "m", "M" -> {
                val relative = command.first().isLowerCase()
                val rf = if (relative) 1.0 else 0.0
                val y = popNumber() + rf * cursor.y
                emit("M", yModifier(y), relative = false, x = 0.0, y = y)
            }

            /**
             * Handle line command
             */
            "l", "L" -> {
                val relative = command.first().isLowerCase()
                val rf = if (relative) 1.0 else 0.0
                val x = popNumber()
                val y = popNumber() + rf * cursor.y

                emit("L", xModifier(x), yModifier(y), relative = false, x = x, y = y)
            }

            /**
             * Handle cubic bezier command
             */
            "c", "C" -> {
                val relative = command.first().isLowerCase()
                val rf = if (relative) 1.0 else 0.0

                val tcx0 = popToken()
                val tcy0 = popToken()
                val tcx1 = popToken()
                val tcy1 = popToken()
                val x = popNumber()
                val y = popNumber()
                val ay = y + cursor.y * rf
                val x0 = tcx0.numberOrPercentageOf { x }
                val y0 = tcy0.numberOrFactorOf { factor ->
                    if (relative) y * factor else cursor.y * (1.0 - factor).coerceAtLeast(0.0) + y * factor
                } + cursor.y * rf
                val x1 = tcx1.numberOrPercentageOf { x }
                val y1 = tcy1.numberOrFactorOf { factor ->
                    if (relative) y * factor else cursor.y * (1.0 - factor).coerceAtLeast(0.0) + y * factor
                } + cursor.y * rf
                emit(
                    "C",
                    xModifier(x0),
                    yModifier(y0),
                    xModifier(x1),
                    yModifier(y1),
                    xModifier(x),
                    xModifier(ay),
                    relative = false,
                    x = x,
                    y = ay
                )
            }

            /**
             * Handle quadratic bezier command
             */
            "q", "Q" -> {
                val relative = command.first().isLowerCase()
                val rf = if (relative) 1.0 else 0.0
                val tcx0 = popToken()
                val tcy0 = popToken()
                val x = popNumber()
                val y = popNumber()
                val ay = y + cursor.y * rf
                val x0 = tcx0.numberOrPercentageOf { x }
                val y0 = tcy0.numberOrFactorOf { factor ->
                    if (relative) y * factor else cursor.y * (1.0 - factor).coerceAtLeast(0.0) + y * factor
                } + rf * cursor.y
                emit("Q", xModifier(x0), yModifier(y0), xModifier(x), yModifier(ay), relative = false, x = x, y = ay)
            }

            /**
             * Handle horizontal line (or hold) command
             */
            "h", "H" -> {
                if (command == "H") {
                    val x = popNumber()
                    emit(command, xModifier(x), relative = false, x = x, y = cursor.y)
                    cursor = Vector2(x, cursor.y)
                } else {
                    val x = popNumber()
                    emit(command, xModifier(x), relative = false, x = x, y = cursor.y)
                }
            }

            /**
             * Handle cubic smooth to command
             */
            "s", "S" -> {
                val relative = command.first().isLowerCase()
                val rf = if (relative) 1.0 else 0.0
                val tcx0 = popToken()
                val tcy0 = popToken()
                val x = popNumber()
                val y = popNumber()
                val ay = y + cursor.y * rf
                val x1 = tcx0.numberOrPercentageOf { x }
                val y1 = tcy0.numberOrFactorOf { factor ->
                    if (relative) y * factor else cursor.y * (1.0 - factor).coerceAtLeast(0.0) + y * factor
                } + rf * cursor.y
                emit("S", xModifier(x1), yModifier(y1), xModifier(x), yModifier(ay), relative = false, x = x, y = ay)
            }

            /**
             * Handle quadratic smooth to command
             */
            "t", "T" -> {
                val relative = command.first().isLowerCase()
                val rf = if (relative) 1.0 else 0.0
                val x = popNumber()
                val y = popNumber() + cursor.y * rf
                emit("T", xModifier(x), yModifier(y), relative = false, x = x, y = y)
            }

            else -> error("unknown command: $command in ${parts}")
        }
    }
    return modified
}

fun main() {
    val f = "l 10 10 h 4 t 20.0 20.0 s 5% 50% 30.0 30.0"
    println(modifyFCurve(f, "x={sqrt(x)} y={y * 2.0}"))

    val mf = "l 10 10 h 4 t 20.0 20.0 s 5% 50% 30.0 30.0 | x={2.0 * x} y={-3.0 * y}"
    val f2 = mfcurve(mf)
    println(f2)
}