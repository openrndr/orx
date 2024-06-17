package org.openrndr.extra.expressions.typed

import org.antlr.v4.kotlinruntime.tree.ParseTreeWalker

/**
 * Compile a function
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
        ParseTreeWalker.DEFAULT.walk(listener, root)
        @Suppress("UNCHECKED_CAST")
        listener.state.lastExpressionResult as? R ?: error("no result")
    }
}

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

//

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

//

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