package org.openrndr.extra.color.spaces

import kotlinx.serialization.Serializable
import org.openrndr.color.*
import org.openrndr.math.*
import kotlin.jvm.JvmRecord
import kotlin.math.*


/**
 * Represents a color in the OKLCH color space, which is based on the OKLab color space with added polar coordinates for chroma and hue.
 *
 * @property l Lightness of the color. Range: 0.0 (black) to 1.0 (white).
 * @property c Chroma, representing color intensity. Typically non-negative.
 * @property h Hue angle in degrees. Range: 0.0 to 360.0.
 * @property alpha Opacity of the color. Range: 0.0 (fully transparent) to 1.0 (fully opaque). Default is 1.0.
 */
@Serializable
@JvmRecord
data class ColorOKLCHa(val l: Double, val c: Double, val h: Double, override val alpha: Double = 1.0) :
    ColorModel<ColorOKLCHa>,
    ShadableColor<ColorOKLCHa>,
    ChromaColor<ColorOKLCHa>,
    HueShiftableColor<ColorOKLCHa>,
    LuminosityColor<ColorOKLCHa>,
    AlgebraicColor<ColorOKLCHa> {

    companion object {
        fun fromColorOKLABa(oklaba: ColorOKLABa): ColorOKLCHa {
            val l = oklaba.l
            val c = sqrt(oklaba.a * oklaba.a + oklaba.b * oklaba.b)
            var h = atan2(oklaba.b, oklaba.a)

            if (h < 0) {
                h += PI * 2
            }
            h = h.asDegrees
            return ColorOKLCHa(l, c, h, oklaba.alpha)
        }
    }

    override fun opacify(factor: Double) = copy(alpha = alpha * factor)
    override fun shade(factor: Double) = copy(l = l * factor)

    override fun plus(right: ColorOKLCHa) = copy(l = l + right.l, c = c + right.c, h = h + right.h, alpha = alpha + right.alpha)
    override fun minus(right: ColorOKLCHa) = copy(l = l - right.l, c = c - right.c, h = h - right.h, alpha = alpha - right.alpha)
    override fun times(scale: Double) = copy(l = l * scale, c = c * scale, h = h * scale, alpha = alpha * scale)
    override fun mix(other: ColorOKLCHa, factor: Double) = mix(this, other, factor)

    fun toOKLABa(): ColorOKLABa {
        val a = c * cos(h.asRadians)
        val b = c * sin(h.asRadians)
        return ColorOKLABa(l, a, b, alpha = this.alpha)
    }

    override fun toRGBa(): ColorRGBa = toOKLABa().toRGBa()
    override fun toVector4(): Vector4 = Vector4(l, c, h, alpha)
    override val chroma: Double
        get() = c * 100.0
    override fun withChroma(chroma: Double): ColorOKLCHa = copy(c = chroma / 100.0)
    override val hue: Double
        get() = h

    override fun withHue(hue: Double): ColorOKLCHa = copy(h = hue)
    override val luminosity: Double
        get() = l * 100.0

    override fun withLuminosity(luminosity: Double): ColorOKLCHa = copy(l = luminosity / 100.0)
}

fun mix(left: ColorOKLCHa, right: ColorOKLCHa, x: Double): ColorOKLCHa {
    val sx = x.coerceIn(0.0, 1.0)
    return ColorOKLCHa(
        (1.0 - sx) * left.l + sx * right.l,
        (1.0 - sx) * left.c + sx * right.c,
        mixAngle(left.h, right.h, sx),
        (1.0 - sx) * left.alpha + sx * right.alpha
    )
}

fun ColorRGBa.toOKLCHa() = ColorOKLABa.fromRGBa(this).toOKLCHa()
