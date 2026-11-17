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
        else -> error("$name(): unsupported argument ${vector::class.simpleName}")
    }
    for (a in args) {
        require(a is Double || a::class == vector::class) {
            "$name(): unsupported arguments ${args.joinToString { it::class.simpleName ?: "value" }}"
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

/** the math functions of one argument, of numbers and of the components of vectors */
internal fun mathFunction1(name: String): ((Array<Any>) -> Any)? {
    val f: (Double) -> Double = when (name) {
        "tan" -> { x -> kotlin.math.tan(x) }
        "atan" -> { x -> kotlin.math.atan(x) }
        "acos" -> { x -> kotlin.math.acos(x) }
        "asin" -> { x -> kotlin.math.asin(x) }
        "exp" -> { x -> kotlin.math.exp(x) }
        "log" -> { x -> kotlin.math.ln(x) }
        "floor" -> { x -> kotlin.math.floor(x) }
        "ceil" -> { x -> kotlin.math.ceil(x) }
        "fract" -> { x -> x - floor(x) }
        "sign" -> { x -> kotlin.math.sign(x) }
        "saturate" -> { x -> x.coerceIn(0.0, 1.0) }
        "radians" -> { x -> x * kotlin.math.PI / 180.0 }
        "degrees" -> { x -> x * 180.0 / kotlin.math.PI }
        else -> return null
    }
    return { x -> componentwise(name, x) { f(it[0]) } }
}

/**
 * the math functions of two arguments: pow(x, y), mod(x, y) and atan2(y, x); `step` is a word of the grammar, for
 * ranges, `x >= edge` gives what step(edge, x) would
 */
internal fun mathFunction2(name: String): ((Array<Any>) -> Any)? {
    val f: (Double, Double) -> Double = when (name) {
        "pow" -> { x, y -> x.pow(y) }
        "mod" -> { x, y -> x.mod(y) }
        "atan2" -> { y, x -> kotlin.math.atan2(y, x) }
        else -> return null
    }
    return { x -> componentwise(name, x) { f(it[0], it[1]) } }
}

/** the math functions of three arguments: clamp(x, min, max) and smoothstep(edge0, edge1, x) */
internal fun mathFunction3(name: String): ((Array<Any>) -> Any)? {
    val f: (Double, Double, Double) -> Double = when (name) {
        "clamp" -> { x, lo, hi -> x.coerceIn(lo, hi) }
        "smoothstep" -> { e0, e1, x -> smoothstep(e0, e1, x) }
        else -> return null
    }
    return { x -> componentwise(name, x) { f(it[0], it[1], it[2]) } }
}
