package org.openrndr.extra.fcurve

import org.openrndr.extra.expressions.FunctionExtensions
import org.openrndr.extra.expressions.evaluateExpression

/**
 * expand mfcurve to fcurve
 */
fun mfcurve(
    mf: String,
    constants: Map<String, Double> = emptyMap(),
    functions: FunctionExtensions = FunctionExtensions.EMPTY
): String {
    /**
     * perform comment substitution
     */
    val stripped = Regex("(#.*)$", RegexOption.MULTILINE).replace(mf, "")

    /**
     * detect modifier
     */
    val parts = stripped.split("|")

    val efcurve = parts.getOrElse(0) { "" }
    val modifier = parts.getOrNull(1)

    var fcurve = efcurve(efcurve, constants, functions)
    if (modifier != null) {
        fcurve = modifyFCurve(fcurve, modifier, constants, functions)
    }
    return fcurve
}

/**
 * Processes and expands a formatted string based on specific expressions and rules such as comments, lists,
 * and repetitions. The method allows for recursive evaluation of expressions within the string.
 *
 * @param ef The input string to be processed, containing expression placeholders, lists, or repetitions.
 * @param constants A map of constants used for substituting and evaluating expressions.
 * @param functions An object containing user-defined functions for expression evaluation.
 * @return A processed string with all expressions, lists, and repetitions evaluated and expanded.
 */
fun efcurve(
    ef: String,
    constants: Map<String, Double> = emptyMap(),
    functions: FunctionExtensions = FunctionExtensions.EMPTY
): String {
    // IntelliJ falsely reports a redundant escape character. the escape character is required when running the regular
    // expression on a javascript target. Removing the escape character will result in a `Lone quantifier brackets`
    // syntax error.

    @Suppress("RegExpRedundantEscape")
    val expression = Regex("\\{([^{}]+)\\}")

    @Suppress("RegExpRedundantEscape")
    val repetition = Regex("""\(([^()]+)\)\[([^\[\]]+)\]""")

    @Suppress("RegExpRedundantEscape")
    val list = Regex("\\(([^()]+)\\)\\{([^\\[\\]]+)\\}")

    /**
     * perform comment substitution
     */
    var curve = Regex("(#.*)$", RegexOption.MULTILINE).replace(ef, "")

    /**
     * Allow for nested repetitions and lists
     */
    do {
        val referenceCurve = curve

        /**
         * perform list expansion |text|{items}
         */
        curve = list.replace(curve) { occ ->
            val listText = expression.replace(occ.groupValues[2]) { exp ->
                val expressionText = exp.groupValues[1]
                evaluateExpression(expressionText, constants, functions)?.toString()
                    ?: error("parse error in repetition count expression '$expressionText'")
            }
            val listTokens = listText.split(Regex("[,;][\t\n ]*|[\t\n ]+"))
            val listItems = listTokens.filter { it.isNotEmpty() }
                .map { it.trim().toDoubleOrNull() ?: error("'$it' is not a number in $listTokens") }

            listItems.mapIndexed { index, value ->
                expression.replace(occ.groupValues[1]) { exp ->
                    val expressionText = exp.groupValues[1]
                    evaluateExpression(
                        exp.groupValues[1],
                        constants + mapOf("index" to index.toDouble(), "it" to value),
                        functions
                    )?.toString() ?: error("parse error in repeated expression '$expressionText'")
                }
            }.joinToString(" ")
        }

        /**
         * perform repetition expansion |text|[repeat-count]
         */
        curve = repetition.replace(curve) { occ ->
            val repetitions = expression.replace(occ.groupValues[2]) { exp ->
                val expressionText = exp.groupValues[1]
                evaluateExpression(expressionText, constants)?.toInt()?.toString()
                    ?: error("parse error in repetition count expression '$expressionText'")
            }.toInt()
            List(repetitions) { repetition ->
                expression.replace(occ.groupValues[1]) { exp ->
                    val expressionText = exp.groupValues[1]
                    evaluateExpression(
                        exp.groupValues[1],
                        constants + mapOf("it" to repetition.toDouble()),
                        functions
                    )?.toString()
                        ?: error("parse error in repeated expression '$expressionText'")
                }
            }.joinToString(" ")
        }
    } while (curve != referenceCurve)

    /**
     * evaluate expression in expansion
     */
    return (expression.replace(curve) { exp ->
        evaluateExpression(exp.groupValues[1], constants, functions)?.toString() ?: error("parse error in '$curve")
    })
}