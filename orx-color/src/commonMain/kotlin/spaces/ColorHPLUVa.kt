package org.openrndr.extra.color.spaces

import kotlinx.serialization.Serializable
import org.openrndr.color.*
import org.openrndr.math.Vector4
import org.openrndr.math.mixAngle
import kotlin.jvm.JvmRecord

/**
 * Represents a color in the HPLUVa (Hue, Perceptual Lightness, Saturation, Alpha) color space.
 * This color space is based on perceptual uniformity, making it suitable for operations
 * like interpolation, shading, and manipulation of hue, saturation, and lightness values.
 *
 * @property h The hue component of the color, representing the angle on the color wheel in degrees [0, 360).
 * @property s The saturation component of the color, representing the intensity of the color [0.0, 1.0].
 * @property l The lightness component of the color, representing the relative brightness [0.0, 1.0].
 * @property alpha The alpha (opacity) component of the color, ranging from fully transparent (0.0) to fully opaque (1.0).
 */
@Serializable
@JvmRecord
data class ColorHPLUVa(val h: Double, val s: Double, val l: Double, override val alpha: Double = 1.0) :
    ColorModel<ColorHPLUVa>,
    HueShiftableColor<ColorHPLUVa>,
    SaturatableColor<ColorHPLUVa>,
    ShadableColor<ColorHPLUVa>,
    LuminosityColor<ColorHPLUVa>,
    AlgebraicColor<ColorHPLUVa> {
    fun toLCHUVa(): ColorLCHUVa {
        val l1 = l
        if (l1 > 0.9999999) {
            return ColorLCHUVa(100.0, 0.0, h)
        }
        if (l1 < 0.00000001) {
            return ColorLCHUVa(0.0, 0.0, h)
        }
        val l100 = l1 * 100.0
        val max100 = maxSafeChromaForL(l100)
        val c100 = max100 * s
        return ColorLCHUVa(l100, c100, h)
    }

    override val hue: Double
        get() = h
    override fun withHue(hue: Double) = copy(h = hue)


    override fun shade(factor: Double): ColorHPLUVa = copy(l = l * factor)

    override val saturation: Double
        get() = s

    override fun withSaturation(saturation: Double): ColorHPLUVa = copy(s = saturation)

    override fun toRGBa(): ColorRGBa = toLCHUVa().toRGBa()

    override fun opacify(factor: Double) = copy(alpha = alpha * factor)

    override fun minus(right: ColorHPLUVa) = copy(h = h - right.h, s = s - right.s, l = l - right.l, alpha = alpha - right.alpha)

    override fun plus(right: ColorHPLUVa) = copy(h = h + right.h, s = s + right.s, l = l + right.l, alpha = alpha + right.alpha)

    override fun times(scale: Double) = copy(h = h * scale, s = s * scale, l = l * scale, alpha = alpha * scale)

    override fun mix(other: ColorHPLUVa, factor: Double) = mix(this, other, factor)

    override fun toVector4(): Vector4 = Vector4(h, s, l, alpha)
    override val luminosity: Double
        get() = l

    override fun withLuminosity(luminosity: Double): ColorHPLUVa = copy(l = luminosity)
}

fun mix(left: ColorHPLUVa, right: ColorHPLUVa, x: Double): ColorHPLUVa {
    val sx = x.coerceIn(0.0, 1.0)
    return ColorHPLUVa(
        mixAngle(left.h, right.h, sx),
        (1.0 - sx) * left.s + sx * right.s,
        (1.0 - sx) * left.l + sx * right.l,
        (1.0 - sx) * left.alpha + sx * right.alpha)
}

fun ColorRGBa.toHPLUVa(): ColorHPLUVa = toLCHUVa().toHPLUVa()

fun ColorLCHUVa.toHPLUVa(): ColorHPLUVa {
    val l100 = l
    if (l100 > 99.9999999) {
        return ColorHPLUVa(h, 0.0, 1.0)
    }
    if (l100 < 0.00000001) {
        return ColorHPLUVa(h, 0.0, 0.0)

    }
    val max100 = maxSafeChromaForL(l)
    val s1 = c / max100
    return ColorHPLUVa(h, s1, l100 / 100.0)
}
