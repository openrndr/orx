package org.openrndr.extra.expressions

import org.antlr.v4.kotlinruntime.*
import org.antlr.v4.kotlinruntime.tree.ParseTreeWalker
import org.antlr.v4.kotlinruntime.tree.TerminalNode
import org.openrndr.extra.expressions.parser.KeyLangLexer
import org.openrndr.extra.expressions.parser.KeyLangParser
import org.openrndr.extra.expressions.parser.KeyLangParserBaseListener

/**
 * A problem in an expression, for an editor to mark: of [kind], in the characters from [start] to [end], end
 * exclusive, with a short [message], and the [name] of the value or function it is about
 */
data class ExpressionProblem(
    val kind: Kind,
    val start: Int,
    val end: Int,
    val message: String,
    val name: String? = null
) {
    enum class Kind {
        /** the expression can not be read, like a missing parenthesis */
        SYNTAX,

        /** a name that is no value */
        UNKNOWN_VALUE,

        /** a name that is no function */
        UNKNOWN_FUNCTION,

        /** a function that takes another number of arguments */
        WRONG_ARGUMENTS,

        /** a value that is declared with `val` a second time, or a parameter of a function twice */
        DUPLICATE_VALUE,

        /** a function that is declared with `fun` a second time, with as many parameters */
        DUPLICATE_FUNCTION,

        /** something the grammar reads that is not evaluated, like a comparison or a list */
        UNSUPPORTED,

        /** something that went wrong while evaluating */
        EVALUATION
    }
}

/** the built-in functions by name, with the numbers of arguments that each takes */
val builtInFunctions: Map<String, Set<Int>> = mapOf(
    "random" to setOf(0, 2),
    "sqrt" to setOf(1), "radians" to setOf(1), "degrees" to setOf(1), "cos" to setOf(1), "sin" to setOf(1),
    "tan" to setOf(1), "atan" to setOf(1), "acos" to setOf(1), "asin" to setOf(1), "exp" to setOf(1),
    "abs" to setOf(1), "floor" to setOf(1), "round" to setOf(1), "ceil" to setOf(1), "saturate" to setOf(1),
    "max" to setOf(2, 3, 4, 5), "min" to setOf(2, 3, 4, 5), "pow" to setOf(2), "mod" to setOf(2),
    "atan2" to setOf(2), "length" to setOf(2, 3, 4), "mix" to setOf(3), "sum" to setOf(3, 4, 5),
    "smoothstep" to setOf(3), "map" to setOf(5)
)

/** the built-in values */
val builtInValues: Set<String> = setOf("PI")

/** the numbers of arguments that [name] takes, of the built-in functions and of [functions] */
internal fun arities(name: String, functions: FunctionExtensions): Set<Int> = builtInFunctions[name].orEmpty() + listOfNotNull(
    0.takeIf { name in functions.functions0 }, 1.takeIf { name in functions.functions1 },
    2.takeIf { name in functions.functions2 }, 3.takeIf { name in functions.functions3 },
    4.takeIf { name in functions.functions4 }, 5.takeIf { name in functions.functions5 }
)

/** the message that [name] takes the numbers of arguments [takes], not [arguments] */
fun argumentsMessage(name: String, takes: Set<Int>, arguments: Int): String {
    fun count(n: Int) = if (n == 1) "1 argument" else "$n arguments"
    val sorted = takes.sorted()
    val allowed = when {
        sorted.size == 1 -> count(sorted[0])
        sorted.zipWithNext().all { (a, b) -> b == a + 1 } -> "${sorted.first()} to ${count(sorted.last())}"
        else -> sorted.dropLast(1).joinToString(", ") + " or " + count(sorted.last())
    }
    return "$name takes $allowed, not $arguments"
}

/** the characters of [token], as the start and the end, exclusive, of an [ExpressionProblem] */
fun span(token: Token): IntRange = token.startIndex until token.stopIndex + 1

/** the characters of [ctx], as the start and the end, exclusive, of an [ExpressionProblem] */
fun span(ctx: ParserRuleContext): IntRange {
    val start = ctx.start?.startIndex ?: 0
    val stop = ctx.stop?.stopIndex ?: (start - 1)
    return start until maxOf(start, stop + 1)
}

/** a short message for a syntax error that ANTLR describes as [message], at [token] */
private fun syntaxMessage(message: String, token: Token?): String {
    val atEnd = token == null || token.type == Token.EOF
    Regex("""^missing (.+?) at .*""").find(message)?.let { return "missing ${it.groupValues[1]}" }
    return if (atEnd) "incomplete expression" else "unexpected '${token.text}'"
}

/** the last opening bracket of [tokens] that is not closed, with the bracket that closes it */
private fun unclosed(tokens: List<Token>): Pair<Token, Char>? {
    val closing = mapOf(KeyLangLexer.Tokens.LPAREN to ')', KeyLangLexer.Tokens.LBRACKET to ']', KeyLangLexer.Tokens.LCURLY to '}')
    val closers = setOf(KeyLangLexer.Tokens.RPAREN, KeyLangLexer.Tokens.RBRACKET, KeyLangLexer.Tokens.RCURLY)
    val stack = ArrayDeque<Token>()
    for (t in tokens) {
        when (t.type) {
            in closing -> stack.addLast(t)
            in closers -> stack.removeLastOrNull()
        }
    }
    return stack.lastOrNull()?.let { it to closing.getValue(it.type) }
}

/**
 * the parse tree of [expression] and its syntax problems, all of them; with [lexer] also the characters that are no
 * tokens. For evaluators of this grammar, like the typed one
 */
fun parseWithProblems(expression: String, lexer: Boolean = true): Pair<KeyLangParser.KeyLangFileContext, List<ExpressionProblem>> {
    val problems = mutableListOf<ExpressionProblem>()
    // the offsets of the lines, for the errors of the lexer, which only know a line and a column
    val lineStarts = listOf(0) + expression.indices.filter { expression[it] == '\n' }.map { it + 1 }
    val keyLangLexer = KeyLangLexer(CharStreams.fromString(expression))
    keyLangLexer.removeErrorListeners()
    if (lexer) keyLangLexer.addErrorListener(object : BaseErrorListener() {
        override fun syntaxError(
            recognizer: Recognizer<*, *>, offendingSymbol: Any?, line: Int, charPositionInLine: Int, msg: String,
            e: RecognitionException?
        ) {
            val start = (lineStarts.getOrNull(line - 1) ?: 0) + charPositionInLine
            val c = expression.getOrNull(start)
            problems.add(ExpressionProblem(ExpressionProblem.Kind.SYNTAX, start, minOf(expression.length, start + 1), "unexpected '${c ?: ""}'"))
        }
    })
    val tokens = CommonTokenStream(keyLangLexer)
    val parser = KeyLangParser(tokens)
    // the token of the last error; recovering from an error, the parser can go back over tokens and report one of
    // those, which is fine, again
    var lastErrorToken = -1
    parser.removeErrorListeners()
    parser.addErrorListener(object : BaseErrorListener() {
        override fun syntaxError(
            recognizer: Recognizer<*, *>, offendingSymbol: Any?, line: Int, charPositionInLine: Int, msg: String,
            e: RecognitionException?
        ) {
            val token = offendingSymbol as? Token
            val index = token?.tokenIndex ?: Int.MAX_VALUE
            if (index <= lastErrorToken) return
            lastErrorToken = index
            val atEnd = token == null || token.type == Token.EOF
            // at the end with a bracket that is not closed: that bracket, which is missing its closing one
            val open = if (atEnd) unclosed(tokens.tokens) else null
            if (open != null) {
                val range = span(open.first)
                problems.add(ExpressionProblem(ExpressionProblem.Kind.SYNTAX, range.first, range.last + 1, "missing '${open.second}'"))
                return
            }
            // at the end, the last token before it
            val at = if (atEnd) {
                (tokens.tokens.lastOrNull { it.type != Token.EOF && it.channel == Token.DEFAULT_CHANNEL && it.type != KeyLangLexer.Tokens.NEWLINE && it.type != KeyLangLexer.Tokens.SEMICOLON })
            } else token
            val range = at?.let(::span) ?: (expression.length until expression.length)
            problems.add(ExpressionProblem(ExpressionProblem.Kind.SYNTAX, range.first, range.last + 1, syntaxMessage(msg, token)))
        }
    })
    val root = parser.keyLangFile()
    return root to problems.sortedBy { it.start }
}

/**
 * A walk over a program that knows the values and functions it declares, in the program and in the bodies of its
 * functions, from the statement after the declaration on; [declaredTwice] is called with the names of the values,
 * parameters and functions that are declared again, with the kind of the problem
 */
private open class DeclarationWalk(
    val declaredTwice: (TerminalNode, ExpressionProblem.Kind) -> Unit = { _, _ -> }
) : KeyLangParserBaseListener() {
    private class Scope {
        val values = mutableSetOf<String>()
        val functions = mutableSetOf<Pair<String, Int>>()
    }

    private val scopes = ArrayList<Scope>().apply { add(Scope()) }

    private fun name(id: TerminalNode) = id.text.replace("`", "")

    /** whether [name] is a value declared before, or a parameter of a function around it */
    fun isDeclaredValue(name: String) = scopes.any { name in it.values }

    /** the numbers of parameters of the functions [name] declared before */
    fun declaredArities(name: String): Set<Int> = scopes.flatMap { s -> s.functions.filter { it.first == name }.map { it.second } }.toSet()

    override fun exitValDeclaration(ctx: KeyLangParser.ValDeclarationContext) {
        val id = ctx.ID() ?: return
        if (!scopes.last().values.add(name(id))) declaredTwice(id, ExpressionProblem.Kind.DUPLICATE_VALUE)
    }

    override fun enterFunDeclaration(ctx: KeyLangParser.FunDeclarationContext) {
        val body = Scope()
        for (parameter in ctx.ID().drop(1)) {
            if (!body.values.add(name(parameter))) declaredTwice(parameter, ExpressionProblem.Kind.DUPLICATE_VALUE)
        }
        scopes.add(body)
    }

    // the function is declared after its body, which can not call it
    override fun exitFunDeclaration(ctx: KeyLangParser.FunDeclarationContext) {
        scopes.removeAt(scopes.lastIndex)
        val id = ctx.ID(0) ?: return
        if (!scopes.last().functions.add(name(id) to ctx.ID().size - 1)) declaredTwice(id, ExpressionProblem.Kind.DUPLICATE_FUNCTION)
    }
}

/**
 * The problems of [expression], all of them, without evaluating it: syntax errors, names that are neither in
 * [values], built-in values, parameters nor values declared before them, calls of functions that are neither built
 * in, in [functions] nor declared before them, or that take other numbers of arguments, values and functions declared
 * twice, functions of more than 5 parameters, and what is read but not evaluated. Names of values are only checked
 * when the expression can be read.
 */
fun checkExpression(
    expression: String,
    values: Set<String>,
    functions: FunctionExtensions = FunctionExtensions.EMPTY
): List<ExpressionProblem> {
    val (root, syntax) = parseWithProblems(expression)
    if (syntax.isNotEmpty()) return syntax
    val problems = mutableListOf<ExpressionProblem>()
    fun add(kind: ExpressionProblem.Kind, range: IntRange, message: String, name: String? = null) {
        problems.add(ExpressionProblem(kind, range.first, range.last + 1, message, name))
    }

    fun unsupported(ctx: ParserRuleContext, what: String) = add(ExpressionProblem.Kind.UNSUPPORTED, span(ctx), "$what not supported")

    ParseTreeWalker.DEFAULT.walk(object : DeclarationWalk({ id, kind ->
        val name = id.text.replace("`", "")
        add(kind, span(id.symbol), "$name is declared already", name)
    }) {
        fun call(ctx: ParserRuleContext, id: TerminalNode?, arguments: Int) {
            val token = id?.symbol ?: return
            val name = token.text?.replace("`", "") ?: return
            val takes = arities(name, functions) + declaredArities(name)
            when {
                arguments in takes -> {}
                takes.isEmpty() -> add(ExpressionProblem.Kind.UNKNOWN_FUNCTION, span(token), "no function $name", name)
                else -> add(ExpressionProblem.Kind.WRONG_ARGUMENTS, span(ctx), argumentsMessage(name, takes, arguments), name)
            }
        }

        override fun enterValueReference(ctx: KeyLangParser.ValueReferenceContext) {
            val token = ctx.ID()?.symbol ?: return
            val name = token.text?.replace("`", "") ?: return
            if (name !in values && name !in builtInValues && !isDeclaredValue(name)) add(ExpressionProblem.Kind.UNKNOWN_VALUE, span(token), "no value $name", name)
        }

        override fun enterFunDeclaration(ctx: KeyLangParser.FunDeclarationContext) {
            super.enterFunDeclaration(ctx)
            if (ctx.ID().size > 6) unsupported(ctx, "functions of more than 5 parameters are")
        }

        override fun enterFunctionCall0Expression(ctx: KeyLangParser.FunctionCall0ExpressionContext) = call(ctx, ctx.ID(), 0)
        override fun enterFunctionCall1Expression(ctx: KeyLangParser.FunctionCall1ExpressionContext) = call(ctx, ctx.ID(), 1)
        override fun enterFunctionCall2Expression(ctx: KeyLangParser.FunctionCall2ExpressionContext) = call(ctx, ctx.ID(), 2)
        override fun enterFunctionCall3Expression(ctx: KeyLangParser.FunctionCall3ExpressionContext) = call(ctx, ctx.ID(), 3)
        override fun enterFunctionCall4Expression(ctx: KeyLangParser.FunctionCall4ExpressionContext) = call(ctx, ctx.ID(), 4)
        override fun enterFunctionCall5Expression(ctx: KeyLangParser.FunctionCall5ExpressionContext) = call(ctx, ctx.ID(), 5)

        override fun enterRangeExpression(ctx: KeyLangParser.RangeExpressionContext) = unsupported(ctx, "ranges are")
        override fun enterListLiteral(ctx: KeyLangParser.ListLiteralContext) = unsupported(ctx, "lists are")
        override fun enterLambdaExpression(ctx: KeyLangParser.LambdaExpressionContext) = unsupported(ctx, "lambdas are")
        override fun enterStringLiteral(ctx: KeyLangParser.StringLiteralContext) = unsupported(ctx, "strings are")
        override fun enterIndexExpression(ctx: KeyLangParser.IndexExpressionContext) = unsupported(ctx, "indexing is")
        override fun enterPropReference(ctx: KeyLangParser.PropReferenceContext) = unsupported(ctx, "properties are")
        override fun enterMemberFunctionCall0Expression(ctx: KeyLangParser.MemberFunctionCall0ExpressionContext) = unsupported(ctx, "member functions are")
        override fun enterMemberFunctionCall0LambdaExpression(ctx: KeyLangParser.MemberFunctionCall0LambdaExpressionContext) = unsupported(ctx, "member functions are")
        override fun enterMemberFunctionCall1Expression(ctx: KeyLangParser.MemberFunctionCall1ExpressionContext) = unsupported(ctx, "member functions are")
        override fun enterMemberFunctionCall2Expression(ctx: KeyLangParser.MemberFunctionCall2ExpressionContext) = unsupported(ctx, "member functions are")
        override fun enterMemberFunctionCall3Expression(ctx: KeyLangParser.MemberFunctionCall3ExpressionContext) = unsupported(ctx, "member functions are")
        override fun enterMemberFunctionCall4Expression(ctx: KeyLangParser.MemberFunctionCall4ExpressionContext) = unsupported(ctx, "member functions are")
        override fun enterNegateExpression(ctx: KeyLangParser.NegateExpressionContext) = unsupported(ctx, "'!' is")
        override fun enterComparisonOperation(ctx: KeyLangParser.ComparisonOperationContext) = unsupported(ctx, "comparisons are")
        override fun enterJoinOperation(ctx: KeyLangParser.JoinOperationContext) = unsupported(ctx, "'&&' and '||' are")
        override fun enterTernaryExpression(ctx: KeyLangParser.TernaryExpressionContext) = unsupported(ctx, "'? :' is")
    }, root)
    return problems.sortedBy { it.start }
}

/**
 * the names of the values that [expression] reads, without the built-in values, the parameters of its functions and
 * the values it declares before reading them; also of an expression with syntax errors, as far as it can be read
 */
fun expressionNames(expression: String): Set<String> {
    val (root, problems) = parseWithProblems(expression, lexer = false)
    val names = linkedSetOf<String>()
    if (problems.isNotEmpty()) {
        // the tree of an expression that can not be read misses parts: the names that are not called or properties
        val tokens = CommonTokenStream(KeyLangLexer(CharStreams.fromString(expression)).also { it.removeErrorListeners() })
        tokens.fill()
        val visible = tokens.tokens.filter { it.channel == Token.DEFAULT_CHANNEL }
        // the values declared so far, of which a name is read from the next statement on, and the parameters of the
        // functions, which are taken for declared from the function on
        val declared = mutableSetOf<String>()
        var declaring: String? = null
        var parameters = false
        for ((i, t) in visible.withIndex()) {
            if (t.type == KeyLangLexer.Tokens.NEWLINE || t.type == KeyLangLexer.Tokens.SEMICOLON) {
                declaring?.let(declared::add)
                declaring = null
            }
            if (t.type == KeyLangLexer.Tokens.FUN) parameters = true
            if (t.type == KeyLangLexer.Tokens.RPAREN) parameters = false
            if (t.type != KeyLangLexer.Tokens.ID) continue
            val name = t.text?.replace("`", "") ?: continue
            if (visible.getOrNull(i - 1)?.type == KeyLangLexer.Tokens.VAL) {
                declaring = name
                continue
            }
            if (parameters && visible.getOrNull(i + 1)?.type != KeyLangLexer.Tokens.LPAREN) {
                declared.add(name)
                continue
            }
            if (visible.getOrNull(i + 1)?.type == KeyLangLexer.Tokens.LPAREN || visible.getOrNull(i - 1)?.type == KeyLangLexer.Tokens.DOT) continue
            if (name !in builtInValues && name !in declared) names.add(name)
        }
        return names
    }
    ParseTreeWalker.DEFAULT.walk(object : DeclarationWalk() {
        override fun enterValueReference(ctx: KeyLangParser.ValueReferenceContext) {
            val name = ctx.ID()?.symbol?.text?.replace("`", "") ?: return
            if (name !in builtInValues && !isDeclaredValue(name)) names.add(name)
        }
    }, root)
    return names
}
