package org.openrndr.extra.keyframer

import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.tree.ParseTreeWalker
import org.antlr.v4.runtime.tree.TerminalNode
import org.openrndr.extra.keyframer.antlr.*
import org.openrndr.extra.noise.uniform
import org.openrndr.math.map
import org.openrndr.math.mix
import org.openrndr.math.mod
import org.openrndr.math.smoothstep
import java.util.*
import kotlin.math.*

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

internal enum class IDType {
    VARIABLE,
    FUNCTION0,
    FUNCTION1,
    FUNCTION2,
    FUNCTION3,
    FUNCTION4,
    FUNCTION5
}

internal class ExpressionListener(val functions: FunctionExtensions = FunctionExtensions.EMPTY) :
    KeyLangParserBaseListener() {
    val doubleStack = Stack<Double>()
    val functionStack = Stack<(DoubleArray) -> Double>()
    val variables = mutableMapOf<String, Double>()

    val idTypeStack = Stack<IDType>()
    var lastExpressionResult: Double? = null

    val exceptionStack = Stack<ExpressionException>()


    override fun exitExpressionStatement(ctx: KeyLangParser.ExpressionStatementContext) {
        ifError {
            throw ExpressionException("error in evaluation of '${ctx.text}': ${it.message ?: ""}")
        }
        val result = doubleStack.pop()
        lastExpressionResult = result
    }

    override fun exitAssignment(ctx: KeyLangParser.AssignmentContext) {
        val value = doubleStack.pop()
        variables[ctx.ID()?.text ?: error("buh")] = value
    }

    override fun exitMinusExpression(ctx: KeyLangParser.MinusExpressionContext) {
        super.exitMinusExpression(ctx)
    }

    override fun exitBinaryOperation1(ctx: KeyLangParser.BinaryOperation1Context) {
        ifError {
            pushError(it.message ?: "")
            return
        }

        val right = doubleStack.pop()
        val left = doubleStack.pop()
        val result = when (val operator = ctx.operator?.type) {
            KeyLangParser.PLUS -> left + right
            KeyLangParser.MINUS -> left - right
            KeyLangParser.ASTERISK -> left * right
            KeyLangParser.DIVISION -> left / right
            KeyLangParser.PERCENTAGE -> mod(left, right)
            else -> error("operator '$operator' not implemented")
        }
        doubleStack.push(result)
    }

    override fun exitBinaryOperation2(ctx: KeyLangParser.BinaryOperation2Context) {
        ifError {
            pushError(it.message ?: "")
            return
        }

        val left = doubleStack.pop()
        val right = doubleStack.pop()
        val result = when (val operator = ctx.operator?.type) {
            KeyLangParser.PLUS -> left + right
            KeyLangParser.MINUS -> right - left
            KeyLangParser.ASTERISK -> left * right
            KeyLangParser.DIVISION -> left / right
            else -> error("operator '$operator' not implemented")
        }
        doubleStack.push(result)
    }

    override fun enterValueReference(ctx: KeyLangParser.ValueReferenceContext) {
        idTypeStack.push(IDType.VARIABLE)
    }

    override fun enterFunctionCall0Expression(ctx: KeyLangParser.FunctionCall0ExpressionContext) {
        idTypeStack.push(IDType.FUNCTION0)
    }

    override fun exitFunctionCall0Expression(ctx: KeyLangParser.FunctionCall0ExpressionContext) {
        ifError {
            pushError(it.message ?: "")
            return
        }

        val function = functionStack.pop()
        val result = function.invoke(doubleArrayOf())
        doubleStack.push(result)
    }

    override fun enterFunctionCall1Expression(ctx: KeyLangParser.FunctionCall1ExpressionContext) {
        idTypeStack.push(IDType.FUNCTION1)
    }

    override fun exitFunctionCall1Expression(ctx: KeyLangParser.FunctionCall1ExpressionContext) {
        ifError {
            pushError(it.message ?: "")
            return
        }

        val function = functionStack.pop()
        val argument = doubleStack.pop()

        val result = function.invoke(doubleArrayOf(argument))
        doubleStack.push(result)
    }

    override fun enterFunctionCall2Expression(ctx: KeyLangParser.FunctionCall2ExpressionContext) {
        idTypeStack.push(IDType.FUNCTION2)
    }

    override fun exitFunctionCall2Expression(ctx: KeyLangParser.FunctionCall2ExpressionContext) {
        ifError {
            pushError(it.message ?: "")
            return
        }

        val function = functionStack.pop()
        val argument1 = doubleStack.pop()
        val argument0 = doubleStack.pop()

        val result = function.invoke(doubleArrayOf(argument0, argument1))
        doubleStack.push(result)
    }

    override fun enterFunctionCall3Expression(ctx: KeyLangParser.FunctionCall3ExpressionContext) {
        idTypeStack.push(IDType.FUNCTION3)
    }

    override fun exitFunctionCall3Expression(ctx: KeyLangParser.FunctionCall3ExpressionContext) {
        ifError {
            pushError(it.message ?: "")
            return
        }

        val function = functionStack.pop()
        val argument2 = doubleStack.pop()
        val argument1 = doubleStack.pop()
        val argument0 = doubleStack.pop()

        val result = function.invoke(doubleArrayOf(argument0, argument1, argument2))
        doubleStack.push(result)
    }

    override fun enterFunctionCall4Expression(ctx: KeyLangParser.FunctionCall4ExpressionContext) {
        idTypeStack.push(IDType.FUNCTION4)
    }

    override fun exitFunctionCall4Expression(ctx: KeyLangParser.FunctionCall4ExpressionContext) {
        ifError {
            pushError(it.message ?: "")
            return
        }

        val function = functionStack.pop()
        val argument3 = doubleStack.pop()
        val argument2 = doubleStack.pop()
        val argument1 = doubleStack.pop()
        val argument0 = doubleStack.pop()

        val result = function.invoke(doubleArrayOf(argument0, argument1, argument2, argument3))
        doubleStack.push(result)
    }


    override fun enterFunctionCall5Expression(ctx: KeyLangParser.FunctionCall5ExpressionContext) {
        idTypeStack.push(IDType.FUNCTION5)
    }

    override fun exitFunctionCall5Expression(ctx: KeyLangParser.FunctionCall5ExpressionContext) {
        ifError {
            pushError(it.message ?: "")
            return
        }

        val function = functionStack.pop()
        val argument4 = doubleStack.pop()
        val argument3 = doubleStack.pop()
        val argument2 = doubleStack.pop()
        val argument1 = doubleStack.pop()
        val argument0 = doubleStack.pop()

        val result = function.invoke(doubleArrayOf(argument0, argument1, argument2, argument3, argument4))
        doubleStack.push(result)
    }

    private fun <T> errorValue(message: String, value: T): T {
        pushError(message)
        return value
    }

    private fun pushError(message: String) {
        exceptionStack.push(ExpressionException(message))
    }

    private inline fun ifError(f: (e: Throwable) -> Unit) {
        if (exceptionStack.isNotEmpty()) {
            val e = exceptionStack.pop()
            f(e)
        }
    }

    override fun visitTerminal(node: TerminalNode) {
        val type = node.symbol?.type
        if (type == KeyLangParser.INTLIT) {
            doubleStack.push(node.text.toDouble())
        }
        if (type == KeyLangParser.DECLIT) {
            doubleStack.push(node.text.toDouble())
        }
        if (type == KeyLangParser.ID) {

            @Suppress("DIVISION_BY_ZERO")
            when (val idType = idTypeStack.pop()) {
                IDType.VARIABLE -> doubleStack.push(
                    when (val name = node.text) {
                        "PI" -> PI
                        else -> variables[name] ?: errorValue("unresolved variable: '${name}'", 0.0 / 0.0)
                    }
                )

                IDType.FUNCTION0 -> {
                    val function: (DoubleArray) -> Double =
                        when (val candidate = node.text) {
                            "random" -> { _ -> Double.uniform(0.0, 1.0) }
                            else -> functions.functions0[candidate]?.let { { _:DoubleArray -> it.invoke() } }
                                ?: errorValue(
                                    "unresolved function: '${candidate}()'"
                                ) { _ -> error("this is the error function") }
                        }
                    functionStack.push(function)
                }

                IDType.FUNCTION1 -> {
                    val function: (DoubleArray) -> Double =
                        when (val candidate = node.text) {
                            "sqrt" -> { x -> sqrt(x[0]) }
                            "radians" -> { x -> Math.toRadians(x[0]) }
                            "degrees" -> { x -> Math.toDegrees(x[0]) }
                            "cos" -> { x -> cos(x[0]) }
                            "sin" -> { x -> sin(x[0]) }
                            "tan" -> { x -> tan(x[0]) }
                            "atan" -> { x -> atan(x[0]) }
                            "acos" -> { x -> acos(x[0]) }
                            "asin" -> { x -> asin(x[0]) }
                            "exp" -> { x -> exp(x[0]) }
                            "abs" -> { x -> abs(x[0]) }
                            "floor" -> { x -> floor(x[0]) }
                            "ceil" -> { x -> ceil(x[0]) }
                            "saturate" -> { x -> x[0].coerceIn(0.0, 1.0) }
                            else -> functions.functions1[candidate]?.let { { x:DoubleArray -> it.invoke(x[0]) } }
                                ?: errorValue(
                                    "unresolved function: '${candidate}(x0)'"
                                ) { _ -> error("this is the error function") }
                        }
                    functionStack.push(function)
                }
                IDType.FUNCTION2 -> {
                    val function: (DoubleArray) -> Double =
                        when (val candidate = node.text) {
                            "max" -> { x -> max(x[0], x[1]) }
                            "min" -> { x -> min(x[0], x[1]) }
                            "pow" -> { x -> x[0].pow(x[1]) }
                            "atan2" -> { x -> atan2(x[0], x[1]) }
                            "random" -> { x -> Double.uniform(x[0], x[1]) }
                            else -> functions.functions2[candidate]?.let { { x:DoubleArray -> it.invoke(x[0], x[1]) } }
                                ?: errorValue(
                                    "unresolved function: '${candidate}(x0, x1)'"
                                ) { _ -> error("this is the error function") }
                        }
                    functionStack.push(function)
                }
                IDType.FUNCTION3 -> {
                    val function: (DoubleArray) -> Double =
                        when (val candidate = node.text) {
                            "mix" -> { x -> mix(x[0], x[1], x[2]) }
                            "smoothstep" -> { x -> smoothstep(x[0], x[1], x[2]) }
                            else -> functions.functions3[candidate]?.let { { x:DoubleArray -> it.invoke(x[0], x[1], x[2]) } }
                                ?: errorValue(
                                    "unresolved function: '${candidate}(x0, x1, x2)'"
                                ) { _ -> error("this is the error function") }
                        }
                    functionStack.push(function)
                }
                IDType.FUNCTION4 -> {
                    val function: (DoubleArray) -> Double =
                        when (val candidate = node.text) {
                            else -> functions.functions4[candidate]?.let { { x:DoubleArray -> it.invoke(x[0], x[1], x[2], x[3]) } }
                                ?: errorValue(
                                    "unresolved function: '${candidate}(x0, x1, x2, x3)'"
                                ) { _ -> error("this is the error function") }
                        }
                    functionStack.push(function)
                }

                IDType.FUNCTION5 -> {
                    val function: (DoubleArray) -> Double =
                        when (val candidate = node.text) {
                            "map" -> { x -> map(x[0], x[1], x[2], x[3], x[4]) }
                            else -> functions.functions5[candidate]?.let { { x:DoubleArray -> it.invoke(x[0], x[1], x[2], x[3], x[4]) } }
                                ?: errorValue(
                                    "unresolved function: '${candidate}(x0, x1, x2, x3, x4)'"
                                ) { _ -> error("this is the error function") }
                        }
                    functionStack.push(function)
                }
                else -> error("unsupported id-type $idType")
            }
        }
    }
}

class ExpressionException(message: String) : RuntimeException(message)

fun evaluateExpression(
    input: String,
    variables: Map<String, Double> = emptyMap(),
    functions: FunctionExtensions = FunctionExtensions.EMPTY
): Double? {
    val lexer = KeyLangLexer(CharStreams.fromString(input))
//
//    lexer.removeErrorListeners()
//    lexer.addErrorListener(object : BaseErrorListener() {
//        override fun syntaxError(
//            recognizer: Recognizer<*, *>?,
//            offendingSymbol: Any?,
//            line: Int,
//            charPositionInLine: Int,
//            msg: String?,
//            e: RecognitionException?
//        ) {
//            println("syntax error!")
//        }
//    })
    val parser = KeyLangParser(CommonTokenStream(lexer))
    parser.removeErrorListeners()
    parser.addErrorListener(object : BaseErrorListener() {
        override fun syntaxError(
            recognizer: Recognizer<*, *>?,
            offendingSymbol: Any?,
            line: Int,
            charPositionInLine: Int,
            msg: String?,
            e: RecognitionException?
        ) {
            throw ExpressionException("parser error in expression: '$input'; [line: $line, character: $charPositionInLine ${offendingSymbol?.let { ", near: $it" } ?: ""} ]")
        }
    })

    val root = parser.miniCalcFile()
    val listener = ExpressionListener(functions)
    listener.variables.putAll(variables)
    try {
        ParseTreeWalker.DEFAULT.walk(listener, root)
    } catch (e: ExpressionException) {
        throw ExpressionException(e.message ?: "")
    }
    return listener.lastExpressionResult
}