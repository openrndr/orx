package org.openrndr.extra.expressions.typed

import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import org.openrndr.math.mix as mix_

internal fun mix(x: Any, y: Any, f: Any): Any {
    return when {
        x is Double && y is Double && f is Double -> mix_(x, y, f)
        x is Vector2 && y is Vector2 && f is Double -> mix_(x, y, f)
        x is Vector3 && y is Vector3 && f is Double -> mix_(x, y, f)
        x is Vector4 && y is Vector4 && f is Double -> mix_(x, y, f)
        else -> error("unsupported arguments")
    }
}

internal fun vec3(x: Any, y: Any, z: Any): Vector3 {
    require(x is Double && y is Double && z is Double)
    return Vector3(x, y, z)
}

internal fun vec4(x: Any, y: Any, z: Any): Vector4 {
    return when {
        x is Vector2 && y is Double && z is Double -> Vector4(x.x, x.y, y, z)
        x is Double && y is Vector2 && z is Double -> Vector4(x, y.x, y.y, z)
        x is Double && y is Double && z is Vector2 -> Vector4(x, y, z.x, z.y)
        else -> error("unsupported arguments")
    }
}

internal fun dispatchFunction3(name: String, functions: Map<String, TypedFunction3>): ((Array<Any>) -> Any)? {
    return when (name) {
        "vec3" -> { x -> vec3(x[0], x[1], x[2]) }
        "vec4" -> { x -> vec4(x[0], x[1], x[2]) }
        "mix" -> { x -> mix(x[0], x[1], x[2]) }
        else -> functions[name]?.let {
            { x: Array<Any> ->
                it.invoke(x[0], x[1], x[2])
            }
        }
    }
}