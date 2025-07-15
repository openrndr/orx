package org.openrndr.extra.expressions.typed

import org.antlr.v4.kotlinruntime.*
import org.antlr.v4.kotlinruntime.tree.ParseTreeWalker
import org.antlr.v4.kotlinruntime.tree.TerminalNode
import org.openrndr.collections.pop
import org.openrndr.collections.push
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.expressions.parser.KeyLangLexer
import org.openrndr.extra.expressions.parser.KeyLangLexer.Tokens.RANGE_DOWNTO
import org.openrndr.extra.expressions.parser.KeyLangLexer.Tokens.RANGE_EXCLUSIVE
import org.openrndr.extra.expressions.parser.KeyLangLexer.Tokens.RANGE_EXCLUSIVE_UNTIL
import org.openrndr.extra.expressions.parser.KeyLangLexer.Tokens.RANGE_INCLUSIVE
import org.openrndr.extra.expressions.parser.KeyLangParser
import org.openrndr.extra.expressions.parser.KeyLangParserBaseListener
import org.openrndr.extra.noise.uniform
import org.openrndr.math.*
import kotlin.math.PI
import kotlin.math.roundToInt

typealias TypedFunction0 = () -> Any
typealias TypedFunction1 = (Any) -> Any
typealias TypedFunction2 = (Any, Any) -> Any
typealias TypedFunction3 = (Any, Any, Any) -> Any
typealias TypedFunction4 = (Any, Any, Any, Any) -> Any
typealias TypedFunction5 = (Any, Any, Any, Any, Any) -> Any


private fun ArrayDeque<Any>.pushChecked(item: Any) {
//    require(item is Double || item is Vector2 || item is Vector3 || item is Vector4 || item is Map<*, *> || item is Matrix44) {
//
//        "$item ${item::class}"
//    }
    push(item)
}

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

enum class IDType {
    VARIABLE,
    PROPERTY,
    MEMBER_FUNCTION0,
    MEMBER_FUNCTION1,
    MEMBER_FUNCTION2,
    MEMBER_FUNCTION3,
    FUNCTION0,
    FUNCTION1,
    FUNCTION2,
    FUNCTION3,
    FUNCTION4,
    FUNCTION5,
    FUNCTION_ARGUMENT
}

/**
 * A base class for handling typed expressions in a custom language parser.
 * This class provides an extensive set of overrides for various parser rules
 * to allow custom implementation when these rules are triggered during parsing.
 *
 * @property functions Represents a mapping of functions categorized by their arity.
 * @property constants Tracks constants identified during parsing.
 * @property state Maintains the current state of the listener, preserving contextual information.
 *
 * Methods primarily handle entering and exiting parser rules for expressions, statements, and
 * function calls, offering hooks to extend or modify behavior for each parsing scenario. Additionally,
 * utility methods are provided to handle and propagate errors during parsing.
 */
abstract class TypedExpressionListenerBase(
    val functions: TypedFunctionExtensions = TypedFunctionExtensions.EMPTY,
    val constants: (String) -> Any? = { null }
) :
    KeyLangParserBaseListener() {

    class State {
        val valueStack = ArrayDeque<Any>()
        val functionStack = ArrayDeque<(Array<Any>) -> Any>()
        val propertyStack = ArrayDeque<String>()

        val idTypeStack = ArrayDeque<IDType>()
        var lastExpressionResult: Any? = null

        val exceptionStack = ArrayDeque<ExpressionException>()

        var inFunctionLiteral = 0

        fun reset() {
            valueStack.clear()
            functionStack.clear()
            propertyStack.clear()
            idTypeStack.clear()
            lastExpressionResult = null
            exceptionStack.clear()
            inFunctionLiteral = 0
        }
    }

    abstract val state: State
    override fun enterLine(ctx: KeyLangParser.LineContext) {
        val s = state
        s.reset()
    }

    override fun exitRangeExpression(ctx: KeyLangParser.RangeExpressionContext) {
        val s = state
        if (s.inFunctionLiteral > 0) {
            return
        }

        val step: Any?
        if (ctx.step != null) {
            step = s.valueStack.pop()
        } else {
            step = null
        }

        val right = s.valueStack.pop()
        val left = s.valueStack.pop()


        val lower = (left as Double).toInt()
        val upper = (right as Double).toInt()
        val list = if (step == null) {
            when (ctx.operator?.type) {
                RANGE_INCLUSIVE -> (lower..upper).toList().map { it.toDouble() }
                RANGE_EXCLUSIVE -> (lower..<upper).toList().map { it.toDouble() }
                RANGE_EXCLUSIVE_UNTIL -> (lower until upper).toList().map { it.toDouble() }
                RANGE_DOWNTO -> (lower downTo upper).toList().map { it.toDouble() }
                else -> error("unsupported operator: '${ctx.operator?.text}'")
            }
        } else {
            val stepSize = (step as Double).toInt()
            when (ctx.operator?.type) {
                RANGE_INCLUSIVE -> (lower..upper step stepSize).toList().map { it.toDouble() }
                RANGE_EXCLUSIVE -> (lower..<upper step stepSize).toList().map { it.toDouble() }
                RANGE_EXCLUSIVE_UNTIL -> (lower until upper step stepSize).toList().map { it.toDouble() }
                RANGE_DOWNTO -> (lower downTo upper step stepSize).toList().map { it.toDouble() }
                else -> error("unsupported operator: '${ctx.operator?.text}'")
            }
        }
        s.valueStack.push(list)

    }

    override fun exitListLiteral(ctx: KeyLangParser.ListLiteralContext) {
        val s = state
        if (s.inFunctionLiteral > 0) {
            return
        }
        val list = (0 until ctx.expressionRoot().size).map { s.valueStack.pop() }
        s.valueStack.push(list.reversed())
    }

    override fun exitIndexExpression(ctx: KeyLangParser.IndexExpressionContext) {
        val s = state
        if (s.inFunctionLiteral > 0) {
            return
        }
        val index = (s.valueStack.pop() as? Double)?.roundToInt() ?: error("index is not a number")
        val listValue = s.valueStack.pop()

        @Suppress("UNCHECKED_CAST") val value = when (listValue) {
            is List<*> -> listValue[index] ?: error("got null")
            is Function<*> -> (listValue as (Int) -> Any)(index)
            else -> error("can't index on '$listValue'")
        }
        s.valueStack.push(value)
    }

    override fun enterFunctionLiteral(ctx: KeyLangParser.FunctionLiteralContext) {
        val s = state
        s.inFunctionLiteral++
    }

    override fun exitFunctionLiteral(ctx: KeyLangParser.FunctionLiteralContext) {
        val s = state
        s.inFunctionLiteral--
        val functionExpr = ctx.expression().text

        val ids = ctx.ID()
        val f = when (ids.size) {
            0 -> compileFunction1<Any, Any>(functionExpr, "it", constants, functions)
            1 -> compileFunction1<Any, Any>(functionExpr, ids[0].text, constants, functions)
            2 -> compileFunction2<Any, Any, Any>(functionExpr, ids[0].text, ids[1].text, constants, functions)
            else -> error("functions with ${ids.size} parameters are not supported")

        }
        s.valueStack.push(f)
    }

    override fun exitExpressionStatement(ctx: KeyLangParser.ExpressionStatementContext) {
        val s = state
        if (s.inFunctionLiteral > 0) {
            return
        }

        ifError {
            throw ExpressionException("error in evaluation of '${ctx.text}': ${it.message ?: ""}")
        }
        val result = state.valueStack.pop()
        s.lastExpressionResult = result
    }

    override fun exitMinusExpression(ctx: KeyLangParser.MinusExpressionContext) {
        val s = state
        if (s.inFunctionLiteral > 0) {
            return
        }
        val op = s.valueStack.pop()
        s.valueStack.pushChecked(
            when (op) {
                is Double -> -op
                is Vector3 -> -op
                is Vector2 -> -op
                is Vector4 -> -op
                is Matrix44 -> op * -1.0
                else -> error("unsupported type")
            }
        )
    }

    @Suppress("IMPLICIT_CAST_TO_ANY")
    override fun exitBinaryOperation1(ctx: KeyLangParser.BinaryOperation1Context) {
        val s = state
        if (s.inFunctionLiteral > 0) {
            return
        }

        ifError {
            pushError(it.message ?: "")
            return
        }

        val right = s.valueStack.pop()
        val left = s.valueStack.pop()

        val result = when (val operator = ctx.operator?.type) {
            KeyLangLexer.Tokens.ASTERISK -> when {
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
                else -> error("unsupported operands for * operator left:${left::class} right:${right::class}")
            }

            KeyLangLexer.Tokens.DIVISION -> when {
                left is Double && right is Double -> left / right
                left is Vector2 && right is Vector2 -> left / right
                left is Vector2 && right is Double -> left / right
                left is Vector3 && right is Vector3 -> left / right
                left is Vector3 && right is Double -> left / right
                left is Vector4 && right is Vector4 -> left / right
                left is Vector4 && right is Double -> left / right
                left is ColorRGBa && right is Double -> left / right
                else -> error("unsupported operands for - operator left:${left::class} right:${right::class}")
            }

            KeyLangLexer.Tokens.PERCENTAGE -> when {
                left is Double && right is Double -> left.mod(right)
                left is Vector2 && right is Vector2 -> left.mod(right)
                left is Vector3 && right is Vector3 -> left.mod(right)
                left is Vector4 && right is Vector4 -> left.mod(right)
                else -> error("unsupported operands for - operator left:${left::class} right:${right::class}")
            }

            else -> error("operator '$operator' not implemented")
        }
        s.valueStack.pushChecked(result)
    }

    @Suppress("IMPLICIT_CAST_TO_ANY")
    override fun exitBinaryOperation2(ctx: KeyLangParser.BinaryOperation2Context) {
        val s = state
        if (s.inFunctionLiteral > 0) {
            return
        }


        ifError {
            pushError(it.message ?: "")
            return
        }

        val right = s.valueStack.pop()
        val left = s.valueStack.pop()

        val result = when (val operator = ctx.operator?.type) {
            KeyLangLexer.Tokens.PLUS -> when {
                left is Double && right is Double -> left + right
                left is Vector2 && right is Vector2 -> left + right
                left is Vector3 && right is Vector3 -> left + right
                left is Vector4 && right is Vector4 -> left + right
                left is Matrix44 && right is Matrix44 -> left + right
                left is ColorRGBa && right is ColorRGBa -> left + right
                left is String && right is String -> left + right
                left is List<*> && right is List<*> -> left + right
                else -> error("unsupported operands for + operator left:${left::class} right:${right::class}")
            }

            KeyLangLexer.Tokens.MINUS -> when {
                left is Double && right is Double -> left - right
                left is Vector2 && right is Vector2 -> left - right
                left is Vector3 && right is Vector3 -> left - right
                left is Vector4 && right is Vector4 -> left - right
                left is Matrix44 && right is Matrix44 -> left - right
                left is ColorRGBa && right is ColorRGBa -> left - right
                else -> error("unsupported operands for - operator left:${left::class} right:${right::class}")
            }

            else -> error("operator '$operator' not implemented")
        }
        s.valueStack.pushChecked(result)
    }

    override fun exitJoinOperation(ctx: KeyLangParser.JoinOperationContext) {
        val s = state
        if (s.inFunctionLiteral > 0) {
            return
        }

        val right = (s.valueStack.pop() as Double).roundToInt()
        val left = (s.valueStack.pop() as Double).roundToInt()

        val result = when (val operator = ctx.operator?.type) {
            KeyLangLexer.Tokens.AND -> right != 0 && left != 0
            KeyLangLexer.Tokens.OR -> right != 0 || left != 0
            else -> error("operator '$operator' not implemented")
        }
        s.valueStack.pushChecked(if (result) 1.0 else 0.0)
    }

    override fun exitComparisonOperation(ctx: KeyLangParser.ComparisonOperationContext) {
        val s = state
        if (s.inFunctionLiteral > 0) {
            return
        }

        val right = s.valueStack.pop()
        val left = s.valueStack.pop()

        val result = when (val operator = ctx.operator?.type) {
            KeyLangLexer.Tokens.EQ -> when {
                left is Double && right is Double -> left == right
                left is Vector2 && right is Vector2 -> left == right
                left is Vector3 && right is Vector3 -> left == right
                left is Vector4 && right is Vector4 -> left == right
                left is ColorRGBa && right is ColorRGBa -> left == right
                left is String && right is String -> left == right
                else -> error("unsupported operands for == operator left:${left::class} right:${right::class}")
            }

            KeyLangLexer.Tokens.LTEQ -> when {
                left is Double && right is Double -> left <= right
                else -> error("unsupported operands for <= operator left:${left::class} right:${right::class}")
            }

            KeyLangLexer.Tokens.LT -> when {
                left is Double && right is Double -> left < right
                else -> error("unsupported operands for < operator left:${left::class} right:${right::class}")
            }

            KeyLangLexer.Tokens.GTEQ -> when {
                left is Double && right is Double -> left >= right
                else -> error("unsupported operands for >= operator left:${left::class} right:${right::class}")
            }

            KeyLangLexer.Tokens.GT -> when {
                left is Double && right is Double -> left > right
                else -> error("unsupported operands for > operator left:${left::class} right:${right::class}")
            }

            else -> error("operator '$operator' not implemented")
        }
        s.valueStack.pushChecked(if (result) 1.0 else 0.0)
    }

    override fun exitNegateExpression(ctx: KeyLangParser.NegateExpressionContext) {
        val s = state
        if (s.inFunctionLiteral > 0) {
            return
        }

        val operand = (s.valueStack.pop() as Double).roundToInt()
        s.valueStack.pushChecked(if (operand == 0) 1.0 else 0.0)
    }

    override fun exitTernaryExpression(ctx: KeyLangParser.TernaryExpressionContext) {
        val s = state
        if (s.inFunctionLiteral > 0) {
            return
        }

        val right = s.valueStack.pop()
        val left = s.valueStack.pop()
        val comp = s.valueStack.pop()

        val result = when (comp) {
            is Double -> if (comp.roundToInt() != 0) left else right
            else -> error("can't compare")
        }
        s.valueStack.pushChecked(result)
    }

    override fun enterValueReference(ctx: KeyLangParser.ValueReferenceContext) {
        val s = state
        if (s.inFunctionLiteral > 0) {
            return
        }

        s.idTypeStack.push(IDType.VARIABLE)
    }

    override fun enterMemberFunctionCall0Expression(ctx: KeyLangParser.MemberFunctionCall0ExpressionContext) {
        val s = state
        if (s.inFunctionLiteral > 0) {
            return
        }

        s.idTypeStack.push(IDType.MEMBER_FUNCTION1)
    }

    override fun exitMemberFunctionCall0Expression(ctx: KeyLangParser.MemberFunctionCall0ExpressionContext) {
        val s = state
        if (s.inFunctionLiteral > 0) {
            return
        }

        ifError {
            pushError(it.message ?: "")
            return
        }
        s.valueStack.pushChecked(s.functionStack.pop().invoke(emptyArray()))
    }

    override fun enterMemberFunctionCall1Expression(ctx: KeyLangParser.MemberFunctionCall1ExpressionContext) {
        val s = state
        if (s.inFunctionLiteral > 0) {
            return
        }

        s.idTypeStack.push(IDType.MEMBER_FUNCTION1)
    }

    override fun exitMemberFunctionCall1Expression(ctx: KeyLangParser.MemberFunctionCall1ExpressionContext) {
        val s = state
        if (s.inFunctionLiteral > 0) {
            return
        }

        ifError {
            pushError(it.message ?: "")
            return
        }
        s.valueStack.pushChecked(s.functionStack.pop().invoke(arrayOf(s.valueStack.pop())))
    }

    override fun enterMemberFunctionCall2Expression(ctx: KeyLangParser.MemberFunctionCall2ExpressionContext) {
        val s = state
        if (s.inFunctionLiteral > 0) {
            return
        }

        s.idTypeStack.push(IDType.MEMBER_FUNCTION2)
    }

    override fun exitMemberFunctionCall2Expression(ctx: KeyLangParser.MemberFunctionCall2ExpressionContext) {
        val s = state
        if (s.inFunctionLiteral > 0) {
            return
        }

        ifError {
            pushError(it.message ?: "")
            return
        }
        val argument1 = s.valueStack.pop()
        val argument0 = s.valueStack.pop()

        s.valueStack.pushChecked(s.functionStack.pop().invoke(arrayOf(argument0, argument1)))
    }

    override fun enterMemberFunctionCall3Expression(ctx: KeyLangParser.MemberFunctionCall3ExpressionContext) {
        val s = state
        if (s.inFunctionLiteral > 0) {
            return
        }

        s.idTypeStack.push(IDType.MEMBER_FUNCTION3)
    }

    override fun exitMemberFunctionCall3Expression(ctx: KeyLangParser.MemberFunctionCall3ExpressionContext) {
        val s = state
        if (s.inFunctionLiteral > 0) {
            return
        }

        ifError {
            pushError(it.message ?: "")
            return
        }
        val argument2 = s.valueStack.pop()
        val argument1 = s.valueStack.pop()
        val argument0 = s.valueStack.pop()

        s.valueStack.pushChecked(s.functionStack.pop().invoke(arrayOf(argument0, argument1, argument2)))
    }

    override fun enterMemberFunctionCall0LambdaExpression(ctx: KeyLangParser.MemberFunctionCall0LambdaExpressionContext) {
        val s = state
        if (s.inFunctionLiteral > 0) {
            return
        }

        s.idTypeStack.push(IDType.MEMBER_FUNCTION1)
    }

    override fun exitMemberFunctionCall0LambdaExpression(ctx: KeyLangParser.MemberFunctionCall0LambdaExpressionContext) {
        val s = state
        if (s.inFunctionLiteral > 0) {
            return
        }

        ifError {
            pushError(it.message ?: "")
            return
        }
        s.valueStack.pushChecked(s.functionStack.pop().invoke(arrayOf(s.valueStack.pop())))
    }


    override fun enterFunctionCall0Expression(ctx: KeyLangParser.FunctionCall0ExpressionContext) {
        val s = state
        if (s.inFunctionLiteral > 0) {
            return
        }

        s.idTypeStack.push(IDType.FUNCTION0)
    }

    override fun exitFunctionCall0Expression(ctx: KeyLangParser.FunctionCall0ExpressionContext) {
        val s = state
        if (s.inFunctionLiteral > 0) {
            return
        }

        ifError {
            pushError(it.message ?: "")
            return
        }

        val function = s.functionStack.pop()
        val result = function.invoke(arrayOf())
        s.valueStack.pushChecked(result)
    }

    override fun enterFunctionCall1Expression(ctx: KeyLangParser.FunctionCall1ExpressionContext) {
        val s = state
        if (s.inFunctionLiteral > 0) {
            return
        }

        s.idTypeStack.push(IDType.FUNCTION1)
    }

    override fun exitFunctionCall1Expression(ctx: KeyLangParser.FunctionCall1ExpressionContext) {
        val s = state
        if (s.inFunctionLiteral > 0) {
            return
        }

        ifError {
            pushError(it.message ?: "")
            return
        }

        val function = s.functionStack.pop()
        val argument = s.valueStack.pop()

        val result = function.invoke(arrayOf(argument))
        s.valueStack.pushChecked(result)
    }

    override fun enterFunctionCall2Expression(ctx: KeyLangParser.FunctionCall2ExpressionContext) {
        val s = state
        if (s.inFunctionLiteral > 0) {
            return
        }
        s.idTypeStack.push(IDType.FUNCTION2)
    }


    override fun exitFunctionCall2Expression(ctx: KeyLangParser.FunctionCall2ExpressionContext) {
        val s = state
        if (s.inFunctionLiteral > 0) {
            return
        }

        ifError {
            pushError(it.message ?: "")
            return
        }

        val function = s.functionStack.pop()
        val argument1 = s.valueStack.pop()
        val argument0 = s.valueStack.pop()

        val result = function.invoke(arrayOf(argument0, argument1))
        s.valueStack.pushChecked(result)
    }

    override fun enterFunctionCall3Expression(ctx: KeyLangParser.FunctionCall3ExpressionContext) {
        val s = state
        if (s.inFunctionLiteral > 0) {
            return
        }

        s.idTypeStack.push(IDType.FUNCTION3)
    }

    override fun exitFunctionCall3Expression(ctx: KeyLangParser.FunctionCall3ExpressionContext) {
        val s = state
        if (s.inFunctionLiteral > 0) {
            return
        }

        ifError {
            pushError(it.message ?: "")
            return
        }

        val function = s.functionStack.pop()
        val argument2 = s.valueStack.pop()
        val argument1 = s.valueStack.pop()
        val argument0 = s.valueStack.pop()

        val result = function.invoke(arrayOf(argument0, argument1, argument2))
        s.valueStack.pushChecked(result)
    }

    override fun enterFunctionCall4Expression(ctx: KeyLangParser.FunctionCall4ExpressionContext) {
        val s = state
        if (s.inFunctionLiteral > 0) {
            return
        }

        s.idTypeStack.push(IDType.FUNCTION4)
    }

    override fun exitFunctionCall4Expression(ctx: KeyLangParser.FunctionCall4ExpressionContext) {
        val s = state
        if (s.inFunctionLiteral > 0) {
            return
        }

        ifError {
            pushError(it.message ?: "")
            return
        }

        val function = s.functionStack.pop()
        val argument3 = s.valueStack.pop()
        val argument2 = s.valueStack.pop()
        val argument1 = s.valueStack.pop()
        val argument0 = s.valueStack.pop()

        val result = function.invoke(arrayOf(argument0, argument1, argument2, argument3))
        s.valueStack.pushChecked(result)
    }


    override fun enterFunctionCall5Expression(ctx: KeyLangParser.FunctionCall5ExpressionContext) {
        val s = state
        if (s.inFunctionLiteral > 0) {
            return
        }

        s.idTypeStack.push(IDType.FUNCTION5)
    }

    override fun exitFunctionCall5Expression(ctx: KeyLangParser.FunctionCall5ExpressionContext) {
        val s = state
        if (s.inFunctionLiteral > 0) {
            return
        }

        ifError {
            pushError(it.message ?: "")
            return
        }

        val function = s.functionStack.pop()
        val argument4 = s.valueStack.pop()
        val argument3 = s.valueStack.pop()
        val argument2 = s.valueStack.pop()
        val argument1 = s.valueStack.pop()
        val argument0 = s.valueStack.pop()

        val result = function.invoke(arrayOf(argument0, argument1, argument2, argument3, argument4))
        s.valueStack.pushChecked(result)
    }

    private fun <T> errorValue(message: String, value: T): T {
        pushError(message)
        return value
    }

    private fun pushError(message: String) {
        val s = state

        s.exceptionStack.push(ExpressionException(message))
    }

    private inline fun ifError(f: (e: Throwable) -> Unit) {
        val s = state
        if (s.exceptionStack.isNotEmpty()) {
            val e = s.exceptionStack.pop()
            f(e)
        }
    }

    override fun enterPropReference(ctx: KeyLangParser.PropReferenceContext) {
        val s = state
        if (s.inFunctionLiteral > 0) {
            return
        }

        s.idTypeStack.push(IDType.PROPERTY)
    }

    override fun exitPropReference(ctx: KeyLangParser.PropReferenceContext) {
        val s = state
        if (s.inFunctionLiteral > 0) {
            return
        }

        val root = s.valueStack.pop()
        var current = root
        val property = s.propertyStack.pop()
        @Suppress("UNCHECKED_CAST")
        current = when (current) {
            is Map<*, *> -> current[property] ?: error("property '$property' not found")
            is Function<*> -> (current as ((String) -> Any?)).invoke(property)
                ?: error("property '$property' not found")

            is Vector2 -> current.property(property)
            is Vector3 -> current.property(property)
            is Vector4 -> current.property(property)
            is ColorRGBa -> current.property(property)
            is Matrix44 -> current.property(property)
            is String -> current.property(property)
            else -> error("can't look up: ${current::class} '$current', root:'$root' ${ctx.text} ")
        }
        s.valueStack.push(current)
    }


    @Suppress("MoveLambdaOutsideParentheses")
    override fun visitTerminal(node: TerminalNode) {
        val s = state
        if (s.inFunctionLiteral > 0) {
            return
        }


        fun <T> handleFunction(
            name: String,
            dispatchFunction: (String, Map<String, T>) -> ((Array<Any>) -> Any)?,
            functionMap: Map<String, T>,
            adapter: (T) -> (Array<Any>) -> Any,
            errorMessage: String
        ) {
            val function = dispatchFunction(name, functionMap)

            if (function != null) {
                s.functionStack.push(function)
            } else {
                val cfunction = constants(name) as? T
                if (cfunction != null) {
                    s.functionStack.push(adapter(cfunction))
                } else {
                    s.functionStack.push(errorValue("unresolved function: '$errorMessage'") { _ ->
                        error("this is the error function")
                    })
                }
            }
        }



        val type = node.symbol.type
        if (type == KeyLangParser.Tokens.INTLIT) {
            s.valueStack.pushChecked(node.text.toDouble())
        } else if (type == KeyLangParser.Tokens.DECLIT) {
            s.valueStack.pushChecked(node.text.toDouble())
        } else if (type == KeyLangParser.Tokens.STRING_CONTENT) {
            s.valueStack.pushChecked(node.text)
        } else if (type == KeyLangParser.Tokens.ID) {
            val name = node.text.replace("`", "")
            @Suppress("DIVISION_BY_ZERO")
            when (val idType = s.idTypeStack.pop()) {
                IDType.VARIABLE -> s.valueStack.pushChecked(
                    when (name) {
                        "PI" -> PI
                        else -> constants(name)
                            ?: errorValue("unresolved value: '${name}'. Available constant: ${constants}", Unit)
                    }
                )

                IDType.PROPERTY -> s.propertyStack.push(name)

                IDType.MEMBER_FUNCTION0,
                IDType.MEMBER_FUNCTION1,
                IDType.MEMBER_FUNCTION2,
                IDType.MEMBER_FUNCTION3 -> {
                    val receiver = s.valueStack.pop()
                    when (receiver) {
                        is String -> {
                            s.functionStack.push(
                                receiver.memberFunctions(name)
                                    ?: error("no member function '$receiver.$name()'")
                            )
                        }

                        is List<*> -> {
                            s.functionStack.push(
                                receiver.memberFunctions(name)
                                    ?: error("no member function '$receiver.$name()'")
                            )
                        }

                        is ColorRGBa -> {
                            when (idType) {
                                IDType.MEMBER_FUNCTION1 -> {
                                    s.functionStack.push(
                                        when (name) {
                                            "shade" -> { x -> receiver.shade(x[0] as Double) }
                                            "opacify" -> { x -> receiver.opacify(x[0] as Double) }
                                            else -> error("no member function '$receiver.$name()'")
                                        })
                                }

                                else -> error("no member function $idType '$receiver.$name()")
                            }
                        }

                        is Function<*> -> {

                            fun input(): String {
                                return "in '${node.getParent()?.text}'"
                            }

                            @Suppress("UNCHECKED_CAST")
                            receiver as (String) -> Any?
                            val function =
                                receiver.invoke(name) ?: error("Unresolved function '${name} ${input()}")

                            when (idType) {
                                IDType.MEMBER_FUNCTION0 -> {
                                    @Suppress("UNCHECKED_CAST")
                                    function as () -> Any
                                    s.functionStack.push({ function() })
                                }

                                IDType.MEMBER_FUNCTION1 -> {
                                    @Suppress("UNCHECKED_CAST")
                                    (function as? (Any) -> Any)
                                        ?: error("Cannot cast function '$name' ($function) to (Any) -> Any ${input()}")
                                    s.functionStack.push({ x -> function(x[0]) })
                                }

                                IDType.MEMBER_FUNCTION2 -> {
                                    @Suppress("UNCHECKED_CAST")
                                    function as? (Any, Any) -> Any
                                        ?: error("Cannot cast function '$name' ($function) to (Any, Any) -> Any ${input()}")
                                    s.functionStack.push({ x -> function(x[0], x[1]) })
                                }

                                IDType.MEMBER_FUNCTION3 -> {
                                    @Suppress("UNCHECKED_CAST")
                                    function as? (Any, Any, Any) -> Any
                                        ?: error("Cannot cast function '$name' ($function) to (Any, Any, Any) -> Any ${input()}")
                                    s.functionStack.push({ x -> function(x[0], x[1], x[2]) })
                                }

                                else -> error("unreachable")
                            }
                        }
                        else -> error(
                            "receiver for '$name' '${
                                receiver.toString().take(30)
                            }' ${receiver::class} not supported"
                        )
                    }
                }

                IDType.FUNCTION0 ->  handleFunction(
                    name,
                    ::dispatchFunction0,
                    functions.functions0,
                    { f -> { x -> f() } },
                    "${name}()"
                )

                IDType.FUNCTION1 -> handleFunction(
                    name,
                    ::dispatchFunction1,
                    functions.functions1,
                    { f -> { x -> f(x[0]) } },
                    "${name}(x0)"
                )

                IDType.FUNCTION2 -> handleFunction(
                    name,
                    ::dispatchFunction2,
                    functions.functions2,
                    { f -> { x -> f(x[0], x[1]) } },
                    "${name}(x0, x1)"
                )

                IDType.FUNCTION3 -> handleFunction(
                    name,
                    ::dispatchFunction3,
                    functions.functions3,
                    { f -> { x -> f(x[0], x[1], x[2]) } },
                    "${name}(x0, x1, x2)"
                )

                IDType.FUNCTION4 -> handleFunction(
                    name,
                    ::dispatchFunction4,
                    functions.functions4,
                    { f -> { x -> f(x[0], x[1], x[2], x[3]) } },
                    "${name}(x0, x1, x2, x3)"
                )

                IDType.FUNCTION5 -> {
                    val cfunction = constants(name) as? (Any, Any, Any, Any, Any) -> Any
                    if (cfunction != null) {
                        s.functionStack.push({ x -> cfunction(x[0], x[1], x[2], x[3], x[4]) })
                    } else {
                        s.functionStack.push(errorValue("unresolved function: '${name}(x0, x1, x2, x3, x4)'") { _ ->
                            error("this is the error function")
                        })
                    }
                }

                IDType.FUNCTION_ARGUMENT -> {

                }

                else -> error("unsupported id-type $idType")
            }
        }
    }
}

expect class TypedExpressionListener(
    functions: TypedFunctionExtensions = TypedFunctionExtensions.EMPTY,
    constants: (String) -> Any? = { null }
) : TypedExpressionListenerBase {

    override val state: State
}

class ExpressionException(message: String) : RuntimeException(message)

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
): Any? {
    val lexer = KeyLangLexer(CharStreams.fromString(expression))
    val parser = KeyLangParser(CommonTokenStream(lexer))
    parser.removeErrorListeners()
    parser.addErrorListener(object : BaseErrorListener() {
        override fun syntaxError(
            recognizer: Recognizer<*, *>,
            offendingSymbol: Any?,
            line: Int,
            charPositionInLine: Int,
            msg: String,
            e: RecognitionException?
        ) {
            throw ExpressionException("parser error in expression: '$expression'; [line: $line, character: $charPositionInLine ${offendingSymbol?.let { ", near: $it" } ?: ""} ]")
        }
    })

    val root = parser.keyLangFile()
    val listener = TypedExpressionListener(functions, constants)
    try {
        ParseTreeWalker.DEFAULT.walk(listener, root)
    } catch (e: ExpressionException) {
        throw ExpressionException(e.message ?: "")
    }

    return listener.state.lastExpressionResult
}

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
    val lexer = KeyLangLexer(CharStreams.fromString(expression))
    val parser = KeyLangParser(CommonTokenStream(lexer))
    parser.removeErrorListeners()
    parser.addErrorListener(object : BaseErrorListener() {
        override fun syntaxError(
            recognizer: Recognizer<*, *>,
            offendingSymbol: Any?,
            line: Int,
            charPositionInLine: Int,
            msg: String,
            e: RecognitionException?
        ) {
            throw ExpressionException("parser error in expression: '$expression'; [line: $line, character: $charPositionInLine ${offendingSymbol?.let { ", near: $it" } ?: ""} ]")
        }
    })
    val root = parser.keyLangFile()
    val listener = TypedExpressionListener(functions, constants)

    return {
        try {
            ParseTreeWalker.DEFAULT.walk(listener, root)
        } catch (e: ExpressionException) {
            throw ExpressionException(e.message ?: "")
        }
        listener.state.lastExpressionResult ?: error("no result")
    }
}

internal fun expressionRoot(expression: String): KeyLangParser.KeyLangFileContext {
    val lexer = KeyLangLexer(CharStreams.fromString(expression))
    val parser = KeyLangParser(CommonTokenStream(lexer))
    parser.removeErrorListeners()
    parser.addErrorListener(object : BaseErrorListener() {
        override fun syntaxError(
            recognizer: Recognizer<*, *>,
            offendingSymbol: Any?,
            line: Int,
            charPositionInLine: Int,
            msg: String,
            e: RecognitionException?
        ) {
            throw ExpressionException("parser error in expression: '$expression'; [line: $line, character: $charPositionInLine ${offendingSymbol?.let { ", near: $it" } ?: ""} ]")
        }
    })
    return parser.keyLangFile()
}