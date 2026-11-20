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
 * A walk over a typed expression that knows the parameters of the lambdas and functions around every name, and the
 * values and functions declared before it: [named] is called with every name of a value that is neither a parameter
 * nor declared, [called] with every call of a function that is not declared with as many parameters, with its number
 * of arguments and the numbers of parameters of the functions of its name that are declared, [declaredTwice] with
 * every name of a value, parameter or function that is declared again, with the kind of the problem, [function] with
 * every declaration of a function, with its number of parameters
 */
private class NameWalk(
    val named: (TerminalNode) -> Unit,
    val called: (ParserRuleContext, TerminalNode, Int, Set<Int>) -> Unit = { _, _, _, _ -> },
    val lambda: (KeyLangParser.FunctionLiteralContext, Int) -> Unit = { _, _ -> },
    val declaredTwice: (TerminalNode, ExpressionProblem.Kind) -> Unit = { _, _ -> },
    val function: (KeyLangParser.FunDeclarationContext, Int) -> Unit = { _, _ -> }
) : KeyLangParserBaseListener() {
    /** the parameters of the lambdas around what is walked */
    private val scopes = ArrayList<List<String>>()

    /** the values and functions declared in the program and the bodies of the functions around what is walked */
    private class Declarations {
        val values = mutableSetOf<String>()
        val functions = mutableSetOf<Pair<String, Int>>()
    }

    private val declarations = ArrayList<Declarations>().apply { add(Declarations()) }

    private fun name(id: TerminalNode) = id.text.replace("`", "")

    override fun exitValDeclaration(ctx: KeyLangParser.ValDeclarationContext) {
        val id = ctx.ID() ?: return
        if (!declarations.last().values.add(name(id))) declaredTwice(id, ExpressionProblem.Kind.DUPLICATE_VALUE)
    }

    override fun enterFunDeclaration(ctx: KeyLangParser.FunDeclarationContext) {
        function(ctx, ctx.ID().size - 1)
        val body = Declarations()
        for (parameter in ctx.ID().drop(1)) {
            if (!body.values.add(name(parameter))) declaredTwice(parameter, ExpressionProblem.Kind.DUPLICATE_VALUE)
        }
        declarations.add(body)
    }

    // the function is declared after its body, which can not call it
    override fun exitFunDeclaration(ctx: KeyLangParser.FunDeclarationContext) {
        declarations.removeAt(declarations.lastIndex)
        val id = ctx.ID(0) ?: return
        if (!declarations.last().functions.add(name(id) to ctx.ID().size - 1)) declaredTwice(id, ExpressionProblem.Kind.DUPLICATE_FUNCTION)
    }

    private fun call(ctx: ParserRuleContext, id: TerminalNode, n: Int) {
        val declared = declarations.flatMap { d -> d.functions.filter { it.first == name(id) }.map { it.second } }.toSet()
        if (n !in declared) called(ctx, id, n, declared)
    }

    override fun enterFunctionLiteral(ctx: KeyLangParser.FunctionLiteralContext) {
        val names = ctx.ID().map { it.text.replace("`", "") }
        lambda(ctx, names.size)
        scopes.add(names.ifEmpty { listOf("it") })
        // what the body declares is declared in it
        declarations.add(Declarations())
    }

    override fun exitFunctionLiteral(ctx: KeyLangParser.FunctionLiteralContext) {
        declarations.removeAt(declarations.lastIndex)
        scopes.removeAt(scopes.lastIndex)
    }

    override fun enterValueReference(ctx: KeyLangParser.ValueReferenceContext) {
        val id = ctx.ID() ?: return
        val name = id.text.replace("`", "")
        if (name == "PI" || declarations.any { name in it.values } || scopes.any { name in it }) return
        named(id)
    }

    override fun enterFunctionCall0Expression(ctx: KeyLangParser.FunctionCall0ExpressionContext) = call(ctx, ctx.ID()!!, 0)
    override fun enterFunctionCall1Expression(ctx: KeyLangParser.FunctionCall1ExpressionContext) = call(ctx, ctx.ID()!!, 1)
    override fun enterFunctionCall2Expression(ctx: KeyLangParser.FunctionCall2ExpressionContext) = call(ctx, ctx.ID()!!, 2)
    override fun enterFunctionCall3Expression(ctx: KeyLangParser.FunctionCall3ExpressionContext) = call(ctx, ctx.ID()!!, 3)
    override fun enterFunctionCall4Expression(ctx: KeyLangParser.FunctionCall4ExpressionContext) = call(ctx, ctx.ID()!!, 4)
    override fun enterFunctionCall5Expression(ctx: KeyLangParser.FunctionCall5ExpressionContext) = call(ctx, ctx.ID()!!, 5)
}

/**
 * The problems of the typed [expression], all of them, without evaluating it: syntax errors, names that are neither
 * in [values], parameters of the lambdas and functions around them, values declared before them nor PI, values and
 * functions declared twice, functions of more than 5 parameters, calls of functions that are neither built in, in
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
        called = { ctx, id, n, declared ->
            val name = id.text.replace("`", "")
            val takes = typedArities(name, functions) + declared
            when {
                n in takes || name in values -> {}
                takes.isEmpty() -> add(ExpressionProblem.Kind.UNKNOWN_FUNCTION, span(id.symbol), "no function $name", name)
                else -> add(ExpressionProblem.Kind.WRONG_ARGUMENTS, span(ctx), argumentsMessage(name, takes, n), name)
            }
        },
        lambda = { ctx, n ->
            if (n > 2) add(ExpressionProblem.Kind.UNSUPPORTED, span(ctx), "lambdas with $n parameters are not supported")
        },
        declaredTwice = { id, kind ->
            val name = id.text.replace("`", "")
            add(kind, span(id.symbol), "$name is declared already", name)
        },
        function = { ctx, n ->
            if (n > 5) add(ExpressionProblem.Kind.UNSUPPORTED, span(ctx), "functions of more than 5 parameters are not supported")
        }
    ), root)
    return problems.sortedBy { it.start }
}

/**
 * the names of the values that the typed [expression] reads, without the parameters of its lambdas and functions, the
 * values it declares before reading them and PI, as far as it can be read
 */
fun typedExpressionNames(expression: String): Set<String> {
    val (root, _) = parseWithProblems(expression, lexer = false)
    val names = linkedSetOf<String>()
    ParseTreeWalker.DEFAULT.walk(NameWalk(named = { names.add(it.text.replace("`", "")) }), root)
    return names
}
