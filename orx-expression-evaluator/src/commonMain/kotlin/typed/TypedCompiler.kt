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
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.math.PI
import kotlin.math.roundToInt

/** the value of a name that could not be found: what is computed from it is not computed, and the name is reported */
private object Missing

/** a failure while evaluating a part of an expression, at [problem] */
private class Failure(message: String, val problem: ExpressionProblem) : RuntimeException(message)

/** the value of a slot of which the number is in the numbers of its frame, see [Frame.store] */
private object Unboxed

/**
 * the values of the parameters of an expression or of a lambda in it, in the frame of the expression or lambda around
 * it, and the errors of the evaluation; the numbers of the slots are in [numbers], unboxed, when it has them
 */
private class Frame(
    var parent: Frame?,
    val values: Array<Any?>,
    var errors: MutableList<ExpressionException>,
    val context: Context,
    val numbers: DoubleArray? = null
) {
    /** stores the number [d] in slot [i], or [o] when it is not null, the value that was no number */
    fun store(i: Int, d: Double, o: Any?) {
        val n = numbers
        when {
            o != null -> if (values[i] !== o) values[i] = o
            n == null -> values[i] = d
            else -> {
                n[i] = d
                // a frame of the call before keeps its marks, nothing is written but the number
                if (values[i] !== Unboxed) values[i] = Unboxed
            }
        }
    }

    /** the value of slot [i], boxed */
    fun value(i: Int): Any? = values[i].let { if (it === Unboxed) numbers!![i] else it }

    /** whether the frame is that of a call of a function that has not returned, see [Context] */
    var busy = false

    /** the frame for a call of the same function while this one is busy, see [Context] */
    var next: Frame? = null

    /** the value of a node that was evaluated as a number but gave something else, until it is taken */
    private var spilled: Any? = null

    /** the value that was spilled since, or null when the node gave a number */
    fun take(): Any? {
        val s = spilled ?: return null
        spilled = null
        return s
    }

    /** [v] as a number, or NaN with [v] spilled when it is no number */
    fun spill(v: Any): Double {
        if (v is Double) return v
        spilled = v
        return Double.NaN
    }
}

/**
 * a part of an expression, evaluated in a [Frame]; [evalDouble] gives a number without boxing it, so that what is
 * computed from numbers only is computed without boxing
 */
private fun interface TypedNode {
    fun eval(frame: Frame): Any

    /** the value as a number, or NaN with the value spilled in [frame], see [Frame.take], when it is no number */
    fun evalDouble(frame: Frame): Double = frame.spill(eval(frame))
}

/** a node of which the value is mostly a number, that is evaluated as a number first */
private abstract class DoubleNode : TypedNode {
    abstract override fun evalDouble(frame: Frame): Double

    override fun eval(frame: Frame): Any {
        val d = evalDouble(frame)
        return frame.take() ?: d
    }
}

/** a number that is known when compiling */
private class NumberNode(val value: Double) : TypedNode {
    private val boxed: Any = value
    override fun eval(frame: Frame) = boxed
    override fun evalDouble(frame: Frame) = value
}

/**
 * [a] and [b] with [double] when both are numbers, with [other] otherwise, of which the values that are numbers are
 * boxed; inlined, so that every operation is a class of its own, of which [double] is inlined too
 */
private inline fun binary(
    a: TypedNode,
    b: TypedNode,
    crossinline double: (Double, Double) -> Double,
    noinline other: (Any, Any) -> Any
): TypedNode = object : DoubleNode() {
    override fun evalDouble(frame: Frame): Double {
        val l = a.evalDouble(frame)
        val lo = frame.take()
        val r = b.evalDouble(frame)
        val ro = frame.take()
        if (lo == null && ro == null) return double(l, r)
        return frame.spill(other(lo ?: l, ro ?: r))
    }
}

/** [a] with [double] when it is a number, with [other] otherwise, inlined like [binary] */
private inline fun unary(a: TypedNode, crossinline double: (Double) -> Double, noinline other: (Any) -> Any): TypedNode = object : DoubleNode() {
    override fun evalDouble(frame: Frame): Double {
        val v = a.evalDouble(frame)
        val o = frame.take() ?: return double(v)
        return frame.spill(other(o))
    }
}

/**
 * the frames of the calls of the declared functions in an evaluation, or in a call of a lambda, which can be called
 * on any thread: a function can not call itself, so a call can have the frame of the call before, when that has
 * returned and no lambda in the function can have kept it, see [DeclaredFunction.id]
 */
@OptIn(ExperimentalAtomicApi::class)
private class Context(val functions: Int) {
    private var frames: Array<Frame?>? = null

    /** the frame of the evaluation before, when no lambda can have kept it */
    var top: Frame? = null

    companion object {
        /** the context of calls without functions of which the calls can have the frame of the call before */
        val NONE = Context(0)

        fun of(functions: Int) = if (functions == 0) NONE else Context(functions)
    }

    /** the frame for a call of [function], with the slots it had, that is busy until the call returns */
    fun frame(function: DeclaredFunction, errors: MutableList<ExpressionException>): Frame {
        val id = function.id
        if (id < 0) return Frame(function.frame, arrayOfNulls(function.slots), errors, this, DoubleArray(function.slots)).also { it.busy = true }
        val frames = frames ?: arrayOfNulls<Frame>(functions).also { frames = it }
        // a call in the arguments of a call of the same function has the next frame
        var cached = frames[id]
        var last: Frame? = null
        while (cached != null && cached.busy) {
            last = cached
            cached = cached.next
        }
        if (cached == null) {
            val frame = Frame(function.frame, arrayOfNulls(function.slots), errors, this, DoubleArray(function.slots))
            frame.busy = true
            if (last == null) frames[id] = frame else last.next = frame
            return frame
        }
        // references are written when they change, which they do not when the frames around are those of before
        if (cached.parent !== function.frame) cached.parent = function.frame
        if (cached.errors !== errors) cached.errors = errors
        cached.busy = true
        return cached
    }
}

/**
 * a function declared with `fun` in [frame], of which the parameters are the first of the [slots] of its frames; [id]
 * is the number of the frames of its calls in a [Context], or -1 when the function has lambdas, which can keep them
 */
private class DeclaredFunction(val frame: Frame, val body: TypedNode, val slots: Int, val id: Int)

/**
 * A typed expression compiled from its parse tree to closures, once: evaluating it calls them, without walking the
 * tree. Values are asked of [constants] at every evaluation; [parameters] are given to [evaluate]. Lambdas in the
 * expression are compiled with it, and see the parameters of the lambdas around them. Evaluations of the same compiled
 * expression on different threads do not share state.
 *
 * A name that can not be found is reported once the whole expression is evaluated, with all the other names that
 * could not be found; what goes wrong otherwise, like operands of types that do not go together, is reported where it
 * happens. Both as an [ExpressionException] with the problems and where they are.
 *
 * An expression can be a small program, of statements separated by semicolons or new lines, that declare values with
 * `val` and functions with `fun` for the statements after them:
 * `fun f(x) { x * 5.0 }; fun g(x) = x * 6.0; val a = f(5); val b = g(a); a + b`. The body of a function is an
 * expression or a block of statements that ends in one; it sees its parameters and the values and functions declared
 * before it, not itself. A value is declared once in its program or body, and not with the name of a parameter of the
 * expression, a function once with as many parameters, at most 5; they come before the constants and the functions of
 * the same name.
 */
@OptIn(ExperimentalAtomicApi::class)
internal class TypedCompiledExpression(
    root: KeyLangParser.KeyLangFileContext,
    private val constants: (String) -> Any?,
    private val functions: TypedFunctionExtensions,
    parameters: List<String> = emptyList()
) {
    /**
     * the names of the slots of the frames of what is being compiled, innermost last: of the expression, its
     * parameters and then the values and functions declared by the statements compiled so far, of a function, its
     * parameters and then the values and functions declared in its body, and of a lambda, its parameters
     */
    private val scopes = ArrayList<MutableList<String>>()

    /** the parameters of the expression, then the values and functions declared by the statements compiled so far */
    private val top = parameters.toMutableList().also { scopes.add(it) }

    /**
     * a statement: its node, the statement for the messages, the slot of the value or the function it declares or -1,
     * and whether it declares
     */
    private class Statement(val node: TypedNode, val context: ParserRuleContext, val slot: Int, val declares: Boolean)

    /** the number of the declared functions of which the calls can have the frame of the call before */
    private var reusable = 0

    /** the statements, of which the last gives the result */
    private val statements: List<Statement> = root.line().map { compileStatement(it.statement()!!) }

    /** the number of the parameters and the values declared by the statements */
    private val slots = top.size

    /** whether the frames of evaluations can be those of the evaluation before: no lambda can keep them */
    private val pooled = !hasLambda(root)

    /** the context of the evaluations, with the frames of their calls, when no evaluation has it */
    private val spare = AtomicReference<Context?>(null)

    /** evaluates the expression with the values of its parameters, null when it has no statements or the last declares a value */
    fun evaluate(vararg arguments: Any?): Any? {
        // the context of the evaluation before, unless another evaluation has it, on another thread or around this one
        if (!pooled && reusable == 0) return evaluate(topFrame(Context.NONE, arguments.size), arguments)
        val context = spare.exchange(null) ?: Context(reusable)
        try {
            return evaluate(if (pooled) context.top?.takeIf { it.values.size >= arguments.size } ?: topFrame(context, arguments.size).also { context.top = it }
                else topFrame(context, arguments.size), arguments)
        } finally {
            spare.store(context)
        }
    }

    /** a frame for the parameters, then the values declared by the statements */
    private fun topFrame(context: Context, arguments: Int): Frame {
        val size = maxOf(slots, arguments)
        return Frame(null, arrayOfNulls(size), ArrayList(), context, DoubleArray(size))
    }

    /** the statements evaluated in [frame] with [arguments], the values of the parameters */
    private fun evaluate(frame: Frame, arguments: Array<out Any?>): Any? {
        for (i in arguments.indices) {
            val a = arguments[i]
            if (a is Double) frame.store(i, a, null) else frame.values[i] = a
        }
        val errors = frame.errors
        var result: Any? = null
        for (s in statements) {
            val node = s.node
            val statement = s.context
            errors.clear()
            val d = try {
                node.evalDouble(frame)
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
            val o = frame.take()
            if (s.slot >= 0) frame.store(s.slot, d, o)
            result = if (s.declares) null else o ?: d
        }
        return result
    }

    /** the name of the slot of a function, which no value can have */
    private fun slotName(name: String, parameters: Int) = "$name/$parameters"

    /** [statement], of which a declaration is declared in the innermost scope */
    private fun compileStatement(statement: KeyLangParser.StatementContext): Statement {
        val scope = scopes.last()
        return when (statement) {
            is KeyLangParser.ValDeclarationContext -> {
                // the value is declared after its expression, which can read a constant of the same name
                val node = compileRoot(statement.expressionRoot())
                val id = statement.ID()!!
                val name = id.text.replace("`", "")
                if (name in scope) Statement(declaredTwice(id, ExpressionProblem.Kind.DUPLICATE_VALUE), statement, -1, true)
                else Statement(node, statement, scope.size, true).also { scope.add(name) }
            }
            is KeyLangParser.FunDeclarationContext -> function(statement)
            is KeyLangParser.ExpressionStatementContext -> Statement(compileRoot(statement.expressionRoot()), statement, -1, false)
            else -> throw IllegalStateException("unknown statement '${statement.text}'")
        }
    }

    /**
     * the declaration of the function of [ctx], after compiling its body, which can not call it: a statement that
     * evaluates to the function in the frame it is declared in
     */
    private fun function(ctx: KeyLangParser.FunDeclarationContext): Statement {
        val scope = scopes.last()
        val id = ctx.ID(0)!!
        val parameters = ctx.ID().drop(1)
        val slot = slotName(id.text.replace("`", ""), parameters.size)
        if (parameters.size > 5) return Statement(TypedNode { failure(ctx, "functions of more than 5 parameters are not supported") }, ctx, -1, true)
        if (slot in scope) return Statement(declaredTwice(id, ExpressionProblem.Kind.DUPLICATE_FUNCTION), ctx, -1, true)
        val names = ArrayList<String>()
        for (parameter in parameters) {
            val name = parameter.text.replace("`", "")
            if (name in names) return Statement(declaredTwice(parameter, ExpressionProblem.Kind.DUPLICATE_VALUE), ctx, -1, true)
            names.add(name)
        }
        scopes.add(names)
        val body = ctx.block()?.let(::block) ?: compileRoot(ctx.expressionRoot()!!)
        scopes.removeAt(scopes.lastIndex)
        val slots = names.size
        scope.add(slot)
        // a lambda in the function, also in the functions it declares, can keep the frame of a call
        val number = if (hasLambda(ctx)) -1 else reusable++
        val node = object : TypedNode {
            /** the function of the frame before, which is that of this evaluation when the frames are those of before */
            private var last: DeclaredFunction? = null

            override fun eval(frame: Frame): Any {
                val l = last
                if (l != null && l.frame === frame) return l
                return DeclaredFunction(frame, body, slots, number).also { last = it }
            }
        }
        return Statement(node, ctx, scope.size - 1, true)
    }

    private fun hasLambda(tree: org.antlr.v4.kotlinruntime.tree.ParseTree): Boolean {
        if (tree is KeyLangParser.FunctionLiteralContext) return true
        for (i in 0 until tree.childCount) if (hasLambda(tree.getChild(i)!!)) return true
        return false
    }

    /** the statements of [ctx] and the expression that they end in, which gives the value */
    private fun block(ctx: KeyLangParser.BlockContext): TypedNode = body(ctx.statement(), ctx.expressionRoot()!!)

    /** [statements], of a function or a lambda, and the expression [result] that they end in, which gives the value */
    private fun body(statements: List<KeyLangParser.StatementContext>, result: KeyLangParser.ExpressionRootContext): TypedNode {
        val compiled = statements.map(::compileStatement)
        val value = compileRoot(result)
        if (compiled.isEmpty()) return value
        return object : TypedNode {
            fun run(f: Frame) {
                for (s in compiled) {
                    val d = s.node.evalDouble(f)
                    val o = f.take()
                    if (s.slot >= 0) f.store(s.slot, d, o)
                }
            }

            override fun eval(frame: Frame): Any {
                run(frame)
                return value.eval(frame)
            }

            override fun evalDouble(frame: Frame): Double {
                run(frame)
                return value.evalDouble(frame)
            }
        }
    }

    //<editor-fold desc="errors">
    private fun failure(ctx: ParserRuleContext, message: String): Nothing {
        val range = span(ctx)
        throw Failure(message, ExpressionProblem(ExpressionProblem.Kind.EVALUATION, range.first, range.last + 1, message))
    }

    /** the declaration of [id], a value, a parameter or a function, of [kind], that is declared already */
    private fun declaredTwice(id: TerminalNode, kind: ExpressionProblem.Kind): TypedNode {
        val name = id.text.replace("`", "")
        val range = span(id.symbol)
        val problem = ExpressionProblem(kind, range.first, range.last + 1, "$name is declared already", name)
        return TypedNode { throw Failure("'$name' is declared already", problem) }
    }

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
        is KeyLangParser.IntLiteralContext, is KeyLangParser.DecimalLiteralContext -> NumberNode(ctx.text.toDouble())
        is KeyLangParser.StringLiteralContext -> ctx.parts.joinToString("") { it.text }.let { v -> TypedNode { v } }
        is KeyLangParser.ListLiteralContext -> ctx.expressionRoot().map(::compileRoot).let { items ->
            TypedNode { f -> items.map { it.eval(f) }.let { list -> if (list.any { it === Missing }) Missing else list } }
        }
        is KeyLangParser.ParenExpressionContext -> compileRoot(ctx.expressionRoot())
        is KeyLangParser.LambdaExpressionContext -> lambda(ctx.lambda() as KeyLangParser.FunctionLiteralContext)
        is KeyLangParser.ValueReferenceContext -> value(ctx.ID()!!)
        is KeyLangParser.MinusExpressionContext -> minus(ctx)
        is KeyLangParser.NegateExpressionContext -> unary(compile(ctx.expression()), { v -> (!v.truth()).number() }) { v ->
            when (v) {
                Missing -> Missing
                else -> failure(ctx, "'!' takes a number, not ${typeName(v)}")
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
            object : TypedNode {
                /** whether the condition holds, or null when it is missing */
                fun holds(f: Frame): Boolean? {
                    val c = condition.evalDouble(f)
                    return when (val o = f.take()) {
                        null -> c.truth()
                        Missing -> null
                        else -> failure(ctx, "a condition is a number, not ${typeName(o)}")
                    }
                }

                override fun eval(frame: Frame): Any = when (holds(frame)) {
                    null -> Missing
                    true -> yes.eval(frame)
                    false -> no.eval(frame)
                }

                override fun evalDouble(frame: Frame): Double = when (holds(frame)) {
                    null -> frame.spill(Missing)
                    true -> yes.evalDouble(frame)
                    false -> no.evalDouble(frame)
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

    /**
     * a lambda, a function of one or two parameters, `it` when it names none, of which the body is an expression or
     * statements that end in one, over one or more lines
     */
    private fun lambda(ctx: KeyLangParser.FunctionLiteralContext): TypedNode {
        val names = ctx.ID().map { it.text.replace("`", "") }
        if (names.size > 2) return TypedNode { failure(ctx, "lambdas with ${names.size} parameters are not supported") }
        val scope = names.ifEmpty { listOf("it") }.toMutableList()
        scopes.add(scope)
        val body = body(ctx.statement(), ctx.expressionRoot()!!)
        scopes.removeAt(scopes.lastIndex)
        // the parameters, then the values and functions declared in the body
        val slots = scope.size
        return if (names.size == 2) {
            TypedNode { f -> { a: Any, b: Any -> body.eval(Frame(f, arrayOfNulls<Any?>(slots).also { it[0] = a; it[1] = b }, f.errors, Context.of(reusable))) } }
        } else {
            TypedNode { f -> { a: Any -> body.eval(Frame(f, arrayOfNulls<Any?>(slots).also { it[0] = a }, f.errors, Context.of(reusable))) } }
        }
    }

    /** the value of [id]: a parameter of a lambda around it or of the expression, PI, or a constant at every evaluation */
    private fun value(id: TerminalNode): TypedNode {
        val name = id.text.replace("`", "")
        for (depth in 0 until scopes.size) {
            val index = scopes[scopes.lastIndex - depth].indexOf(name)
            if (index >= 0) return object : TypedNode {
                /** the frame of the slot, [depth] frames out */
                fun owner(f: Frame): Frame {
                    var owner = f
                    repeat(depth) { owner = owner.parent!! }
                    return owner
                }

                override fun eval(frame: Frame): Any =
                    owner(frame).value(index) ?: missing(frame, "unresolved value: '$name'", id, ExpressionProblem.Kind.UNKNOWN_VALUE)

                override fun evalDouble(frame: Frame): Double {
                    val owner = owner(frame)
                    val v = owner.values[index]
                    if (v === Unboxed) return owner.numbers!![index]
                    return frame.spill(v ?: missing(frame, "unresolved value: '$name'", id, ExpressionProblem.Kind.UNKNOWN_VALUE))
                }
            }
        }
        if (name == "PI") return NumberNode(PI)
        return TypedNode { f ->
            constants(name) ?: missing(f, "unresolved value: '$name'. Available constant: $constants", id, ExpressionProblem.Kind.UNKNOWN_VALUE)
        }
    }

    private fun minus(ctx: KeyLangParser.MinusExpressionContext): TypedNode {
        val a = compile(ctx.expression())
        return unary(a, { v -> -v }) { v ->
            when (v) {
                Missing -> Missing
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
            KeyLangLexer.Tokens.ASTERISK -> binary(a, b, { l, r -> l * r }) { left, right ->
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
            KeyLangLexer.Tokens.DIVISION -> binary(a, b, { l, r -> l / r }) { left, right ->
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
            else -> binary(a, b, { l, r -> l.mod(r) }) { left, right ->
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
        return if (ctx.operator?.type == KeyLangLexer.Tokens.PLUS) binary(a, b, { l, r -> l + r }) { left, right ->
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
        } else binary(a, b, { l, r -> l - r }) { left, right ->
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
        val other = { left: Any, right: Any ->
            comparisonOf(ctx, operator, symbol, left, right)
        }
        return when (operator) {
            KeyLangLexer.Tokens.EQ -> binary(a, b, { l, r -> (l == r).number() }, other)
            KeyLangLexer.Tokens.LT -> binary(a, b, { l, r -> (l < r).number() }, other)
            KeyLangLexer.Tokens.LTEQ -> binary(a, b, { l, r -> (l <= r).number() }, other)
            KeyLangLexer.Tokens.GT -> binary(a, b, { l, r -> (l > r).number() }, other)
            else -> binary(a, b, { l, r -> (l >= r).number() }, other)
        }
    }

    /** [left] and [right] compared by [operator], of which one is no number */
    private fun comparisonOf(ctx: ParserRuleContext, operator: Int?, symbol: String, left: Any, right: Any): Any {
        if (left === Missing || right === Missing) return Missing
        return if (operator == KeyLangLexer.Tokens.EQ) {
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
        // a function declared before the call, in the frame [depth] frames out
        val slot = slotName(name, n)
        for (depth in 0 until scopes.size) {
            val index = scopes[scopes.lastIndex - depth].indexOf(slot)
            if (index >= 0) return object : DoubleNode() {
                // the arguments go to the slots of the frame of the call, and a number comes back without boxing it
                override fun evalDouble(frame: Frame): Double {
                    var outer = frame
                    repeat(depth) { outer = outer.parent!! }
                    val function = outer.values[index] as DeclaredFunction
                    // the errors are those of the statement that calls it
                    val call = frame.context.frame(function, frame.errors)
                    try {
                        var missing = false
                        for (i in 0 until n) {
                            val d = args[i].evalDouble(frame)
                            val o = frame.take()
                            if (o === Missing) missing = true
                            call.store(i, d, o)
                        }
                        if (missing) return frame.spill(Missing)
                        val d = function.body.evalDouble(call)
                        return frame.spill(call.take() ?: return d)
                    } finally {
                        call.busy = false
                    }
                }
            }
        }
        val known = dispatch(name, n)
        // a built-in function of numbers, of which the arguments are numbers, without boxing them
        val double = doubleFunction(name, n)
        if (known != null && double != null) {
            /** the built-in function of the arguments [x] that are not all numbers */
            fun other(vararg x: Any): Any = if (x.any { it === Missing }) Missing else known(arrayOf(*x))
            val a0 = args[0]
            return when (double) {
                is DoubleFunction1 -> unary(a0, { double.call(it) }) { other(it) }
                is DoubleFunction2 -> binary(a0, args[1], { x, y -> double.call(x, y) }) { x, y -> other(x, y) }
                else -> object : DoubleNode() {
                    val f3 = double as DoubleFunction3
                    val a1 = args[1]
                    val a2 = args[2]
                    override fun evalDouble(frame: Frame): Double {
                        val x = a0.evalDouble(frame)
                        val xo = frame.take()
                        val y = a1.evalDouble(frame)
                        val yo = frame.take()
                        val z = a2.evalDouble(frame)
                        val zo = frame.take()
                        if (xo == null && yo == null && zo == null) return f3.call(x, y, z)
                        return frame.spill(other(xo ?: x, yo ?: y, zo ?: z))
                    }
                }
            }
        }
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
