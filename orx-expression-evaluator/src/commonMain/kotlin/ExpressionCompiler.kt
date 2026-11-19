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
 *
 * An expression can be a small program, of statements separated by semicolons or new lines, that declare values with
 * `val` and functions with `fun` for the statements after them:
 * `fun f(x) { x * 5.0 }; fun g(x) = x * 6.0; val a = f(5); val b = g(a); a + b`. The body of a function is an
 * expression or a block of statements that ends in one; it sees its parameters and the values and functions declared
 * before it, not itself. A value is declared once in its program or body, a function once with as many parameters, at
 * most 5; they come before the constants and the functions of the same name.
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

    /**
     * the values and functions declared in a program or in the body of a function, those declared so far while it is
     * compiled, with the [values] of its parameters and values while it is evaluated. A function only calls the
     * functions declared before it, so it is never called while it is evaluated, and its values need no frames
     */
    private class Scope(val parent: Scope?) {
        val names = ArrayList<String>()

        /** the functions by name and number of parameters */
        val functions = HashMap<Pair<String, Int>, Function>()
        var values = DoubleArray(0)
    }

    /** a declared function, of which the parameters are the first values of [scope] */
    private class Function(val scope: Scope, val body: Node)

    /** a statement: its node, its text for the messages, the slot of the value it declares or -1, and whether it declares */
    private class Statement(val node: Node, val text: String, val slot: Int, val declares: Boolean)

    private val top = Scope(null)

    /** the scope of what is being compiled */
    private var scope = top

    /** the statements, of which the last gives the result */
    private val statements: List<Statement> = root.line().map { compileStatement(it.statement()!!) }

    init {
        top.values = DoubleArray(top.names.size)
    }

    /** evaluates the expression, null when it has no statements or the last declares a value or a function */
    fun evaluate(): Double? {
        var result: Double? = null
        for (statement in statements) {
            errors.clear()
            val value = statement.node.eval()
            if (errors.isNotEmpty()) {
                // the message of the last error, the problems of all of them
                val problems = errors.flatMap { it.problems }.distinct().sortedBy { it.start }
                val last = errors.last()
                errors.clear()
                throw ExpressionException("error in evaluation of '${statement.text}': ${last.message ?: ""}", problems)
            }
            if (statement.slot >= 0) top.values[statement.slot] = value
            result = if (statement.declares) null else value
        }
        return result
    }

    private fun compileStatement(statement: KeyLangParser.StatementContext): Statement = when (statement) {
        is KeyLangParser.ValDeclarationContext -> {
            // the value is declared after its expression, which can read a constant of the same name
            val node = compileRoot(statement.expressionRoot())
            val id = statement.ID()!!
            val name = id.text.replace("`", "")
            if (name in scope.names) Statement(declaredTwice(id, ExpressionProblem.Kind.DUPLICATE_VALUE), statement.text, -1, true)
            else Statement(node, statement.text, scope.names.size, true).also { scope.names.add(name) }
        }
        is KeyLangParser.FunDeclarationContext -> Statement(function(statement), statement.text, -1, true)
        is KeyLangParser.ExpressionStatementContext -> Statement(compileRoot(statement.expressionRoot()), statement.text, -1, false)
        else -> throw IllegalStateException("unknown statement '${statement.text}'")
    }

    /**
     * declares the function of [ctx] in the current scope, after compiling its body, which can not call it; a node
     * that evaluates to nothing, or to the error of a declaration that is not possible
     */
    private fun function(ctx: KeyLangParser.FunDeclarationContext): Node {
        val id = ctx.ID(0)!!
        val name = id.text.replace("`", "")
        val parameters = ctx.ID().drop(1)
        if (parameters.size > 5) {
            return failing(ExpressionProblem.Kind.UNSUPPORTED, span(ctx), "functions of more than 5 parameters are not supported")
        }
        if ((name to parameters.size) in scope.functions) return declaredTwice(id, ExpressionProblem.Kind.DUPLICATE_FUNCTION)
        val body = Scope(scope)
        for (parameter in parameters) {
            val parameterName = parameter.text.replace("`", "")
            if (parameterName in body.names) return declaredTwice(parameter, ExpressionProblem.Kind.DUPLICATE_VALUE)
            body.names.add(parameterName)
        }
        val outer = scope
        scope = body
        val node = ctx.block()?.let(::block) ?: compileRoot(ctx.expressionRoot()!!)
        scope = outer
        body.values = DoubleArray(body.names.size)
        scope.functions[name to parameters.size] = Function(body, node)
        return Node { 0.0 }
    }

    /** the statements of [ctx] and the expression that they end in, which gives the value */
    private fun block(ctx: KeyLangParser.BlockContext): Node {
        val statements = ctx.statement().map(::compileStatement)
        val result = compileRoot(ctx.expressionRoot()!!)
        val values = scope
        return Node {
            for (s in statements) {
                val v = s.node.eval()
                if (s.slot >= 0) values.values[s.slot] = v
            }
            result.eval()
        }
    }

    /**
     * an error about [node], the name of a value or a function, whose value is NaN; a function takes the numbers of
     * arguments of [declared] too
     */
    private fun error(message: String, node: TerminalNode, kind: ExpressionProblem.Kind, declared: Set<Int> = emptySet()): Double {
        val name = node.text.replace("`", "")
        val token = node.symbol
        val call = node.getParent() as? ParserRuleContext
        val takes = arities(name, functions) + declared
        val problem = when {
            kind == ExpressionProblem.Kind.UNKNOWN_VALUE -> span(token).let { ExpressionProblem(kind, it.first, it.last + 1, "no value $name", name) }
            // a function that takes other numbers of arguments: the call
            takes.isNotEmpty() && call != null -> span(call).let {
                val arguments = (0 until call.childCount).count { i -> call.getChild(i) is KeyLangParser.ExpressionContext }
                ExpressionProblem(ExpressionProblem.Kind.WRONG_ARGUMENTS, it.first, it.last + 1, argumentsMessage(name, takes, arguments), name)
            }
            else -> span(token).let { ExpressionProblem(ExpressionProblem.Kind.UNKNOWN_FUNCTION, it.first, it.last + 1, "no function $name", name) }
        }
        errors.add(ExpressionException(message, listOf(problem)))
        return Double.NaN
    }

    /** a node that evaluates to the error of a [kind] at [range], NaN */
    private fun failing(kind: ExpressionProblem.Kind, range: IntRange, message: String, name: String? = null): Node {
        val problem = ExpressionProblem(kind, range.first, range.last + 1, message, name)
        return Node {
            errors.add(ExpressionException(message, listOf(problem)))
            Double.NaN
        }
    }

    /** an error about the declaration of [id], a value or a function, of [kind], that is declared already */
    private fun declaredTwice(id: TerminalNode, kind: ExpressionProblem.Kind): Node {
        val name = id.text.replace("`", "")
        return failing(kind, span(id.symbol), "$name is declared already", name)
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

    /** the value of [id]: a parameter or a value declared before it, PI, or a constant read at every evaluation */
    private fun value(id: TerminalNode): Node {
        val name = id.text.replace("`", "")
        var s: Scope? = scope
        while (s != null) {
            val slot = s.names.indexOf(name)
            val owner = s
            if (slot >= 0) return Node { owner.values[slot] }
            s = s.parent
        }
        if (name == "PI") return constant(PI)
        return Node {
            constants[name] ?: error("unresolved value: '${name}'. available values: $constants", id, ExpressionProblem.Kind.UNKNOWN_VALUE)
        }
    }

    /** the call of [id] with [arguments]: a declared function, a built-in function, a function of [functions], or an error */
    private fun call(id: TerminalNode, arguments: List<KeyLangParser.ExpressionContext>): Node {
        val name = id.text.replace("`", "")
        val args = arguments.map { compile(it) }
        declared(name, args)?.let { return it }
        builtIn(name, args)?.let { return it }
        extension(name, args)?.let { return it }
        // a function that is not there yet, found at every evaluation
        val signature = if (args.isEmpty()) "()" else args.indices.joinToString(", ", "(", ")") { "x$it" }
        val message = "unresolved function: '$name$signature'"
        val declared = generateSequence(scope) { it.parent }.flatMap { s -> s.functions.keys.filter { it.first == name }.map { it.second } }.toSet()
        return Node { extension(name, args)?.eval() ?: error(message, id, ExpressionProblem.Kind.UNKNOWN_FUNCTION, declared) }
    }

    /** the call of the function [name] declared before it with [args], null when none takes as many */
    private fun declared(name: String, args: List<Node>): Node? {
        val f = generateSequence(scope) { it.parent }.firstNotNullOfOrNull { it.functions[name to args.size] } ?: return null
        return when (args.size) {
            0 -> Node { f.body.eval() }
            1 -> { val (x) = args; Node { f.scope.values[0] = x.eval(); f.body.eval() } }
            // all the arguments before the parameters, as an argument can call the function too
            else -> Node {
                val values = DoubleArray(args.size) { args[it].eval() }
                values.copyInto(f.scope.values)
                f.body.eval()
            }
        }
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
