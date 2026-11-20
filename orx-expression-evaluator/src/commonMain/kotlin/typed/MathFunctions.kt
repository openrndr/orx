package org.openrndr.extra.expressions.typed

import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import org.openrndr.math.smoothstep
import kotlin.math.floor
import kotlin.math.pow

/**
 * [f] of numbers, or of the components of vectors one by one; a number given with vectors stands for each of their
 * components, so that `clamp(v, 0.0, 1.0)` clamps every component of v
 */
private fun componentwise(name: String, args: Array<Any>, f: (DoubleArray) -> Double): Any {
    val vector = args.firstOrNull { it !is Double } ?: return f(DoubleArray(args.size) { args[it] as Double })
    val size = when (vector) {
        is Vector2 -> 2
        is Vector3 -> 3
        is Vector4 -> 4
        else -> error("$name(): unsupported argument ${typeName(vector)}")
    }
    for (a in args) {
        require(a is Double || a::class == vector::class) {
            "$name(): unsupported arguments ${args.joinToString { typeName(it) }}"
        }
    }
    fun component(a: Any, i: Int): Double = when (a) {
        is Double -> a
        is Vector2 -> if (i == 0) a.x else a.y
        is Vector3 -> when (i) { 0 -> a.x; 1 -> a.y; else -> a.z }
        else -> (a as Vector4).let { when (i) { 0 -> it.x; 1 -> it.y; 2 -> it.z; else -> it.w } }
    }
    val c = DoubleArray(size) { i -> f(DoubleArray(args.size) { component(args[it], i) }) }
    return when (size) {
        2 -> Vector2(c[0], c[1])
        3 -> Vector3(c[0], c[1], c[2])
        else -> Vector4(c[0], c[1], c[2], c[3])
    }
}

/** a function of numbers, called without boxing them */
internal fun interface DoubleFunction1 {
    fun call(x: Double): Double
}

/** a function of two numbers, called without boxing them */
internal fun interface DoubleFunction2 {
    fun call(x: Double, y: Double): Double
}

/** a function of three numbers, called without boxing them */
internal fun interface DoubleFunction3 {
    fun call(x: Double, y: Double, z: Double): Double
}

/** the math functions of one argument, of numbers */
internal fun mathDouble1(name: String): DoubleFunction1? = when (name) {
    "tan" -> DoubleFunction1 { x -> kotlin.math.tan(x) }
    "atan" -> DoubleFunction1 { x -> kotlin.math.atan(x) }
    "acos" -> DoubleFunction1 { x -> kotlin.math.acos(x) }
    "asin" -> DoubleFunction1 { x -> kotlin.math.asin(x) }
    "exp" -> DoubleFunction1 { x -> kotlin.math.exp(x) }
    "log" -> DoubleFunction1 { x -> kotlin.math.ln(x) }
    "floor" -> DoubleFunction1 { x -> kotlin.math.floor(x) }
    "ceil" -> DoubleFunction1 { x -> kotlin.math.ceil(x) }
    "fract" -> DoubleFunction1 { x -> x - floor(x) }
    "sign" -> DoubleFunction1 { x -> kotlin.math.sign(x) }
    "saturate" -> DoubleFunction1 { x -> x.coerceIn(0.0, 1.0) }
    "radians" -> DoubleFunction1 { x -> x * kotlin.math.PI / 180.0 }
    "degrees" -> DoubleFunction1 { x -> x * 180.0 / kotlin.math.PI }
    else -> null
}

/** the math functions of one argument, of numbers and of the components of vectors */
internal fun mathFunction1(name: String): ((Array<Any>) -> Any)? {
    val f = mathDouble1(name) ?: return null
    return { x -> componentwise(name, x) { f.call(it[0]) } }
}

/**
 * the math functions of two arguments: pow(x, y), mod(x, y) and atan2(y, x); `step` is a word of the grammar, for
 * ranges, `x >= edge` gives what step(edge, x) would
 */
internal fun mathFunction2(name: String): ((Array<Any>) -> Any)? {
    val f = mathDouble2(name) ?: return null
    return { x -> componentwise(name, x) { f.call(it[0], it[1]) } }
}

/** the math functions of two arguments, of numbers */
internal fun mathDouble2(name: String): DoubleFunction2? = when (name) {
    "pow" -> DoubleFunction2 { x, y -> x.pow(y) }
    "mod" -> DoubleFunction2 { x, y -> x.mod(y) }
    "atan2" -> DoubleFunction2 { y, x -> kotlin.math.atan2(y, x) }
    else -> null
}

/** the math functions of three arguments: clamp(x, min, max) and smoothstep(edge0, edge1, x) */
internal fun mathFunction3(name: String): ((Array<Any>) -> Any)? {
    val f = mathDouble3(name) ?: return null
    return { x -> componentwise(name, x) { f.call(it[0], it[1], it[2]) } }
}

/** the math functions of three arguments, of numbers */
internal fun mathDouble3(name: String): DoubleFunction3? = when (name) {
    "clamp" -> DoubleFunction3 { x, lo, hi -> x.coerceIn(lo, hi) }
    "smoothstep" -> DoubleFunction3 { e0, e1, x -> smoothstep(e0, e1, x) }
    else -> null
}

/**
 * what the built-in function [name] of [n] numbers gives, the same as through [dispatchFunction1],
 * [dispatchFunction2] or [dispatchFunction3] when its arguments are numbers, without boxing them
 */
internal fun doubleFunction(name: String, n: Int): Any? = when (n) {
    1 -> when (name) {
        "cos" -> DoubleFunction1 { kotlin.math.cos(it) }
        "sin" -> DoubleFunction1 { kotlin.math.sin(it) }
        "sqrt" -> DoubleFunction1 { kotlin.math.sqrt(it) }
        "abs" -> DoubleFunction1 { kotlin.math.abs(it) }
        "round" -> DoubleFunction1 { roundHalfAway(it) }
        else -> mathDouble1(name)
    }
    2 -> when (name) {
        "min" -> DoubleFunction2 { x, y -> kotlin.math.min(x, y) }
        "max" -> DoubleFunction2 { x, y -> kotlin.math.max(x, y) }
        else -> mathDouble2(name)
    }
    3 -> when (name) {
        "mix" -> DoubleFunction3 { x, y, f -> org.openrndr.math.mix(x, y, f) }
        else -> mathDouble3(name)
    }
    else -> null
}
