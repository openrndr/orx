package org.openrndr.extra.expressions



import org.antlr.v4.kotlinruntime.*
import org.openrndr.extra.expressions.parser.KeyLangParser


typealias Function0 = () -> Double
typealias Function1 = (Double) -> Double
typealias Function2 = (Double, Double) -> Double
typealias Function3 = (Double, Double, Double) -> Double
typealias Function4 = (Double, Double, Double, Double) -> Double
typealias Function5 = (Double, Double, Double, Double, Double) -> Double

class FunctionExtensions(
    val functions0: Map<String, Function0> = emptyMap(),
    val functions1: Map<String, Function1> = emptyMap(),
    val functions2: Map<String, Function2> = emptyMap(),
    val functions3: Map<String, Function3> = emptyMap(),
    val functions4: Map<String, Function4> = emptyMap(),
    val functions5: Map<String, Function5> = emptyMap()
) {
    companion object {
        val EMPTY = FunctionExtensions()
    }
}

/**
 * An expression could not be read or evaluated, see [problems] for where; the message is for people, the problems for
 * an editor to mark
 */
class ExpressionException(
    message: String,
    /** what is wrong and where, at least one */
    val problems: List<ExpressionProblem> = listOf(ExpressionProblem(ExpressionProblem.Kind.EVALUATION, 0, 0, message))
) : RuntimeException(message)

/** the parse tree of [expression], or an [ExpressionException] with all its syntax problems */
private fun parseOrThrow(expression: String): KeyLangParser.KeyLangFileContext {
    val (root, problems) = parseWithProblems(expression, lexer = false)
    if (problems.isNotEmpty()) {
        val first = problems.first()
        val line = expression.take(first.start).count { it == '\n' } + 1
        val character = first.start - (expression.lastIndexOf('\n', first.start - 1) + 1)
        throw ExpressionException("parser error in expression: '$expression'; [line: $line, character: $character]: ${first.message}", problems)
    }
    return root
}

fun evaluateExpression(
    expression: String,
    constants: Map<String, Double> = emptyMap(),
    functions: FunctionExtensions = FunctionExtensions.EMPTY
): Double? {
    return CompiledExpression(parseOrThrow(expression), constants, functions).evaluate()
}

fun compileExpression(
    expression: String,
    constants: Map<String, Double> = emptyMap(),
    functions: FunctionExtensions = FunctionExtensions.EMPTY
): () -> Double {
    val compiled = CompiledExpression(parseOrThrow(expression), constants, functions)
    return { compiled.evaluate() ?: error("no result") }
}

internal fun expressionRoot(expression: String): KeyLangParser.KeyLangFileContext {
    return parseOrThrow(expression)
}

