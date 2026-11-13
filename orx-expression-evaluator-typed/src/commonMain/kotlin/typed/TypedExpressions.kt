package org.openrndr.extra.expressions.typed

import org.openrndr.extra.expressions.ExpressionProblem
import org.openrndr.extra.expressions.parseWithProblems
import org.openrndr.extra.expressions.parser.KeyLangParser

typealias TypedFunction0 = () -> Any
typealias TypedFunction1 = (Any) -> Any
typealias TypedFunction2 = (Any, Any) -> Any
typealias TypedFunction3 = (Any, Any, Any) -> Any
typealias TypedFunction4 = (Any, Any, Any, Any) -> Any
typealias TypedFunction5 = (Any, Any, Any, Any, Any) -> Any


class TypedFunctionExtensions(
    val functions0: Map<String, TypedFunction0> = emptyMap(),
    val functions1: Map<String, TypedFunction1> = emptyMap(),
    val functions2: Map<String, TypedFunction2> = emptyMap(),
    val functions3: Map<String, TypedFunction3> = emptyMap(),
    val functions4: Map<String, TypedFunction4> = emptyMap(),
    val functions5: Map<String, TypedFunction5> = emptyMap()
) {
    companion object {
        val EMPTY = TypedFunctionExtensions()
    }
}

/**
 * An expression could not be read or evaluated, see [ExpressionProblem]s for where: the exception of the untyped
 * evaluator, so that both are caught alike
 */
typealias ExpressionException = org.openrndr.extra.expressions.ExpressionException

/**
 * Evaluates a typed expression based on the provided string input, constants,
 * and function definitions.
 *
 * @param expression The string representation of the expression to evaluate.
 * @param constants A lambda function providing constant values for specific
 * variables. Returns null if a constant is not found. Defaults to a function
 * returning null for any input.
 * @param functions A `TypedFunctionExtensions` instance encapsulating function
 * definitions for 0 to 5 arguments. Defaults to an empty set of functions.
 * @return The result of the evaluated expression as an `Any?` type.
 *         Returns null if the evaluation produces no result.
 * @throws ExpressionException If a syntax error occurs in the input expression
 * or during expression evaluation.
 */
fun evaluateTypedExpression(
    expression: String,
    constants: (String) -> Any? = { null },
    functions: TypedFunctionExtensions = TypedFunctionExtensions.EMPTY
): Any? = TypedCompiledExpression(expressionRoot(expression), constants, functions).evaluate()

/**
 * Compiles a typed expression and returns a lambda that can execute the compiled expression.
 *
 * @param expression The string representation of the expression to compile.
 * @param constants A lambda function to resolve constants by their names. Defaults to a resolver that returns null.
 * @param functions An instance of `TypedFunctionExtensions` containing the supported custom functions for the expression. Defaults to an empty set of functions.
 * @return A lambda function that evaluates the compiled expression and returns its result.
 * @throws ExpressionException If there is a syntax error or a parsing issue in the provided expression.
 */
fun compileTypedExpression(
    expression: String,
    constants: (String) -> Any? = { null },
    functions: TypedFunctionExtensions = TypedFunctionExtensions.EMPTY
): () -> Any {
    val compiled = TypedCompiledExpression(expressionRoot(expression), constants, functions)
    return { compiled.evaluate() ?: error("no result") }
}

/** the parse tree of [expression], or an [ExpressionException] with all its syntax problems */
internal fun expressionRoot(expression: String): KeyLangParser.KeyLangFileContext {
    val (root, problems) = parseWithProblems(expression, lexer = false)
    if (problems.isNotEmpty()) {
        val first = problems.first()
        val line = expression.take(first.start).count { it == '\n' } + 1
        val character = first.start - (expression.lastIndexOf('\n', first.start - 1) + 1)
        throw ExpressionException("parser error in expression: '$expression'; [line: $line, character: $character]: ${first.message}", problems)
    }
    return root
}
