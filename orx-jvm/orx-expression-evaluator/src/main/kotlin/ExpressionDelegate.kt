package org.openrndr.extra.expressions

import org.openrndr.extra.propertywatchers.watchingProperty
import kotlin.reflect.KProperty0

fun watchingExpression1(
    expressionProperty: KProperty0<String>,
    parameter0: String = "x",
    constants: Map<String, Double> = emptyMap(),
    functions: FunctionExtensions = FunctionExtensions.EMPTY,
    error: (p0: Double) -> Double = { 0.0 }
) =
    watchingProperty(expressionProperty) {
        compileFunction1(it, parameter0, constants, functions, error)
    }

fun watchingExpression2(
    expressionProperty: KProperty0<String>,
    parameter0: String = "x",
    parameter1: String = "y",
    constants: Map<String, Double> = emptyMap(),
    functions: FunctionExtensions = FunctionExtensions.EMPTY,
    error: (p0: Double, p1: Double) -> Double = { _, _ -> 0.0 }
) =
    watchingProperty(expressionProperty) {
        compileFunction2(it, parameter0, parameter1, constants, functions, error)
    }

fun watchingExpression3(
    expressionProperty: KProperty0<String>,
    parameter0: String = "x",
    parameter1: String = "y",
    parameter2: String = "z",
    constants: Map<String, Double> = emptyMap(),
    functions: FunctionExtensions = FunctionExtensions.EMPTY,
    error: (p0: Double, p1: Double, p2: Double) -> Double = { _, _, _ -> 0.0 }
) =
    watchingProperty(expressionProperty) {
        compileFunction3(it, parameter0, parameter1, parameter2, constants, functions, error)
    }