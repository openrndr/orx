package org.openrndr.extra.expressions.typed

import org.antlr.v4.kotlinruntime.ParserRuleContext
import org.antlr.v4.kotlinruntime.tree.TerminalNode
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.expressions.ExpressionProblem
import org.openrndr.extra.expressions.parser.KeyLangLexer
import org.openrndr.extra.expressions.parser.KeyLangParser
import org.openrndr.extra.expressions.span
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import org.openrndr.math.mod
import kotlin.math.PI
import kotlin.math.roundToInt

/** the value of a name that could not be found: what is computed from it is not computed, and the name is reported */
private object Missing

/** a failure while evaluating a part of an expression, at [problem] */
private class Failure(message: String, val problem: ExpressionProblem) : RuntimeException(message)

/**
 * the values of the parameters of an expression or of a lambda in it, in the frame of the expression or lambda around
 * it, and the errors of the evaluation
 */
private class Frame(val parent: Frame?, val values: Array<Any?>, val errors: MutableList<ExpressionException>)

/** a part of an expression, evaluated in a [Frame] */
private fun interface TypedNode {
    fun eval(frame: Frame): Any
}

/**
 * A typed expression compiled from its parse tree to closures, once: evaluating it calls them, without walking the
 * tree. Values are asked of [constants] at every evaluation; [parameters] are given to [evaluate]. Lambdas in the
 * expression are compiled with it, and see the parameters of the lambdas around them. Evaluations of the same compiled
 * expression on different threads do not share state.
 *
 * A name that can not be found is reported once the whole expression is evaluated, with all the other names that
 * could not be found; what goes wrong otherwise, like operands of types that do not go together, is reported where it
 * happens. Both as an [ExpressionException] with the problems and where they are.
 */
internal class TypedCompiledExpression(
    root: KeyLangParser.KeyLangFileContext,
    private val constants: (String) -> Any?,
    private val functions: TypedFunctionExtensions,
    parameters: List<String> = emptyList()
) {
    /** the names of the parameters of the expression and of the lambdas around what is being compiled, innermost last */
    private val scopes = ArrayList<List<String>>().apply { add(parameters) }

    private val statements: List<Pair<TypedNode, ParserRuleContext>> = root.line().map { line ->
        val statement = line.statement() as KeyLangParser.ExpressionStatementContext
        compileRoot(statement.expressionRoot()) to statement
    }

    /** evaluates the expression with the values of its parameters, null when it has no statements */
    fun evaluate(vararg arguments: Any?): Any? {
        var result: Any? = null
        for ((node, statement) in statements) {
            val errors = ArrayList<ExpressionException>()
            val frame = Frame(null, arrayOf(*arguments), errors)
            val value = try {
                node.eval(frame)
            } catch (f: Failure) {
                val problems = (errors.flatMap { it.problems } + f.problem).distinct().sortedBy { it.start }
                throw ExpressionException("error in evaluation of '${statement.text}': ${f.message ?: ""}", problems)
            } catch (e: ExpressionException) {
                throw e
            } catch (e: Exception) {
                // what a function or a value threw: at the whole statement
                val range = span(statement)
                val problem = ExpressionProblem(ExpressionProblem.Kind.EVALUATION, range.first, range.last + 1, e.message ?: e::class.simpleName ?: "error")
                throw ExpressionException("error in evaluation of '${statement.text}': ${e.message ?: ""}", errors.flatMap { it.problems } + problem)
            }
            if (errors.isNotEmpty()) {
                val problems = errors.flatMap { it.problems }.distinct().sortedBy { it.start }
                throw ExpressionException("error in evaluation of '${statement.text}': ${errors.last().message ?: ""}", problems)
            }
            result = value
        }
        return result
    }

    //<editor-fold desc="errors">
    private fun failure(ctx: ParserRuleContext, message: String): Nothing {
        val range = span(ctx)
        throw Failure(message, ExpressionProblem(ExpressionProblem.Kind.EVALUATION, range.first, range.last + 1, message))
    }

    private fun typeName(x: Any) = x::class.simpleName ?: "value"

    private fun operands(ctx: ParserRuleContext, operator: String, left: Any, right: Any): Nothing =
        failure(ctx, "unsupported operands for $operator: ${typeName(left)} and ${typeName(right)}")

    /** the value of a name that could not be found */
    private fun missing(frame: Frame, message: String, node: TerminalNode, kind: ExpressionProblem.Kind): Any {
        val name = node.text.replace("`", "")
        val range = if (kind == ExpressionProblem.Kind.UNKNOWN_VALUE) span(node.symbol) else span(node.getParent() as ParserRuleContext)
        val short = if (kind == ExpressionProblem.Kind.UNKNOWN_VALUE) "no value $name" else "no function $name"
        frame.errors.add(ExpressionException(message, listOf(ExpressionProblem(kind, range.first, range.last + 1, short, name))))
        return Missing
    }

    private fun Double.truth() = roundToInt() != 0
    private fun Boolean.number() = if (this) 1.0 else 0.0
    //</editor-fold>

    private fun compileRoot(ctx: KeyLangParser.ExpressionRootContext): TypedNode =
        ctx.rangeExpression()?.let(::range) ?: compile(ctx.expression()!!)

    private fun range(ctx: KeyLangParser.RangeExpressionContext): TypedNode {
        val lower = compile(ctx.expression(0)!!)
        val upper = compile(ctx.expression(1)!!)
        val step = ctx.step?.let { compile(ctx.expression(2)!!) }
        val operator = ctx.operator?.type
        return TypedNode { f ->
            val l = lower.eval(f)
            val u = upper.eval(f)
            val s = step?.eval(f)
            if (l === Missing || u === Missing || s === Missing) return@TypedNode Missing
            val a = (l as? Double ?: failure(ctx, "a range starts at a number, not at ${typeName(l)}")).toInt()
            val b = (u as? Double ?: failure(ctx, "a range ends at a number, not at ${typeName(u)}")).toInt()
            val progression = when (operator) {
                KeyLangLexer.Tokens.RANGE_INCLUSIVE -> a..b
                KeyLangLexer.Tokens.RANGE_EXCLUSIVE, KeyLangLexer.Tokens.RANGE_EXCLUSIVE_UNTIL -> a until b
                else -> a downTo b
            }
            val stepped = if (s == null) progression else progression step (s as? Double ?: failure(ctx, "a step is a number")).toInt()
            stepped.map { it.toDouble() }
        }
    }

    private fun compile(ctx: KeyLangParser.ExpressionContext): TypedNode = when (ctx) {
        is KeyLangParser.IntLiteralContext, is KeyLangParser.DecimalLiteralContext -> ctx.text.toDouble().let { v -> TypedNode { v } }
        is KeyLangParser.StringLiteralContext -> ctx.parts.joinToString("") { it.text }.let { v -> TypedNode { v } }
        is KeyLangParser.ListLiteralContext -> ctx.expressionRoot().map(::compileRoot).let { items ->
            TypedNode { f -> items.map { it.eval(f) }.let { list -> if (list.any { it === Missing }) Missing else list } }
        }
        is KeyLangParser.ParenExpressionContext -> compileRoot(ctx.expressionRoot())
        is KeyLangParser.LambdaExpressionContext -> lambda(ctx.lambda() as KeyLangParser.FunctionLiteralContext)
        is KeyLangParser.ValueReferenceContext -> value(ctx.ID()!!)
        is KeyLangParser.MinusExpressionContext -> minus(ctx)
        is KeyLangParser.NegateExpressionContext -> compile(ctx.expression()).let { a ->
            TypedNode { f ->
                when (val v = a.eval(f)) {
                    Missing -> Missing
                    is Double -> (!v.truth()).number()
                    else -> failure(ctx, "'!' takes a number, not ${typeName(v)}")
                }
            }
        }
        is KeyLangParser.BinaryOperation1Context -> binary1(ctx)
        is KeyLangParser.BinaryOperation2Context -> binary2(ctx)
        is KeyLangParser.ComparisonOperationContext -> comparison(ctx)
        is KeyLangParser.JoinOperationContext -> join(ctx)
        is KeyLangParser.TernaryExpressionContext -> {
            val condition = compile(ctx.expression(0)!!)
            val yes = compile(ctx.expression(1)!!)
            val no = compile(ctx.expression(2)!!)
            TypedNode { f ->
                when (val c = condition.eval(f)) {
                    Missing -> Missing
                    is Double -> if (c.truth()) yes.eval(f) else no.eval(f)
                    else -> failure(ctx, "a condition is a number, not ${typeName(c)}")
                }
            }
        }
        is KeyLangParser.PropReferenceContext -> property(ctx)
        is KeyLangParser.IndexExpressionContext -> index(ctx)
        is KeyLangParser.FunctionCall0ExpressionContext -> call(ctx, ctx.ID()!!, emptyList())
        is KeyLangParser.FunctionCall1ExpressionContext -> call(ctx, ctx.ID()!!, listOf(ctx.expression()))
        is KeyLangParser.FunctionCall2ExpressionContext -> call(ctx, ctx.ID()!!, ctx.expression())
        is KeyLangParser.FunctionCall3ExpressionContext -> call(ctx, ctx.ID()!!, ctx.expression())
        is KeyLangParser.FunctionCall4ExpressionContext -> call(ctx, ctx.ID()!!, ctx.expression())
        is KeyLangParser.FunctionCall5ExpressionContext -> call(ctx, ctx.ID()!!, ctx.expression())
        is KeyLangParser.MemberFunctionCall0ExpressionContext -> member(ctx, ctx.expression(), ctx.ID()!!, emptyList())
        is KeyLangParser.MemberFunctionCall1ExpressionContext -> member(ctx, ctx.expression(0)!!, ctx.ID()!!, ctx.expression().drop(1).map(::compile))
        is KeyLangParser.MemberFunctionCall2ExpressionContext -> member(ctx, ctx.expression(0)!!, ctx.ID()!!, ctx.expression().drop(1).map(::compile))
        is KeyLangParser.MemberFunctionCall3ExpressionContext -> member(ctx, ctx.expression(0)!!, ctx.ID()!!, ctx.expression().drop(1).map(::compile))
        is KeyLangParser.MemberFunctionCall4ExpressionContext -> member(ctx, ctx.expression(0)!!, ctx.ID()!!, ctx.expression().drop(1).map(::compile))
        is KeyLangParser.MemberFunctionCall0LambdaExpressionContext ->
            member(ctx, ctx.expression(), ctx.ID()!!, listOf(lambda(ctx.lambda() as KeyLangParser.FunctionLiteralContext)))
        else -> TypedNode { failure(ctx, "'${ctx.text}' is not supported") }
    }

    /** a lambda, a function of one or two parameters, `it` when it names none */
    private fun lambda(ctx: KeyLangParser.FunctionLiteralContext): TypedNode {
        val names = ctx.ID().map { it.text.replace("`", "") }
        if (names.size > 2) return TypedNode { failure(ctx, "lambdas with ${names.size} parameters are not supported") }
        scopes.add(names.ifEmpty { listOf("it") })
        val body = compile(ctx.expression())
        scopes.removeAt(scopes.lastIndex)
        return if (names.size == 2) {
            TypedNode { f -> { a: Any, b: Any -> body.eval(Frame(f, arrayOf(a, b), f.errors)) } }
        } else {
            TypedNode { f -> { a: Any -> body.eval(Frame(f, arrayOf(a), f.errors)) } }
        }
    }

    /** the value of [id]: a parameter of a lambda around it or of the expression, PI, or a constant at every evaluation */
    private fun value(id: TerminalNode): TypedNode {
        val name = id.text.replace("`", "")
        for (depth in 0 until scopes.size) {
            val index = scopes[scopes.lastIndex - depth].indexOf(name)
            if (index >= 0) return TypedNode { f ->
                var frame = f
                repeat(depth) { frame = frame.parent!! }
                frame.values[index] ?: missing(f, "unresolved value: '$name'", id, ExpressionProblem.Kind.UNKNOWN_VALUE)
            }
        }
        if (name == "PI") return TypedNode { PI }
        return TypedNode { f ->
            constants(name) ?: missing(f, "unresolved value: '$name'. Available constant: $constants", id, ExpressionProblem.Kind.UNKNOWN_VALUE)
        }
    }

    private fun minus(ctx: KeyLangParser.MinusExpressionContext): TypedNode {
        val a = compile(ctx.expression())
        return TypedNode { f ->
            when (val v = a.eval(f)) {
                Missing -> Missing
                is Double -> -v
                is Vector2 -> -v
                is Vector3 -> -v
                is Vector4 -> -v
                is Matrix44 -> v * -1.0
                else -> failure(ctx, "unsupported operand for -: ${typeName(v)}")
            }
        }
    }

    private fun binary1(ctx: KeyLangParser.BinaryOperation1Context): TypedNode {
        val a = compile(ctx.expression(0)!!)
        val b = compile(ctx.expression(1)!!)
        return when (ctx.operator?.type) {
            KeyLangLexer.Tokens.ASTERISK -> TypedNode { f ->
                val left = a.eval(f)
                val right = b.eval(f)
                when {
                    left === Missing || right === Missing -> Missing
                    left is Double && right is Double -> left * right
                    left is Vector2 && right is Vector2 -> left * right
                    left is Vector2 && right is Double -> left * right
                    left is Vector3 && right is Vector3 -> left * right
                    left is Vector3 && right is Double -> left * right
                    left is Vector4 && right is Vector4 -> left * right
                    left is Vector4 && right is Double -> left * right
                    left is Matrix44 && right is Matrix44 -> left * right
                    left is Matrix44 && right is Vector4 -> left * right
                    left is Matrix44 && right is Double -> left * right
                    left is ColorRGBa && right is Double -> left * right
                    left is String && right is Double -> left.repeat(right.roundToInt())
                    left is List<*> && right is Double -> (0 until right.roundToInt()).flatMap { left }
                    else -> operands(ctx, "*", left, right)
                }
            }
            KeyLangLexer.Tokens.DIVISION -> TypedNode { f ->
                val left = a.eval(f)
                val right = b.eval(f)
                when {
                    left === Missing || right === Missing -> Missing
                    left is Double && right is Double -> left / right
                    left is Vector2 && right is Vector2 -> left / right
                    left is Vector2 && right is Double -> left / right
                    left is Vector3 && right is Vector3 -> left / right
                    left is Vector3 && right is Double -> left / right
                    left is Vector4 && right is Vector4 -> left / right
                    left is Vector4 && right is Double -> left / right
                    left is ColorRGBa && right is Double -> left / right
                    else -> operands(ctx, "/", left, right)
                }
            }
            else -> TypedNode { f ->
                val left = a.eval(f)
                val right = b.eval(f)
                when {
                    left === Missing || right === Missing -> Missing
                    left is Double && right is Double -> left.mod(right)
                    left is Vector2 && right is Vector2 -> left.mod(right)
                    left is Vector3 && right is Vector3 -> left.mod(right)
                    left is Vector4 && right is Vector4 -> left.mod(right)
                    else -> operands(ctx, "%", left, right)
                }
            }
        }
    }

    private fun binary2(ctx: KeyLangParser.BinaryOperation2Context): TypedNode {
        val a = compile(ctx.expression(0)!!)
        val b = compile(ctx.expression(1)!!)
        return if (ctx.operator?.type == KeyLangLexer.Tokens.PLUS) TypedNode { f ->
            val left = a.eval(f)
            val right = b.eval(f)
            when {
                left === Missing || right === Missing -> Missing
                left is Double && right is Double -> left + right
                left is Vector2 && right is Vector2 -> left + right
                left is Vector3 && right is Vector3 -> left + right
                left is Vector4 && right is Vector4 -> left + right
                left is Matrix44 && right is Matrix44 -> left + right
                left is ColorRGBa && right is ColorRGBa -> left + right
                left is String && right is String -> left + right
                left is List<*> && right is List<*> -> left + right
                else -> operands(ctx, "+", left, right)
            }
        } else TypedNode { f ->
            val left = a.eval(f)
            val right = b.eval(f)
            when {
                left === Missing || right === Missing -> Missing
                left is Double && right is Double -> left - right
                left is Vector2 && right is Vector2 -> left - right
                left is Vector3 && right is Vector3 -> left - right
                left is Vector4 && right is Vector4 -> left - right
                left is Matrix44 && right is Matrix44 -> left - right
                left is ColorRGBa && right is ColorRGBa -> left - right
                else -> operands(ctx, "-", left, right)
            }
        }
    }

    private fun comparison(ctx: KeyLangParser.ComparisonOperationContext): TypedNode {
        val a = compile(ctx.expression(0)!!)
        val b = compile(ctx.expression(1)!!)
        val operator = ctx.operator?.type
        val symbol = ctx.operator?.text ?: "?"
        return TypedNode { f ->
            val left = a.eval(f)
            val right = b.eval(f)
            if (left === Missing || right === Missing) return@TypedNode Missing
            if (operator == KeyLangLexer.Tokens.EQ) {
                val comparable = (left is Double && right is Double) || (left is Vector2 && right is Vector2) ||
                        (left is Vector3 && right is Vector3) || (left is Vector4 && right is Vector4) ||
                        (left is ColorRGBa && right is ColorRGBa) || (left is String && right is String)
                if (!comparable) operands(ctx, symbol, left, right)
                // numbers, also those of vectors and colors, compare as IEEE numbers: -0.0 equals 0.0
                when (left) {
                    is Double -> left == right as Double
                    is Vector2 -> (right as Vector2).let { left.x == it.x && left.y == it.y }
                    is Vector3 -> (right as Vector3).let { left.x == it.x && left.y == it.y && left.z == it.z }
                    is Vector4 -> (right as Vector4).let { left.x == it.x && left.y == it.y && left.z == it.z && left.w == it.w }
                    is ColorRGBa -> (right as ColorRGBa).let {
                        left.r == it.r && left.g == it.g && left.b == it.b && left.alpha == it.alpha && left.linearity == it.linearity
                    }
                    else -> left == right
                }.number()
            } else {
                if (left !is Double || right !is Double) operands(ctx, symbol, left, right)
                when (operator) {
                    KeyLangLexer.Tokens.LT -> left < right
                    KeyLangLexer.Tokens.LTEQ -> left <= right
                    KeyLangLexer.Tokens.GT -> left > right
                    else -> left >= right
                }.number()
            }
        }
    }

    /** && and ||, of which the right side is evaluated when it decides */
    private fun join(ctx: KeyLangParser.JoinOperationContext): TypedNode {
        val a = compile(ctx.expression(0)!!)
        val b = compile(ctx.expression(1)!!)
        val and = ctx.operator?.type == KeyLangLexer.Tokens.AND
        fun truth(v: Any): Boolean? = when (v) {
            Missing -> null
            is Double -> v.truth()
            else -> failure(ctx, "'${ctx.operator?.text}' takes numbers, not ${typeName(v)}")
        }
        return TypedNode { f ->
            val left = truth(a.eval(f)) ?: return@TypedNode Missing
            if (and && !left) 0.0 else if (!and && left) 1.0 else (truth(b.eval(f)) ?: return@TypedNode Missing).number()
        }
    }

    private fun property(ctx: KeyLangParser.PropReferenceContext): TypedNode {
        val receiver = compile(ctx.expression())
        val name = ctx.ID()!!.text.replace("`", "")
        return TypedNode { f ->
            @Suppress("UNCHECKED_CAST")
            when (val r = receiver.eval(f)) {
                Missing -> Missing
                is Map<*, *> -> r[name] ?: failure(ctx, "property '$name' not found")
                is Function<*> -> (r as (String) -> Any?).invoke(name) ?: failure(ctx, "property '$name' not found")
                else -> builtInProperty(r, name) ?: functions.properties(r, name)
                    ?: failure(ctx, "${typeName(r)} has no property '$name'")
            }
        }
    }

    private fun index(ctx: KeyLangParser.IndexExpressionContext): TypedNode {
        val list = compile(ctx.expression(0)!!)
        val index = compile(ctx.expression(1)!!)
        return TypedNode { f ->
            val l = list.eval(f)
            val i = index.eval(f)
            if (l === Missing || i === Missing) return@TypedNode Missing
            val n = (i as? Double ?: failure(ctx, "an index is a number, not ${typeName(i)}")).roundToInt()
            @Suppress("UNCHECKED_CAST")
            when (l) {
                is List<*> -> l.getOrNull(n) ?: failure(ctx, "index $n is outside the ${l.size} items")
                is Function<*> -> (l as (Int) -> Any)(n)
                else -> failure(ctx, "can't index ${typeName(l)}")
            }
        }
    }

    /** a built-in function or one of [functions] of as many arguments, found when compiling */
    private fun dispatch(name: String, n: Int): ((Array<Any>) -> Any)? = when (n) {
        0 -> dispatchFunction0(name, functions.functions0)
        1 -> dispatchFunction1(name, functions.functions1)
        2 -> dispatchFunction2(name, functions.functions2)
        3 -> dispatchFunction3(name, functions.functions3)
        4 -> dispatchFunction4(name, functions.functions4)
        else -> functions.functions5[name]?.let { fn -> { x: Array<Any> -> fn(x[0], x[1], x[2], x[3], x[4]) } }
    }

    /** a function that a constant is, called with [x] */
    @Suppress("UNCHECKED_CAST")
    private fun callConstant(function: Any?, x: Array<Any>): Any? = when (x.size) {
        0 -> (function as? () -> Any)?.invoke()
        1 -> (function as? (Any) -> Any)?.invoke(x[0])
        2 -> (function as? (Any, Any) -> Any)?.invoke(x[0], x[1])
        3 -> (function as? (Any, Any, Any) -> Any)?.invoke(x[0], x[1], x[2])
        4 -> (function as? (Any, Any, Any, Any) -> Any)?.invoke(x[0], x[1], x[2], x[3])
        else -> (function as? (Any, Any, Any, Any, Any) -> Any)?.invoke(x[0], x[1], x[2], x[3], x[4])
    }

    private fun call(ctx: ParserRuleContext, id: TerminalNode, arguments: List<KeyLangParser.ExpressionContext>): TypedNode {
        val name = id.text.replace("`", "")
        val args = arguments.map(::compile)
        val n = args.size
        val signature = if (n == 0) "()" else (0 until n).joinToString(", ", "(", ")") { "x$it" }
        val known = dispatch(name, n)
        return TypedNode { f ->
            val x = Array(n) { args[it].eval(f) }
            if (x.any { it === Missing }) return@TypedNode Missing
            val function = known ?: dispatch(name, n)
            if (function != null) {
                function(x)
            } else {
                // a function that a constant is
                callConstant(constants(name), x)
                    ?: missing(f, "unresolved function: '$name$signature'", id, ExpressionProblem.Kind.UNKNOWN_FUNCTION)
            }
        }
    }

    /** the member function [id] of what [receiver] gives, with [args] */
    private fun member(ctx: ParserRuleContext, receiver: KeyLangParser.ExpressionContext, id: TerminalNode, args: List<TypedNode>): TypedNode {
        val r = compile(receiver)
        val name = id.text.replace("`", "")
        return TypedNode { f ->
            val target = r.eval(f)
            val x = Array(args.size) { args[it].eval(f) }
            if (target === Missing || x.any { it === Missing }) return@TypedNode Missing
            @Suppress("UNCHECKED_CAST")
            when (target) {
                is String -> (target.memberFunctions(name) ?: failure(ctx, "String has no function '$name'"))(x)
                is List<*> -> (target.memberFunctions(name) ?: failure(ctx, "List has no function '$name'"))(x)
                is ColorRGBa -> when {
                    name == "shade" && x.size == 1 -> target.shade(x[0] as? Double ?: failure(ctx, "shade takes a number"))
                    name == "opacify" && x.size == 1 -> target.opacify(x[0] as? Double ?: failure(ctx, "opacify takes a number"))
                    else -> failure(ctx, "ColorRGBa has no function '$name' of ${x.size} arguments")
                }
                is Function<*> -> {
                    val function = (target as (String) -> Any?).invoke(name) ?: failure(ctx, "no function '$name'")
                    callConstant(function, x) ?: failure(ctx, "'$name' is no function of ${x.size} arguments")
                }
                else -> failure(ctx, "${typeName(target)} has no function '$name'")
            }
        }
    }
}
