package org.openrndr.extra.color.spaces

import kotlinx.serialization.Serializable
import org.openrndr.color.*
import org.openrndr.math.Vector4
import org.openrndr.math.map
import org.openrndr.math.mixAngle
import kotlin.jvm.JvmRecord

/**
 * Represents a color in the XSLUV color space with an optional alpha transparency value.
 * XSLUV is a cylindrical representation of the HSLUV color space using an alternative X coordinate
 * instead of the standard hue. It provides a perceptually uniform color representation.
 *
 * @property x The X coordinate representing the hue variation in the XSLUV color space.
 * @property s The saturation of the color in the XSLUV color space.
 * @property l The luminance of the color in the XSLUV color space.
 * @property alpha The alpha transparency value of the color, ranging from 0.0 to 1.0. Defaults to 1.0.
 */
@Serializable
@JvmRecord
data class ColorXSLUVa(val x: Double, val s: Double, val l: Double, override val alpha: Double = 1.0) :
    ColorModel<ColorXSLUVa>,
    HueShiftableColor<ColorXSLUVa>,
    SaturatableColor<ColorXSLUVa>,
    ShadableColor<ColorXSLUVa>,
    AlgebraicColor<ColorXSLUVa> {

    override val hue: Double
        get() = x
    override fun withHue(hue: Double): ColorXSLUVa = copy(x = hue)

    override fun shade(factor: Double) = copy(l = l * factor)
    override val saturation: Double
        get() = s

    override fun withSaturation(saturation: Double): ColorXSLUVa = copy(s = saturation)

    override fun toRGBa(): ColorRGBa = toHSLUVa().toRGBa()

    fun toHSLUVa(): ColorHSLUVa = ColorHSLUVa(xToHue(x), s, l, alpha)

    override fun opacify(factor: Double) = copy(alpha = alpha * factor)

    override fun minus(right: ColorXSLUVa) =
        copy(x = x - right.x, s = s - right.s, l = l - right.l, alpha = alpha - right.alpha)

    override fun plus(right: ColorXSLUVa) =
        copy(x = x + right.x, s = s + right.s, l = l + right.l, alpha = alpha + right.alpha)

    override fun times(scale: Double) = copy(x = x * scale, s = s * scale, l = l * scale, alpha = alpha * scale)

    override fun mix(other: ColorXSLUVa, factor: Double) = mix(this, other, factor)

    override fun toVector4(): Vector4 = Vector4(x, s, l, alpha)
}

fun xToHue(x: Double): Double {
    @Suppress("NAME_SHADOWING") val x = x.mod(360.0)
    return if (0.0 <= x && x < 60.0) {
        x.map(0.0, 60.0, 0.0, 35.0)
    } else if (60.0 <= x && x < 120.0) {
        x.map(60.0, 120.0, 35.0, 60.0)
    } else if (120.0 <= x && x < 180.0) {
        x.map(120.0, 180.0, 60.0, 135.0)
    } else if (180.0 <= x && x < 240.0) {
        x.map(180.0, 240.0, 135.0, 225.0)
    } else if (240.0 <= x && x < 300.0) {
        x.map(240.0, 300.0, 225.0, 275.0)
    } else {
        x.map( 300.0, 360.0, 275.0, 360.0)
    }
}

fun mix(left: ColorXSLUVa, right: ColorXSLUVa, x: Double): ColorXSLUVa {
    val sx = x.coerceIn(0.0, 1.0)
    return ColorXSLUVa(
        mixAngle(left.x, right.x, sx),
        (1.0 - sx) * left.s + sx * right.s,
        (1.0 - sx) * left.l + sx * right.l,
        (1.0 - sx) * left.alpha + sx * right.alpha
    )
}

fun ColorRGBa.toXSLUVa() = toHSLUVa().toXSLUVa()
