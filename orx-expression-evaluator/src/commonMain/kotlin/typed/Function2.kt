package org.openrndr.extra.expressions.typed

import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import org.openrndr.math.transforms.rotate
import org.openrndr.math.transforms.translate
import org.openrndr.math.min as min_
import org.openrndr.math.max as max_

import kotlin.math.max as max_
import kotlin.math.min as min_

internal fun rotate(axis: Any, angleInDegrees:Any): Matrix44 {
    require(angleInDegrees is Double)
    @Suppress("NAME_SHADOWING") val axis = when(axis) {
        is Vector2 -> axis.xy0
        is Vector3 -> axis
        else -> error("unsupported axis argument")
    }
    return Matrix44.rotate(axis, angleInDegrees)
}


internal fun min(x: Any, y: Any): Any {
    return when {
        x is Double && y is Double -> min_(x, y)
        x is Vector2 && y is Vector2 -> min_(x, y)
        x is Vector3 && y is Vector3 -> min_(x, y)
        x is Vector4 && y is Vector4 -> min_(x, y)
        else -> error("unsupported arguments")
    }
}

internal fun max(x: Any, y: Any): Any {
    return when {
        x is Double && y is Double -> max_(x, y)
        x is Vector2 && y is Vector2 -> max_(x, y)
        x is Vector3 && y is Vector3 -> max_(x, y)
        x is Vector4 && y is Vector4 -> max_(x, y)
        else -> error("unsupported arguments")
    }
}

internal fun vec2(x: Any, y: Any): Vector2 {
    require(x is Double)
    require(y is Double)
    return Vector2(x, y)
}

internal fun vec3(x: Any, y: Any): Vector3 = when {
    x is Double && y is Vector2 -> {
        Vector3(x, y.x, y.y)
    }
    x is Vector2 && y is Double -> {
        Vector3(x.x, x.y, y)
    }
    else -> {
        error("unsupported arguments, '$x' (${x::class}) '$y' (${y::class}")
    }
}

internal fun dispatchFunction2(name: String, functions: Map<String, TypedFunction2>): ((Array<Any>) -> Any)? {
    return when (name) {
            "min" -> { x -> min(x[0], x[1]) }
            "max" -> { x -> max(x[0], x[1]) }
            "vec2" -> { x -> vec2(x[0], x[1]) }
            "vec3" -> { x -> vec3(x[0], x[1]) }
            "rotate" -> { x -> rotate(x[0], x[1]) }
            else -> functions[name]?.let { { x: Array<Any> -> it.invoke(x[0], x[1]) } }
    }
}