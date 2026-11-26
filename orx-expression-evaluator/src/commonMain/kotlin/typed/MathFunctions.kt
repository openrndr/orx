package org.openrndr.extra.expressions.typed

import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import org.openrndr.math.smoothstep
import org.openrndr.extra.noise.uniform
import kotlin.math.floor
import kotlin.math.pow

/** a function of the numbers of an array, called without boxing what it gives, see [componentwise] */
internal fun interface DoublesFunction {
    fun call(x: DoubleArray): Double
}

/**
 * [f] of numbers, or of the components of vectors one by one; a number given with vectors stands for each of their
 * components, so that `clamp(v, 0.0, 1.0)` clamps every component of v
 */
private fun componentwise(name: String, args: Array<Any>, f: DoublesFunction): Any {
    val vector = args.firstOrNull { it !is Double } ?: return f.call(DoubleArray(args.size) { args[it] as Double })
    val size = when (vector) {
        is Vector2 -> 2
        is Vector3 -> 3
        is Vector4 -> 4
        else -> error("$name(): unsupported argument ${typeName(vector)}")
    }
    for (a in args) {
        val same = when (size) {
            2 -> a is Vector2
            3 -> a is Vector3
            else -> a is Vector4
        }
        require(a is Double || same) {
            "$name(): unsupported arguments ${args.joinToString { typeName(it) }}"
        }
    }
    fun component(a: Any, i: Int): Double = when (a) {
        is Double -> a
        is Vector2 -> if (i == 0) a.x else a.y
        is Vector3 -> when (i) { 0 -> a.x; 1 -> a.y; else -> a.z }
        else -> (a as Vector4).let { when (i) { 0 -> it.x; 1 -> it.y; 2 -> it.z; else -> it.w } }
    }
    val c = DoubleArray(size) { i -> f.call(DoubleArray(args.size) { component(args[it], i) }) }
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

/** a function of four numbers, called without boxing them */
internal fun interface DoubleFunction4 {
    fun call(x: Double, y: Double, z: Double, w: Double): Double
}

/** a function of five numbers, called without boxing them */
internal fun interface DoubleFunction5 {
    fun call(a: Double, b: Double, c: Double, d: Double, e: Double): Double
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

/*
 * The functions of the untyped evaluator that take more numbers. They give exactly what it gives: min and max from the
 * first number on, so that NaN and -0.0 come out the same, sum from 0.0 on, and length like the length of a vector.
 * With all their arguments numbers, they are called through [doubleFunction] without boxing them; these are for
 * vectors, component by component, and for the errors of what is neither.
 */

/** `min` of 2 to 5 numbers, or of the components of vectors */
internal fun smallest(x: Array<Any>): Any = componentwise("min", x) { d ->
    var m = d[0]
    for (i in 1 until d.size) m = kotlin.math.min(m, d[i])
    m
}

/** `max` of 2 to 5 numbers, or of the components of vectors */
internal fun largest(x: Array<Any>): Any = componentwise("max", x) { d ->
    var m = d[0]
    for (i in 1 until d.size) m = kotlin.math.max(m, d[i])
    m
}

/** `sum` of 3 to 5 numbers, or of the components of vectors */
internal fun total(x: Array<Any>): Any = componentwise("sum", x) { d ->
    var s = 0.0
    for (v in d) s += v
    s
}

/** `random(a, b)`, a number in [a, b), or a vector of which every component is random on its own */
internal fun randomBetween(x: Array<Any>): Any = componentwise("random", x) { d -> Double.uniform(d[0], d[1]) }

/** `map(x0, x1, y0, y1, v)`, of numbers or of the components of vectors */
internal fun mapRange(x: Array<Any>): Any = componentwise("map", x) { d -> org.openrndr.math.map(d[0], d[1], d[2], d[3], d[4]) }

/** `length` of 2 to 4 numbers, the length of the vector of them, computed as [Vector2.length] and the others do */
internal fun lengthOf(x: Array<Any>): Any {
    val d = DoubleArray(x.size) { i ->
        x[i] as? Double ?: error("length(): takes numbers, not ${typeName(x[i])}; the length of a vector is v.length")
    }
    return when (d.size) {
        2 -> lengthOf(d[0], d[1])
        3 -> lengthOf(d[0], d[1], d[2])
        else -> lengthOf(d[0], d[1], d[2], d[3])
    }
}

private fun lengthOf(x: Double, y: Double) = kotlin.math.sqrt(x * x + y * y)
private fun lengthOf(x: Double, y: Double, z: Double) = kotlin.math.sqrt(x * x + y * y + z * z)
private fun lengthOf(x: Double, y: Double, z: Double, w: Double) = kotlin.math.sqrt(x * x + y * y + z * z + w * w)

/**
 * what the built-in function [name] of [n] numbers gives, the same as through [dispatchFunction1] to
 * [dispatchFunction5] when its arguments are numbers, without boxing them
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
        "random" -> DoubleFunction2 { a, b -> Double.uniform(a, b) }
        "length" -> DoubleFunction2 { x, y -> lengthOf(x, y) }
        else -> mathDouble2(name)
    }
    3 -> when (name) {
        "mix" -> DoubleFunction3 { x, y, f -> org.openrndr.math.mix(x, y, f) }
        "min" -> DoubleFunction3 { x, y, z -> kotlin.math.min(kotlin.math.min(x, y), z) }
        "max" -> DoubleFunction3 { x, y, z -> kotlin.math.max(kotlin.math.max(x, y), z) }
        "sum" -> DoubleFunction3 { x, y, z -> 0.0 + x + y + z }
        "length" -> DoubleFunction3 { x, y, z -> lengthOf(x, y, z) }
        else -> mathDouble3(name)
    }
    4 -> when (name) {
        "min" -> DoubleFunction4 { x, y, z, w -> kotlin.math.min(kotlin.math.min(kotlin.math.min(x, y), z), w) }
        "max" -> DoubleFunction4 { x, y, z, w -> kotlin.math.max(kotlin.math.max(kotlin.math.max(x, y), z), w) }
        "sum" -> DoubleFunction4 { x, y, z, w -> 0.0 + x + y + z + w }
        "length" -> DoubleFunction4 { x, y, z, w -> lengthOf(x, y, z, w) }
        else -> null
    }
    5 -> when (name) {
        "min" -> DoubleFunction5 { a, b, c, d, e -> kotlin.math.min(kotlin.math.min(kotlin.math.min(kotlin.math.min(a, b), c), d), e) }
        "max" -> DoubleFunction5 { a, b, c, d, e -> kotlin.math.max(kotlin.math.max(kotlin.math.max(kotlin.math.max(a, b), c), d), e) }
        "sum" -> DoubleFunction5 { a, b, c, d, e -> 0.0 + a + b + c + d + e }
        "map" -> DoubleFunction5 { a, b, c, d, e -> org.openrndr.math.map(a, b, c, d, e) }
        else -> null
    }
    else -> null
}
