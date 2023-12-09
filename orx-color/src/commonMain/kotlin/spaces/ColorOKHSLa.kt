package org.openrndr.extra.color.spaces

import kotlinx.serialization.Serializable
import org.openrndr.color.*
import org.openrndr.math.Vector4
import org.openrndr.math.mixAngle
import kotlin.math.*

@Suppress("LocalVariableName")
@Serializable
data class ColorOKHSLa(val h: Double, val s: Double, val l: Double, override val alpha: Double = 1.0) :
    ColorModel<ColorOKHSLa>,
    HueShiftableColor<ColorOKHSLa>,
    SaturatableColor<ColorOKHSLa>,
    ShadableColor<ColorOKHSLa>,
    AlgebraicColor<ColorOKHSLa> {

    companion object {
        fun fromColorRGBa(c: ColorRGBa): ColorOKHSLa {
            val lab = c.toOKLABa()
            val C = sqrt(lab.a * lab.a + lab.b * lab.b)
            val a_ = lab.a / C
            val b_ = lab.b / C

            val L = lab.l
            val h = 0.5 + 0.5 * atan2(-lab.b, -lab.a) / PI

            val (c0, cMid, cMax) = get_Cs(L, a_, b_)

            val s = if (C < cMid) {
                val k0 = 0
                val k1 = 0.8 * c0
                val k2 = (1 - k1 / cMid)

                val t = (C - k0) / (k1 + k2 * (C - k0))
                t * 0.8
            } else {
                val k0 = cMid
                val k1 = 0.2 * cMid * cMid * 1.25 * 1.25 / c0
                val k2 = (1 - (k1) / (cMax - cMid))

                val t = (C - k0) / (k1 + k2 * (C - k0))
                0.8 + 0.2 * t
            }
            val l = toe(L)
            return ColorOKHSLa(
                h * 360.0,
                if (s == s) s else 0.0,
                if (l == l) l else 0.0,
                c.alpha
            )
        }
    }

    @Deprecated("Legacy alpha parameter name", ReplaceWith("alpha"))
    val a = alpha

    override fun toRGBa(): ColorRGBa {
        if (l == 0.0 || l == 1.0) {
            return ColorRGBa(l, l, l, alpha, Linearity.SRGB)
        }
        val a_ = cos(2 * PI * h / 360.0)
        val b_ = sin(2 * PI * h / 360.0)
        val L = toeInv(l)

        val Cs = get_Cs(L, a_, b_)
        val C_0 = Cs[0]
        val C_mid = Cs[1]
        val C_max = Cs[2]

        val C = if (s < 0.8) {
            val t = 1.25 * s
            val k_0 = 0.0
            val k_1 = 0.8 * C_0
            val k_2 = (1 - k_1 / C_mid)
            k_0 + t * k_1 / (1 - k_2 * t)
        } else {
            val t = 5 * (s - 0.8)
            val k_0 = C_mid
            val k_1 = 0.2 * C_mid * C_mid * 1.25 * 1.25 / C_0
            val k_2 = (1 - (k_1) / (C_max - C_mid))
            k_0 + t * k_1 / (1 - k_2 * t)
        }

        // If we would only use one of the Cs:
        //C = s*C_0;
        //C = s*1.25*C_mid;
        //C = s*C_max;

        return ColorOKLABa(
            if (L == L) L else 0.0,
            if (C == C) C * a_ else 0.0,
            if (C == C) C * b_ else 0.0,
            alpha
        ).toRGBa().toSRGB()
    }

    override val hue: Double = h

    override fun withHue(hue: Double): ColorOKHSLa = copy(h = hue)

    override fun opacify(factor: Double): ColorOKHSLa = copy(alpha = alpha * factor)
    override val saturation: Double = s

    override fun withSaturation(saturation: Double): ColorOKHSLa = copy(s = saturation)

    override fun shade(factor: Double): ColorOKHSLa = copy(l = l * factor)

    override fun minus(right: ColorOKHSLa) =
        copy(h = h - right.h, s = s - right.s, l = l - right.l, alpha = alpha - right.alpha)

    override fun plus(right: ColorOKHSLa) =
        copy(h = h + right.h, s = s + right.s, l = l + right.l, alpha = alpha + right.alpha)

    override fun times(scale: Double): ColorOKHSLa =
        copy(h = h * scale, s = s * scale, l = l * scale, alpha = alpha * scale)

    override fun mix(other: ColorOKHSLa, factor: Double): ColorOKHSLa {
        val sx = factor.coerceIn(0.0, 1.0)
        return ColorOKHSLa(
            mixAngle(h, other.h, sx),
            (1.0 - sx) * s + sx * other.s,
            (1.0 - sx) * l + sx * other.l,
            (1.0 - sx) * alpha + sx * other.alpha
        )
    }

    override fun toVector4(): Vector4 = Vector4(h, s, l, alpha)
}

fun ColorRGBa.toOKHSLa(): ColorOKHSLa = ColorOKHSLa.fromColorRGBa(this)