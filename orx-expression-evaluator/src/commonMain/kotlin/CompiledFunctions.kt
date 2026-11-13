package org.openrndr.extra.expressions



/**
 * Compile a (Double)->Double function from an expression string
 * @param expression the expression string to be compiled
 * @param parameter0 the name of the first parameter
 * @param constants a map of named constant values that can be referred from the expression
 * @param functions a map of named functions that can be invoked from the expression
 * @param error in case the expression fails to compile or evaluate, this function is invoked instead
 */
fun compileFunction1(
    expression: String,
    parameter0: String,
    constants: Map<String, Double> = mapOf(),
    functions: FunctionExtensions = FunctionExtensions.EMPTY,
    error: (Double) -> Double = { 0.0 },
): (Double) -> Double {
    require(!constants.containsKey(parameter0))
    try {
        val root = expressionRoot(expression)
        val variables = mutableMapOf<String, Double>()
        variables.putAll(constants)
        val compiled = CompiledExpression(root, variables, functions)

        return { p0 ->
            variables[parameter0] = p0
            try {
                compiled.evaluate() ?: error("no result")
            } catch (e: ExpressionException) {
                error(p0)
            }
        }
    } catch (e: ExpressionException) {
        return error
    }
}

/**
 * Compile a (Double, Double)->Double function from an expression string
 * @param expression the expression string to be compiled
 * @param parameter0 the name of the first parameter
 * @param parameter1 the name of the second parameter
 * @param constants a map of named constant values that can be referred from the expression
 * @param functions a map of named functions that can be invoked from the expression
 * @param error in case the expression fails to compile or evaluate, this function is invoked instead
 */
fun compileFunction2(
    expression: String,
    parameter0: String,
    parameter1: String,
    constants: Map<String, Double> = mapOf(),
    functions: FunctionExtensions = FunctionExtensions.EMPTY,
    error: (p0: Double, p1: Double) -> Double = { _, _ -> 0.0 },
): (p0: Double, p1: Double) -> Double {
    require(!constants.containsKey(parameter0))
    require(!constants.containsKey(parameter1))
    try {
        val root = expressionRoot(expression)
        val variables = mutableMapOf<String, Double>()
        variables.putAll(constants)
        val compiled = CompiledExpression(root, variables, functions)

        return { p0, p1 ->
            variables[parameter0] = p0
            variables[parameter1] = p1
            try {
                compiled.evaluate() ?: error("no result")
            } catch (e: ExpressionException) {
                error(p0, p1)
            }
        }
    } catch (e: ExpressionException) {
        return error
    }
}

/**
 * Compile a (Double, Double, Double)->Double function from an expression string
 * @param expression the expression string to be compiled
 * @param parameter0 the name of the first parameter
 * @param parameter1 the name of the second parameter
 * @param parameter2 the name of the third parameter
 * @param constants a map of named constant values that can be referred from the expression
 * @param functions a map of named functions that can be invoked from the expression
 * @param error in case the expression fails to compile or evaluate, this function is invoked instead
 */
fun compileFunction3(
    expression: String,
    parameter0: String,
    parameter1: String,
    parameter2: String,
    constants: Map<String, Double> = mapOf(),
    functions: FunctionExtensions = FunctionExtensions.EMPTY,
    error: (p0: Double, p1: Double, p2: Double) -> Double = { _, _, _ -> 0.0 }
): (p0: Double, p1: Double, p2: Double) -> Double {
    require(!constants.containsKey(parameter0))
    require(!constants.containsKey(parameter1))
    require(!constants.containsKey(parameter2))

    try {
        val root = expressionRoot(expression)
        val variables = mutableMapOf<String, Double>()
        variables.putAll(constants)
        val compiled = CompiledExpression(root, variables, functions)

        return { p0, p1, p2 ->
            variables[parameter0] = p0
            variables[parameter1] = p1
            variables[parameter2] = p2
            try {
                compiled.evaluate() ?: error("no result")
            } catch (e: ExpressionException) {
                error(p0, p1, p2)
            }
        }
    } catch(e: ExpressionException) {
        return error
    }
}
