package org.openrndr.extra.expressions.typed

import org.openrndr.color.ColorRGBa
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Polar
import org.openrndr.math.Spherical
import org.openrndr.math.Vector4
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle

internal fun String.property(property: String): Any? {
    return when (property) {
        "length" -> this.length.toDouble()
        "uppercase" -> this.uppercase()
        "lowercase" -> this.lowercase()
        "reversed" -> this.reversed()
        else -> null
    }
}

internal fun Vector2.property(property: String): Any? {
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
        else -> null
    }
}

internal fun Vector3.property(property: String): Any? {
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

        else -> null
    }
}

internal fun Vector4.property(property: String): Any? {
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
        else -> null
    }
}

internal fun ColorRGBa.property(property: String): Any? {
    return when (property) {
        "r" -> r
        "g" -> g
        "b" -> b
        "a" -> alpha
        "linear" -> toLinear()
        "srgb" -> toSRGB()
        else -> null
    }
}

internal fun Matrix44.property(property: String): Any? {
    return when (property) {
        "inversed" -> inversed
        "transposed" -> transposed
        else -> null
    }
}
internal fun Circle.property(property: String): Any? {
    return when (property) {
        "center" -> center
        "x" -> center.x
        "y" -> center.y
        "radius" -> radius
        "area" -> kotlin.math.PI * radius * radius
        else -> null
    }
}

internal fun Rectangle.property(property: String): Any? {
    return when (property) {
        "x" -> x
        "y" -> y
        "width" -> width
        "height" -> height
        "center" -> center
        "corner" -> corner
        "area" -> width * height
        else -> null
    }
}

internal fun Polar.property(property: String): Any? {
    return when (property) {
        "theta" -> theta
        "radius" -> radius
        "cartesian" -> cartesian
        else -> null
    }
}

internal fun Spherical.property(property: String): Any? {
    return when (property) {
        "theta" -> theta
        "phi" -> phi
        "radius" -> radius
        "cartesian" -> cartesian
        else -> null
    }
}

/** the built-in property [name] of [receiver], null when it has none of that name */
internal fun builtInProperty(receiver: Any, name: String): Any? = when (receiver) {
    is Vector2 -> receiver.property(name)
    is Vector3 -> receiver.property(name)
    is Vector4 -> receiver.property(name)
    is ColorRGBa -> receiver.property(name)
    is Matrix44 -> receiver.property(name)
    is String -> receiver.property(name)
    is Circle -> receiver.property(name)
    is Rectangle -> receiver.property(name)
    is Polar -> receiver.property(name)
    is Spherical -> receiver.property(name)
    else -> null
}
