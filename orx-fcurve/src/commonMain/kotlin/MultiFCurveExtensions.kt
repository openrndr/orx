package org.openrndr.extra.fcurve

import org.openrndr.color.ColorRGBa
import org.openrndr.math.*

fun MultiFCurve.boolean(value: String, default: Boolean = true) = BooleanFCurve(this[value], default)
fun MultiFCurve.double(value: String, default: Double = 0.0) = DoubleFCurve(this[value], default)

fun MultiFCurve.int(value: String, default: Int = 0) = IntFCurve(this[value], default)

fun MultiFCurve.vector2(x: String, y: String, default: Vector2 = Vector2.ZERO) =
    Vector2FCurve(this[x], this[y], default)

fun MultiFCurve.vector3(x: String, y: String, z: String, default: Vector3 = Vector3.ZERO) =
    Vector3FCurve(this[x], this[y], this[z], default)

fun MultiFCurve.vector4(x: String, y: String, z: String, w: String, default: Vector4 = Vector4.ZERO) =
    Vector4FCurve(this[x], this[y], this[z], this[w], default)

fun MultiFCurve.rgb(r: String, g: String, b: String, default: ColorRGBa = ColorRGBa.WHITE) =
    RgbFCurve(this[r], this[g], this[b], default)

fun MultiFCurve.rgba(r: String, g: String, b: String, a: String, default: ColorRGBa = ColorRGBa.WHITE) =
    RgbaFCurve(this[r], this[g], this[b], this[a], default)

fun MultiFCurve.polar(angleInDegrees: String, radius: String, default: Polar = Polar(0.0, 1.0)) =
    PolarFCurve(this[angleInDegrees], this[radius], default)

fun MultiFCurve.spherical(
    thetaInDegrees: String,
    phiInDegrees: String,
    radius: String,
    default: Spherical = Spherical(0.0, 0.0, 1.0)
) = SphericalFCurve(this[thetaInDegrees], this[phiInDegrees], this[radius], default)