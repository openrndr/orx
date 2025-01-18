package org.openrndr.extra.expressions.typed

import org.antlr.v4.kotlinruntime.tree.ParseTreeWalker


/**
 * Compiles a string expression into a single-parameter function capable of evaluating the expression.
 *
 * @param T0 The type of the single parameter that will be passed to the compiled function.
 * @param R The return type of the compiled function.
 * @param expression The string representation of the expression to be compiled.
 * @param parameter0 The name of the required parameter in the expression.
 * @param constants A lambda function to resolve constants in the expression by their names. Defaults to a function returning null for all names.
 * @param functions A set of function extensions that can be used within the expression. Defaults to an empty set.
 * @return A single-parameter function that takes an argument of type [T0] and evaluates the expression to return a result of type [R].
 */
fun <T0, R> compileFunction1(
    expression: String,
    parameter0: String,
    constants: (String) -> Any? = { null },
    functions: TypedFunctionExtensions = TypedFunctionExtensions.EMPTY
): ((T0) -> R) {
//    require(constants(parameter0) == null) {
//        "${parameter0} is in constants with value '${constants(parameter0)}"
//    }
    val root = expressionRoot(expression)

    var varP0: T0? = null
    val constantValues = fun(p: String): Any? {
        return if (p == parameter0) {
            varP0
        } else {
            constants(p)
        }
    }
    val listener = TypedExpressionListener(functions, constantValues)

    return { p0 ->
        varP0 = p0
        try {
            ParseTreeWalker.DEFAULT.walk(listener, root)
        } catch(e: Throwable) {
            throw RuntimeException("Error while evaluating '$expression' with parameter $parameter0=$p0. ${e.message}", e)
        }
        @Suppress("UNCHECKED_CAST")
        listener.state.lastExpressionResult as? R ?: error("No result while evaluating '$expression' with parameter $parameter0=$p0")
    }
}

/**
 * Tries to compile a given string expression into a single-parameter function. Returns the compiled function if successful,
 * or `null` if an error occurs during compilation. Errors are handled using the provided [onError] callback.
 *
 * @param T0 The type of the single parameter that will be passed to the compiled function.
 * @param R The return type of the compiled function.
 * @param expression The string representation of the expression to be compiled.
 * @param parameter0 The name of the required parameter in the expression.
 * @param constants A lambda function to resolve constants in the expression by their names. Defaults to a function returning null for all names.
 * @param functions A set of function extensions that can be used within the expression. Defaults to an empty set.
 * @param onError A callback function that will be invoked when an error occurs during compilation.
 *                The error is passed to this function.
 * @return A single-parameter function that takes an argument of type [T0] and evaluates the expression to return a result of type [R],
 *         or `null` if the compilation fails.
 */
fun <T0, R> compileFunction1OrNull(
    expression: String,
    parameter0: String,
    constants: (String) -> Any? = { null },
    functions: TypedFunctionExtensions = TypedFunctionExtensions.EMPTY,
    onError: (Throwable) -> Unit = { }
): ((T0) -> R)? {
    try {
        return compileFunction1(expression, parameter0, constants, functions)
    } catch (e: Throwable) {
        onError(e)
        return null
    }
}

/**
 * Compiles a string expression into a lambda function with two parameters.
 *
 * This function takes an expression as a string, as well as two parameter names,
 * and returns a lambda that evaluates the expression with the provided parameter values.
 * Optionally, a map of constants and custom functions can be provided for use in the expression.
 *
 * @param T0 The type of the first parameter.
 * @param T1 The type of the second parameter.
 * @param R The return type of the resulting lambda function.
 * @param expression The string expression to compile.
 * @param parameter0 The name of the first parameter in the expression.
 * @param parameter1 The name of the second parameter in the expression.
 * @param constants A lambda function that provides constant values by variable name. Defaults to returning null for all names.
 * @param functions The custom functions available in the context of the expression. Defaults to an empty set of functions.
 * @return A lambda function that takes two parameters of types T0 and T1 and returns a result of type R,
 *         based on the compiled expression.
 * @throws IllegalArgumentException if either parameter name exists within the constants map.
 */
fun <T0, T1, R> compileFunction2(
    expression: String,
    parameter0: String,
    parameter1: String,
    constants: (String) -> Any? = { null },
    functions: TypedFunctionExtensions = TypedFunctionExtensions.EMPTY
): ((T0, T1) -> R) {
    require(constants(parameter0) == null) {
        "${parameter0} is in constants with value '${constants(parameter0)}"
    }
    require(constants(parameter1) == null) {
        "${parameter1} is in constants with value '${constants(parameter1)}"
    }

    val root = expressionRoot(expression)

    var varP0: T0? = null
    var varP1: T1? = null
    val constantValues = fun(p: String): Any? {
        return if (p == parameter0) {
            varP0
        } else if (p == parameter1) {
            varP1
        } else {
            constants(p)
        }
    }
    val listener = TypedExpressionListener(functions, constantValues)

    return { p0, p1 ->
        varP0 = p0
        varP1 = p1
        ParseTreeWalker.DEFAULT.walk(listener, root)
        @Suppress("UNCHECKED_CAST")
        listener.state.lastExpressionResult as? R ?: error("no result")
    }
}

/**
 * Attempts to compile a string expression into a lambda function with two parameters,
 * returning null if an error occurs during compilation.
 *
 * This function is a safe wrapper around `compileFunction2`, catching any exceptions
 * that may be thrown during the compilation process. If an error occurs, the provided
 * `onError` callback is invoked with the exception, and the function returns null.
 *
 * @param T0 The type of the first parameter.
 * @param T1 The type of the second parameter.
 * @param R The return type of the resulting lambda function.
 * @param expression The string expression to compile.
 * @param parameter0 The name of the first parameter in the expression.
 * @param parameter1 The name of the second parameter in the expression.
 * @param constants A lambda function that provides constant values for variables by name. Defaults to returning null for all names.
 * @param functions The custom functions available in the context of the expression. Defaults to an empty set of functions.
 * @param onError A callback invoked with the exception if an error occurs during compilation. Defaults to an empty function.
 * @return A lambda function that takes two parameters of types T0 and T1 and returns a result of type R if the compilation is successful.
 *         Returns null if an error occurs during compilation.
 */
fun <T0, T1, R> compileFunction2OrNull(
    expression: String,
    parameter0: String,
    parameter1: String,
    constants: (String) -> Any? = { null },
    functions: TypedFunctionExtensions = TypedFunctionExtensions.EMPTY,
    onError: (Throwable) -> Unit = { }
): ((T0, T1) -> R)? {
    try {
        return compileFunction2(expression, parameter0, parameter1, constants, functions)
    } catch (e: Throwable) {
        onError(e)
        return null
    }
}

/**
 * Compiles a 3-parameter function from a given string expression, allowing dynamic evaluation with specified parameters, constants, and external function extensions.
 *
 * @param T0 The type of the first parameter.
 * @param T1 The type of the second parameter.
 * @param T2 The type of the third parameter.
 * @param R The return type of the compiled function.
 * @param expression The string representation of the expression to be compiled.
 * @param parameter0 The name of the first parameter referenced in the expression.
 * @param parameter1 The name of the second parameter referenced in the expression.
 * @param parameter2 The name of the third parameter referenced in the expression.
 * @param constants A lambda function providing constant values for variable names used in the expression. Defaults to a function returning null.
 * @param functions An optional container of external functions that can be called within the expression. Defaults to an empty set of functions.
 * @return A lambda function that takes three parameters of types T0, T1, and T2, and returns a result of type R after evaluating the compiled expression.
 * @throws IllegalArgumentException If any of the parameter names are found in the constants map.
 * @throws ExpressionException If there is a syntax error in the provided expression.
 */
fun <T0, T1, T2, R> compileFunction3(
    expression: String,
    parameter0: String,
    parameter1: String,
    parameter2: String,
    constants: (String) -> Any? = { null },
    functions: TypedFunctionExtensions = TypedFunctionExtensions.EMPTY
): ((T0, T1, T2) -> R) {
    require(constants(parameter0) == null) {
        "${parameter0} is in constants with value '${constants(parameter0)}"
    }
    require(constants(parameter1) == null) {
        "${parameter1} is in constants with value '${constants(parameter1)}"
    }
    require(constants(parameter2) == null) {
        "${parameter2} is in constants with value '${constants(parameter2)}"
    }

    val root = expressionRoot(expression)

    var varP0: T0? = null
    var varP1: T1? = null
    var varP2: T2? = null
    val constantValues = fun(p: String): Any? {
        return if (p == parameter0) {
            varP0
        } else if (p == parameter1) {
            varP1
        } else if (p == parameter2) {
            varP2
        } else {
            constants(p)
        }
    }
    val listener = TypedExpressionListener(functions, constantValues)

    return { p0, p1, p2 ->
        varP0 = p0
        varP1 = p1
        varP2 = p2
        ParseTreeWalker.DEFAULT.walk(listener, root)
        @Suppress("UNCHECKED_CAST")
        listener.state.lastExpressionResult as? R ?: error("no result")
    }
}

/**
 * Compiles a 3-parameter function from a given string expression, returning null if an error occurs during compilation.
 *
 * @param T0 The type of the first parameter.
 * @param T1 The type of the second parameter.
 * @param T2 The type of the third parameter.
 * @param R The return type of the compiled function.
 * @param expression The string representation of the expression to be compiled.
 * @param parameter0 The name of the first parameter referenced in the expression.
 * @param parameter1 The name of the second parameter referenced in the expression.
 * @param parameter2 The name of the third parameter referenced in the expression.
 * @param constants A lambda function providing constant values for variable names used in the expression. Defaults to a function returning null.
 * @param functions An optional container of external functions that can be called within the expression. Defaults to an empty set of functions.
 * @param onError A lambda function that will be invoked with the exception if an error occurs during compilation. Defaults to an empty function.
 * @return A lambda function that takes three parameters of types T0, T1, and T2, and returns a result of type R after evaluating the compiled expression, or null if an error occurs
 * .
 */
fun <T0, T1, T2, R> compileFunction3OrNull(
    expression: String,
    parameter0: String,
    parameter1: String,
    parameter2: String,
    constants: (String) -> Any? = { null },
    functions: TypedFunctionExtensions = TypedFunctionExtensions.EMPTY,
    onError: (Throwable) -> Unit = { }
): ((T0, T1, T2) -> R)? {
    try {
        return compileFunction3(expression, parameter0, parameter1, parameter2, constants, functions)
    } catch (e: Throwable) {
        onError(e)
        return null
    }
}