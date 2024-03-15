package org.openrndr.extra.fcurve

import org.openrndr.extra.expressions.evaluateExpression

/**
 * expand efcurve to fcurve
 * @param ef an efcurve string
 * @param constants a map of constants that is passed to [evaluateExpression]
 */
fun efcurve(ef: String, constants: Map<String, Double> = emptyMap()): String {
    val expression = Regex("_([^_]+)_")

    // IntelliJ falsely reports a redundant escape character. the escape character is required when running the regular
    // expression on a javascript target. Removing the escape character will result in a `Lone quantifier brackets`
    // syntax error.

    @Suppress("RegExpRedundantEscape")
    val repetition = Regex("""\|([^|]+)\|\[([^\[\]]+)\]""")

    @Suppress("RegExpRedundantEscape")
    val list = Regex("\\|([^|]+)\\|\\{([^\\[\\]]+)\\}")

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
                evaluateExpression(expressionText, constants)?.toString()
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
                        constants + mapOf("index" to index.toDouble(), "it" to value)
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
                    evaluateExpression(exp.groupValues[1], constants + mapOf("it" to repetition.toDouble()))?.toString()
                        ?: error("parse error in repeated expression '$expressionText'")
                }
            }.joinToString(" ")
        }
    } while (curve != referenceCurve)

    /**
     * evaluate expression in expansion
     */
    return (expression.replace(curve) { exp ->
        evaluateExpression(exp.groupValues[1], constants)?.toString() ?: error("parse error in '$curve")
    })
}