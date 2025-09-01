package org.openrndr.extra.expressions.typed

import org.openrndr.color.ColorRGBa
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import org.openrndr.math.transforms.scale
import org.openrndr.math.transforms.translate
import kotlin.math.abs as abs_
import kotlin.math.cos as cos_
import kotlin.math.sin as sin_
import kotlin.math.sqrt as sqrt_

internal fun vec2(x: Any): Vector2 {
    return when (x) {
        is Double -> Vector2(x, x)
        is List<*> -> {
            when (x.size) {
                2 -> {
                    @Suppress("UNCHECKED_CAST")
                    x as List<Double>; Vector2(x[0], x[1])
                }

                else -> error("vec2(): unsupported argument: '$x'")
            }
        }

        else -> error("vec2(): unsupported argument: '$x'")
    }
}

internal fun vec3(x: Any): Vector3 {
    return when (x) {
        is Double -> Vector3(x, x, x)
        is List<*> -> {
            when (x.size) {
                2 -> {
                    vec3(x[0]!!, x[1]!!)
                }

                3 -> {
                    vec3(x[0]!!, x[1]!!, x[2]!!)
                }

                else -> error("vec3(): unsupported argument: '$x'")
            }
        }

        else -> error("vec3(): unsupported argument: '$x'")
    }
}


internal fun vec4(x: Any): Vector4 {
    return when (x) {
        is Double -> Vector4(x, x, x, x)
        is List<*> -> {
            when (x.size) {
                2 -> {
                    vec4(x[0]!!, x[1]!!)
                }

                3 -> {
                    vec4(x[0]!!, x[1]!!, x[2]!!)
                }

                4 -> {
                    vec4(x[0]!!, x[1]!!, x[2]!!, x[3]!!)
                }

                else -> error("vec4(): unsupported argument: '$x'")
            }
        }

        else -> error("vec4(): unsupported argument: '$x'")
    }
}

internal fun rgba(x: Any): ColorRGBa {
    return when (x) {
        is Double -> ColorRGBa(x, x, x, 1.0)
        is Vector3 -> ColorRGBa(x.x, x.y, x.z, 1.0)
        is Vector4 -> ColorRGBa(x.x, x.y, x.z, x.w)
        else -> error("type not supported ${x::class.simpleName}")
    }
}

internal fun cos(x: Any): Any {
    return when (x) {
        is Double -> cos_(x)
        is Vector2 -> x.map { cos_(it) }
        is Vector3 -> x.map { cos_(it) }
        is Vector4 -> x.map { cos_(it) }
        else -> error("type not supported ${x::class.simpleName}")
    }
}

internal fun sin(x: Any): Any {
    return when (x) {
        is Double -> sin_(x)
        is Vector2 -> x.map { sin_(it) }
        is Vector3 -> x.map { sin_(it) }
        is Vector4 -> x.map { sin_(it) }
        else -> error("type not supported ${x::class.simpleName}")
    }
}

internal fun normalize(x: Any): Any {
    return when (x) {
        is Vector2 -> x.normalized
        is Vector3 -> x.normalized
        is Vector4 -> x.normalized
        else -> error("type not supported ${x::class.simpleName}")
    }
}

internal fun inverse(x: Any): Any {
    return when (x) {
        is Matrix44 -> x.inversed
        else -> error("type not supported ${x::class.simpleName}")
    }
}

internal fun transpose(x: Any): Any {
    return when (x) {
        is Matrix44 -> x.transposed
        else -> error("type not supported ${x::class.simpleName}")
    }
}


fun abs(x: Any): Any {
    return when (x) {
        is Double -> abs_(x)
        is Vector2 -> x.map { abs_(it) }
        is Vector3 -> x.map { abs_(it) }
        is Vector4 -> x.map { abs_(it) }
        else -> error("type not supported ${x::class.simpleName}")
    }
}

internal fun scale(scale: Any): Matrix44 {
    @Suppress("NAME_SHADOWING") val scale = when (scale) {
        is Double -> Vector3(scale, scale, scale)
        is Vector2 -> scale.xy1
        is Vector3 -> scale
        else -> error("unsupported axis argument")
    }
    return Matrix44.scale(scale)
}


internal fun sqrt(x: Any): Any {
    return when (x) {
        is Double -> sqrt_(x)
        is Vector2 -> x.map { sqrt_(it) }
        is Vector3 -> x.map { sqrt_(it) }
        is Vector4 -> x.map { sqrt_(it) }
        else -> error("type not supported ${x::class.simpleName}")
    }
}

internal fun translate(translation: Any): Matrix44 {
    @Suppress("NAME_SHADOWING") val translation = when (translation) {
        is Vector2 -> translation.xy0
        is Vector3 -> translation
        else -> error("unsupported axis argument")
    }
    return Matrix44.translate(translation)
}

internal fun mat4(x: Any): Matrix44 {
    return when (x) {
        is List<*> -> {
            @Suppress("UNCHECKED_CAST")
            when (x.size) {
                16 -> Matrix44.fromDoubleArray((x as List<Double>).toDoubleArray())
                4 -> {
                    (x as List<Vector4>)
                    Matrix44.fromColumnVectors(x[0], x[1], x[2], x[3])
                }

                else -> error("mat4(): unsupported argument: '$x'")
            }
        }

        else -> error("mat4(): unsupported argument: '$x'")
    }
}

internal fun dispatchFunction1(name: String, functions: Map<String, TypedFunction1>): ((Array<Any>) -> Any)? {
    return when (name) {
        "vec2" -> { x -> vec2(x[0]) }
        "vec3" -> { x -> vec3(x[0]) }
        "vec4" -> { x -> vec4(x[0]) }
        "mat4" -> { x -> mat4(x[0]) }
        "cos" -> { x -> cos(x[0]) }
        "sin" -> { x -> sin(x[0]) }
        "sqrt" -> { v -> sqrt(v[0]) }
        "abs" -> { v -> abs(v[0]) }
        "scale" -> { x -> scale(x[0]) }
        "rgb", "rgba" -> { x -> rgba(x[0]) }
        "translate" -> { x -> translate(x[0]) }
        "transpose" -> { x -> transpose(x[0]) }
        "inverse" -> { x -> inverse(x[0]) }
        "normalize" -> { x -> normalize(x[0]) }
        else -> functions[name]?.let { { x: Array<Any> -> it.invoke(x[0]) } }
    }
}