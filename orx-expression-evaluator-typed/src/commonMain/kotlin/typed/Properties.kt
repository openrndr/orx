package org.openrndr.extra.expressions.typed

import org.openrndr.color.ColorRGBa
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4

internal fun String.property(property: String): Any {
    return when (property) {
        "length" -> this.length.toDouble()
        "uppercase" -> this.uppercase()
        "lowercase" -> this.lowercase()
        "reversed" -> this.reversed()
        else -> error("unknown property '$property'")
    }
}

internal fun Vector2.property(property: String): Any {
    return when (property) {
        "x" -> x
        "y" -> y
        "xx" -> xx
        "yx" -> yx
        "yy" -> yy
        "xy" -> this
        "xxx" -> Vector3(x, x, x)
        "xxy" -> Vector3(x, x, y)
        "length" -> length
        "normalized" -> normalized
        else -> error("unknown property '$property")
    }
}

internal fun Vector3.property(property: String): Any {
    return when (property) {
        "x" -> x
        "y" -> y
        "z" -> z
        "xx" -> Vector2(x, x)
        "yx" -> Vector2(y, x)
        "yy" -> Vector2(y, y)
        "xy" -> Vector2(x, y)
        "zx" -> Vector2(z, x)
        "xz" -> Vector2(x, z)
        "xxx" -> Vector3(x, x, x)
        "xxy" -> Vector3(x, x, y)
        "length" -> length
        "normalized" -> normalized

        else -> error("unknown property '$property")
    }
}

internal fun Vector4.property(property: String): Any {
    return when (property) {
        "x" -> x
        "y" -> y
        "z" -> z
        "xx" -> Vector2(x, x)
        "yx" -> Vector2(y, x)
        "yy" -> Vector2(y, y)
        "xy" -> Vector2(x, y)
        "zx" -> Vector2(z, x)
        "xz" -> Vector2(x, z)
        "xyz" -> Vector3(x, y, z)
        "xxy" -> Vector3(x, x, y)
        "length" -> length
        "normalized" -> normalized
        else -> error("unknown property '$property")
    }
}

internal fun ColorRGBa.property(property: String): Any {
    return when (property) {
        "r" -> r
        "g" -> g
        "b" -> b
        "a" -> alpha
        "linear" -> toLinear()
        "srgb" -> toSRGB()
        else -> error("unknown property '$property")
    }
}

internal fun Matrix44.property(property: String): Any {
    return when (property) {
        "inversed" -> inversed
        "transposed" -> transposed
        else -> error("unknown property '$property")
    }
}