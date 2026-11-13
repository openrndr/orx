package org.openrndr.extra.expressions.typed

import org.antlr.v4.kotlinruntime.ParserRuleContext
import org.antlr.v4.kotlinruntime.tree.ParseTreeWalker
import org.antlr.v4.kotlinruntime.tree.TerminalNode
import org.openrndr.extra.expressions.ExpressionProblem
import org.openrndr.extra.expressions.argumentsMessage
import org.openrndr.extra.expressions.parseWithProblems
import org.openrndr.extra.expressions.parser.KeyLangParser
import org.openrndr.extra.expressions.parser.KeyLangParserBaseListener
import org.openrndr.extra.expressions.span

/** the numbers of arguments that the built-in function or the function of [functions] [name] takes */
private fun typedArities(name: String, functions: TypedFunctionExtensions): Set<Int> = buildSet {
    if (dispatchFunction0(name, functions.functions0) != null) add(0)
    if (dispatchFunction1(name, functions.functions1) != null) add(1)
    if (dispatchFunction2(name, functions.functions2) != null) add(2)
    if (dispatchFunction3(name, functions.functions3) != null) add(3)
    if (dispatchFunction4(name, functions.functions4) != null) add(4)
    if (name in functions.functions5) add(5)
}

/**
 * A walk over a typed expression that knows the parameters of the lambdas around every name: [named] is called with
 * every name of a value that is not a parameter, [called] with every call of a function, with its number of arguments
 */
private class NameWalk(
    val named: (TerminalNode) -> Unit,
    val called: (ParserRuleContext, TerminalNode, Int) -> Unit = { _, _, _ -> },
    val lambda: (KeyLangParser.FunctionLiteralContext, Int) -> Unit = { _, _ -> }
) : KeyLangParserBaseListener() {
    private val scopes = ArrayList<List<String>>()

    override fun enterFunctionLiteral(ctx: KeyLangParser.FunctionLiteralContext) {
        val names = ctx.ID().map { it.text.replace("`", "") }
        lambda(ctx, names.size)
        scopes.add(names.ifEmpty { listOf("it") })
    }

    override fun exitFunctionLiteral(ctx: KeyLangParser.FunctionLiteralContext) {
        scopes.removeAt(scopes.lastIndex)
    }

    override fun enterValueReference(ctx: KeyLangParser.ValueReferenceContext) {
        val id = ctx.ID() ?: return
        val name = id.text.replace("`", "")
        if (name == "PI" || scopes.any { name in it }) return
        named(id)
    }

    override fun enterFunctionCall0Expression(ctx: KeyLangParser.FunctionCall0ExpressionContext) = called(ctx, ctx.ID()!!, 0)
    override fun enterFunctionCall1Expression(ctx: KeyLangParser.FunctionCall1ExpressionContext) = called(ctx, ctx.ID()!!, 1)
    override fun enterFunctionCall2Expression(ctx: KeyLangParser.FunctionCall2ExpressionContext) = called(ctx, ctx.ID()!!, 2)
    override fun enterFunctionCall3Expression(ctx: KeyLangParser.FunctionCall3ExpressionContext) = called(ctx, ctx.ID()!!, 3)
    override fun enterFunctionCall4Expression(ctx: KeyLangParser.FunctionCall4ExpressionContext) = called(ctx, ctx.ID()!!, 4)
    override fun enterFunctionCall5Expression(ctx: KeyLangParser.FunctionCall5ExpressionContext) = called(ctx, ctx.ID()!!, 5)
}

/**
 * The problems of the typed [expression], all of them, without evaluating it: syntax errors, names that are neither
 * in [values], parameters of the lambdas around them nor PI, calls of functions that are neither built in, in
 * [functions] nor in [values] (a constant can be a function), or that take other numbers of arguments, and lambdas of
 * more than two parameters. The types of values are not known before they are evaluated, so the properties and
 * functions of values, and the types of operands, are not checked. Names are only checked when the expression can be
 * read.
 */
fun checkTypedExpression(
    expression: String,
    values: Set<String>,
    functions: TypedFunctionExtensions = TypedFunctionExtensions.EMPTY
): List<ExpressionProblem> {
    val (root, syntax) = parseWithProblems(expression)
    if (syntax.isNotEmpty()) return syntax
    val problems = mutableListOf<ExpressionProblem>()
    fun add(kind: ExpressionProblem.Kind, range: IntRange, message: String, name: String? = null) {
        problems.add(ExpressionProblem(kind, range.first, range.last + 1, message, name))
    }
    ParseTreeWalker.DEFAULT.walk(NameWalk(
        named = { id ->
            val name = id.text.replace("`", "")
            if (name !in values) add(ExpressionProblem.Kind.UNKNOWN_VALUE, span(id.symbol), "no value $name", name)
        },
        called = { ctx, id, n ->
            val name = id.text.replace("`", "")
            val takes = typedArities(name, functions)
            when {
                n in takes || name in values -> {}
                takes.isEmpty() -> add(ExpressionProblem.Kind.UNKNOWN_FUNCTION, span(id.symbol), "no function $name", name)
                else -> add(ExpressionProblem.Kind.WRONG_ARGUMENTS, span(ctx), argumentsMessage(name, takes, n), name)
            }
        },
        lambda = { ctx, n ->
            if (n > 2) add(ExpressionProblem.Kind.UNSUPPORTED, span(ctx), "lambdas with $n parameters are not supported")
        }
    ), root)
    return problems.sortedBy { it.start }
}

/**
 * the names of the values that the typed [expression] reads, without the parameters of its lambdas and PI, as far as
 * it can be read
 */
fun typedExpressionNames(expression: String): Set<String> {
    val (root, _) = parseWithProblems(expression, lexer = false)
    val names = linkedSetOf<String>()
    ParseTreeWalker.DEFAULT.walk(NameWalk(named = { names.add(it.text.replace("`", "")) }), root)
    return names
}
