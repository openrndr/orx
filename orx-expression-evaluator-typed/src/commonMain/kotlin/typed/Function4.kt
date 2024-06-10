package org.openrndr.extra.expressions.typed

import org.openrndr.color.ColorRGBa
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector4

internal fun vec4(x: Any, y: Any, z: Any, w: Any): Vector4 {
    require(x is Double && y is Double && z is Double && w is Double)
    return Vector4(x, y, z, w)
}

internal fun mat4(x: Any, y: Any, z: Any, w: Any): Matrix44 {
    require(x is Vector4 && y is Vector4 && z is Vector4 && w is Vector4)
    return Matrix44.fromColumnVectors(x, y, z, w)
}

internal fun rgba(r: Any, g: Any, b: Any, a: Any): ColorRGBa {
    require(r is Double && g is Double && b is Double && a is Double)
    return ColorRGBa(r, g, b, a)
}


internal fun dispatchFunction4(name: String, functions: Map<String, TypedFunction4>): ((Array<Any>) -> Any)? {
    return when (name) {
        "vec4" -> { x -> vec4(x[0], x[1], x[2], x[3]) }
        "mat4" -> { x -> mat4(x[0], x[1], x[2], x[3]) }
        "rgba" -> { x -> rgba(x[0], x[1], x[2], x[3]) }
        else -> functions[name]?.let { { x: Array<Any> -> it.invoke(x[0], x[1], x[2], x[3]) } }
    }
}