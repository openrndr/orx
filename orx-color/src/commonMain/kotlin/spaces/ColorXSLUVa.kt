package org.openrndr.extra.color.spaces

import kotlinx.serialization.Serializable
import org.openrndr.color.*
import org.openrndr.math.Vector4
import org.openrndr.math.mixAngle

@Serializable
data class ColorXSLUVa(val x: Double, val s: Double, val l: Double, override val alpha: Double = 1.0) :
    ColorModel<ColorXSLUVa>,
    HueShiftableColor<ColorXSLUVa>,
    SaturatableColor<ColorXSLUVa>,
    ShadableColor<ColorXSLUVa>,
    AlgebraicColor<ColorXSLUVa> {

    @Deprecated("Legacy alpha parameter name", ReplaceWith("alpha"))
    val a = alpha

    override fun shiftHue(shiftInDegrees: Double) = copy(x = x + (shiftInDegrees))

    override fun shade(factor: Double) = copy(l = l * factor)

    override fun saturate(factor: Double) = copy(s = s * factor)

    override fun toRGBa(): ColorRGBa = toHSLUVa().toRGBa()

    fun toHSLUVa(): ColorHSLUVa = ColorHSLUVa(xToHue(x), s, l, alpha)

    override fun opacify(factor: Double) = copy(alpha = alpha * factor)

    override fun minus(right: ColorXSLUVa) = copy(x = x - right.x, s = s - right.s, l = l - right.l, alpha = alpha - right.alpha)

    override fun plus(right: ColorXSLUVa) = copy(x = x + right.x, s = s + right.s, l = l + right.l, alpha = alpha + right.alpha)

    override fun times(scale: Double) = copy(x = x * scale, s = s * scale, l = l * scale, alpha = alpha * scale)

    override fun mix(other: ColorXSLUVa, factor: Double) = mix(this, other, factor)

    override fun toVector4(): Vector4 = Vector4(x, s, l, alpha)
}

private fun xToHue(x:Double) : Double {
    val x = x % 360.0
    return if (0.0 <= x && x < 60.0) {
        map(x, 0.0, 60.0, 0.0, 35.0)
    } else if (60.0 <= x && x < 120.0) {
        map(x, 60.0, 120.0, 35.0, 60.0)
    } else if (120.0 <= x && x < 180.0) {
        map(x, 120.0, 180.0, 60.0, 135.0)
    } else if (180.0 <= x && x < 240.0) {
        map(x, 180.0, 240.0, 135.0, 225.0)
    } else if (240.0 <= x && x < 300.0) {
        map(x, 240.0, 300.0, 225.0, 275.0)
    } else {
        map(x, 300.0, 360.0, 276.0, 360.0)
    }
}

fun mix(left: ColorXSLUVa, right: ColorXSLUVa, x: Double): ColorXSLUVa {
    val sx = x.coerceIn(0.0, 1.0)
    return ColorXSLUVa(
        mixAngle(left.x, right.x, sx),
        (1.0 - sx) * left.s + sx * right.s,
        (1.0 - sx) * left.l + sx * right.l,
        (1.0 - sx) * left.alpha + sx * right.alpha)
}

fun ColorRGBa.toXSLUVa() = toHSLUVa().toXSLUVa()
