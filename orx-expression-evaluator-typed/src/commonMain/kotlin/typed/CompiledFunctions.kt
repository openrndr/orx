package org.openrndr.extra.expressions.typed

import org.antlr.v4.kotlinruntime.tree.ParseTreeWalker
import org.openrndr.extra.expressions.ExpressionException

fun <T0, R> compileFunction1OrNull(
    expression: String,
    parameter0: String,
    constants: (String)->Any? = { null },
    functions: TypedFunctionExtensions = TypedFunctionExtensions.EMPTY
): ((T0) -> R)? {
    require(constants(parameter0) == null) {
        "${parameter0} is in constants with value '${constants(parameter0)}"
    }
    try {
        val root = org.openrndr.extra.expressions.typed.expressionRoot(expression)

        var varP0: T0? = null
        val variables = fun(p : String) : Any? {
            return if (p == parameter0) {
                varP0
            } else {
                constants(p)
            }
        }
        val listener = TypedExpressionListener(functions, variables)

        return { p0 ->
                varP0 = p0
                ParseTreeWalker.DEFAULT.walk(listener, root)
                listener.lastExpressionResult as? R ?: error("no result")

        }
    } catch (e: ExpressionException) {
        return null
    }
}
