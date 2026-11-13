package org.openrndr.extra.expressions

import org.antlr.v4.kotlinruntime.ParserRuleContext
import org.antlr.v4.kotlinruntime.tree.TerminalNode
import org.openrndr.extra.expressions.parser.KeyLangParser
import org.openrndr.extra.noise.uniform
import org.openrndr.math.*
import kotlin.math.*

/**
 * An expression compiled from its parse tree to closures, once: evaluating it calls them, without walking the tree.
 * Values are read from [constants] at every evaluation, so a map that changes between evaluations is followed.
 * Functions are found when the expression is compiled, those that are not there then at every evaluation.
 *
 * Like the evaluation of the tree did, a name that can not be found evaluates to NaN and is reported once the whole
 * expression is evaluated, with the problems of all the names that could not be found.
 */
internal class CompiledExpression(
    root: KeyLangParser.KeyLangFileContext,
    private val constants: Map<String, Double>,
    private val functions: FunctionExtensions
) {
    /** a part of the expression, evaluated */
    private fun interface Node {
        fun eval(): Double
    }

    /** the errors of the current evaluation */
    private val errors = ArrayList<ExpressionException>()

    /** the statements, of which the last gives the result, with their texts for the messages */
    private val statements: List<Pair<Node, String>> = root.line().map { line ->
        val statement = line.statement() as KeyLangParser.ExpressionStatementContext
        compileRoot(statement.expressionRoot()) to statement.text
    }

    /** evaluates the expression, null when it has no statements */
    fun evaluate(): Double? {
        var result: Double? = null
        for ((node, text) in statements) {
            errors.clear()
            val value = node.eval()
            if (errors.isNotEmpty()) {
                // the message of the last error, the problems of all of them
                val problems = errors.flatMap { it.problems }.distinct().sortedBy { it.start }
                val last = errors.last()
                errors.clear()
                throw ExpressionException("error in evaluation of '$text': ${last.message ?: ""}", problems)
            }
            result = value
        }
        return result
    }

    /** an error about [node], the name of a value or a function, whose value is NaN */
    private fun error(message: String, node: TerminalNode, kind: ExpressionProblem.Kind): Double {
        val name = node.text.replace("`", "")
        val token = node.symbol
        val call = node.getParent() as? ParserRuleContext
        val problem = when {
            kind == ExpressionProblem.Kind.UNKNOWN_VALUE -> span(token).let { ExpressionProblem(kind, it.first, it.last + 1, "no value $name", name) }
            // a function that takes other numbers of arguments: the call
            arities(name, functions).isNotEmpty() && call != null -> span(call).let {
                val arguments = (0 until call.childCount).count { i -> call.getChild(i) is KeyLangParser.ExpressionContext }
                ExpressionProblem(ExpressionProblem.Kind.WRONG_ARGUMENTS, it.first, it.last + 1, argumentsMessage(name, arities(name, functions), arguments), name)
            }
            else -> span(token).let { ExpressionProblem(ExpressionProblem.Kind.UNKNOWN_FUNCTION, it.first, it.last + 1, "no function $name", name) }
        }
        errors.add(ExpressionException(message, listOf(problem)))
        return Double.NaN
    }

    /** an error about a part of the grammar that is not evaluated */
    private fun unsupported(ctx: ParserRuleContext): Node {
        val range = span(ctx)
        val problem = ExpressionProblem(ExpressionProblem.Kind.UNSUPPORTED, range.first, range.last + 1, "'${ctx.text}' is not supported")
        return Node {
            errors.add(ExpressionException("unsupported expression: '${ctx.text}'", listOf(problem)))
            Double.NaN
        }
    }

    private fun compileRoot(ctx: KeyLangParser.ExpressionRootContext): Node {
        ctx.rangeExpression()?.let { return unsupported(it) }
        return compile(ctx.expression()!!)
    }

    private fun compile(ctx: KeyLangParser.ExpressionContext): Node = when (ctx) {
        is KeyLangParser.IntLiteralContext -> constant(ctx.text.toDouble())
        is KeyLangParser.DecimalLiteralContext -> constant(ctx.text.toDouble())
        is KeyLangParser.ParenExpressionContext -> compileRoot(ctx.expressionRoot())
        is KeyLangParser.MinusExpressionContext -> compile(ctx.expression()).let { a -> Node { -a.eval() } }
        is KeyLangParser.BinaryOperation1Context -> {
            val a = compile(ctx.expression(0)!!)
            val b = compile(ctx.expression(1)!!)
            when (ctx.operator?.type) {
                KeyLangParser.Tokens.ASTERISK -> Node { a.eval() * b.eval() }
                KeyLangParser.Tokens.DIVISION -> Node { a.eval() / b.eval() }
                else -> Node { a.eval().mod(b.eval()) }
            }
        }
        is KeyLangParser.BinaryOperation2Context -> {
            val a = compile(ctx.expression(0)!!)
            val b = compile(ctx.expression(1)!!)
            if (ctx.operator?.type == KeyLangParser.Tokens.PLUS) Node { a.eval() + b.eval() } else Node { a.eval() - b.eval() }
        }
        is KeyLangParser.ValueReferenceContext -> value(ctx.ID()!!)
        is KeyLangParser.FunctionCall0ExpressionContext -> call(ctx.ID()!!, emptyList())
        is KeyLangParser.FunctionCall1ExpressionContext -> call(ctx.ID()!!, listOf(ctx.expression()))
        is KeyLangParser.FunctionCall2ExpressionContext -> call(ctx.ID()!!, ctx.expression())
        is KeyLangParser.FunctionCall3ExpressionContext -> call(ctx.ID()!!, ctx.expression())
        is KeyLangParser.FunctionCall4ExpressionContext -> call(ctx.ID()!!, ctx.expression())
        is KeyLangParser.FunctionCall5ExpressionContext -> call(ctx.ID()!!, ctx.expression())
        else -> unsupported(ctx)
    }

    private fun constant(v: Double) = Node { v }

    /** the value of [id], read at every evaluation */
    private fun value(id: TerminalNode): Node {
        val name = id.text.replace("`", "")
        if (name == "PI") return constant(PI)
        return Node {
            constants[name] ?: error("unresolved value: '${name}'. available values: $constants", id, ExpressionProblem.Kind.UNKNOWN_VALUE)
        }
    }

    /** the call of [id] with [arguments]: a built-in function, a function of [functions], or an error */
    private fun call(id: TerminalNode, arguments: List<KeyLangParser.ExpressionContext>): Node {
        val name = id.text.replace("`", "")
        val args = arguments.map { compile(it) }
        builtIn(name, args)?.let { return it }
        extension(name, args)?.let { return it }
        // a function that is not there yet, found at every evaluation
        val signature = if (args.isEmpty()) "()" else args.indices.joinToString(", ", "(", ")") { "x$it" }
        val message = "unresolved function: '$name$signature'"
        return Node { extension(name, args)?.eval() ?: error(message, id, ExpressionProblem.Kind.UNKNOWN_FUNCTION) }
    }

    /** the call of the function [name] of [functions] with [args], null when it has none that takes as many */
    private fun extension(name: String, args: List<Node>): Node? = when (args.size) {
        0 -> functions.functions0[name]?.let { f -> Node { f() } }
        1 -> functions.functions1[name]?.let { f -> val (x) = args; Node { f(x.eval()) } }
        2 -> functions.functions2[name]?.let { f -> val (x, y) = args; Node { f(x.eval(), y.eval()) } }
        3 -> functions.functions3[name]?.let { f -> val (x, y, z) = args; Node { f(x.eval(), y.eval(), z.eval()) } }
        4 -> functions.functions4[name]?.let { f -> val (x, y, z, w) = args; Node { f(x.eval(), y.eval(), z.eval(), w.eval()) } }
        5 -> functions.functions5[name]?.let { f -> val (x, y, z, w, v) = args; Node { f(x.eval(), y.eval(), z.eval(), w.eval(), v.eval()) } }
        else -> null
    }

    /** the call of the built-in function [name] with [args], null when there is none that takes as many */
    private fun builtIn(name: String, args: List<Node>): Node? = when (args.size) {
        0 -> when (name) {
            "random" -> Node { Double.uniform(0.0, 1.0) }
            else -> null
        }
        1 -> {
            val x = args[0]
            when (name) {
                "sqrt" -> Node { sqrt(x.eval()) }
                "radians" -> Node { x.eval().asRadians }
                "degrees" -> Node { x.eval().asDegrees }
                "cos" -> Node { cos(x.eval()) }
                "sin" -> Node { sin(x.eval()) }
                "tan" -> Node { tan(x.eval()) }
                "atan" -> Node { atan(x.eval()) }
                "acos" -> Node { acos(x.eval()) }
                "asin" -> Node { asin(x.eval()) }
                "exp" -> Node { exp(x.eval()) }
                "abs" -> Node { abs(x.eval()) }
                "floor" -> Node { floor(x.eval()) }
                "round" -> Node { roundHalfAway(x.eval()) }
                "ceil" -> Node { ceil(x.eval()) }
                "saturate" -> Node { x.eval().coerceIn(0.0, 1.0) }
                else -> null
            }
        }
        2 -> {
            val (x, y) = args
            when (name) {
                "max" -> Node { max(x.eval(), y.eval()) }
                "min" -> Node { min(x.eval(), y.eval()) }
                "pow" -> Node { x.eval().pow(y.eval()) }
                "mod" -> Node { x.eval().mod(y.eval()) }
                "atan2" -> Node { atan2(x.eval(), y.eval()) }
                "random" -> Node { Double.uniform(x.eval(), y.eval()) }
                "length" -> Node { Vector2(x.eval(), y.eval()).length }
                else -> null
            }
        }
        else -> {
            val n = args.size
            when {
                name == "min" -> Node { var m = args[0].eval(); for (i in 1 until n) m = minOf(m, args[i].eval()); m }
                name == "max" -> Node { var m = args[0].eval(); for (i in 1 until n) m = maxOf(m, args[i].eval()); m }
                name == "sum" -> Node { var s = 0.0; for (a in args) s += a.eval(); s }
                n == 3 && name == "mix" -> { val (x, y, z) = args; Node { mix(x.eval(), y.eval(), z.eval()) } }
                n == 3 && name == "smoothstep" -> { val (x, y, z) = args; Node { smoothstep(x.eval(), y.eval(), z.eval()) } }
                n == 3 && name == "length" -> { val (x, y, z) = args; Node { Vector3(x.eval(), y.eval(), z.eval()).length } }
                n == 4 && name == "length" -> { val (x, y, z, w) = args; Node { Vector4(x.eval(), y.eval(), z.eval(), w.eval()).length } }
                n == 5 && name == "map" -> { val (a, b, c, d, e) = args; Node { map(a.eval(), b.eval(), c.eval(), d.eval(), e.eval()) } }
                else -> null
            }
        }
    }
}

/** [x] rounded to the nearest whole number, halves away from zero, like Math.round of Javascript for positive numbers */
internal fun roundHalfAway(x: Double): Double {
    val whole = truncate(x)
    return if (abs(x - whole) == 0.5) whole + sign(x) else round(x)
}
