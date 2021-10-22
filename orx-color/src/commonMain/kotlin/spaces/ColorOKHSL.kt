package org.openrndr.extra.color.spaces

import org.openrndr.color.*
import org.openrndr.math.mixAngle
import kotlin.math.*

data class ColorOKHSLa(val h: Double, val s: Double, val l: Double, val a: Double = 1.0) :
    HueShiftableColor<ColorOKHSLa>,
    OpacifiableColor<ColorOKHSLa>,
    SaturatableColor<ColorOKHSLa>,
    ShadableColor<ColorOKHSLa>,
    AlgebraicColor<ColorOKHSLa>,
    ConvertibleToColorRGBa {

    companion object {
        fun fromColorRGBa(c: ColorRGBa): ColorOKHSLa {
            val lab = c.toOKLABa()
            val C = sqrt(lab.a * lab.a + lab.b * lab.b);
            val a_ = lab.a / C;
            val b_ = lab.b / C;

            val L = lab.l
            val h = 0.5 + 0.5 * atan2(-lab.b, -lab.a) / PI;

            val Cs = get_Cs(L, a_, b_)
            val C_0 = Cs[0];
            val C_mid = Cs[1];
            val C_max = Cs[2];


            val s = if (C < C_mid) {
                val k_0 = 0;
                val k_1 = 0.8 * C_0;
                val k_2 = (1 - k_1 / C_mid);

                val t = (C - k_0) / (k_1 + k_2 * (C - k_0));
                t * 0.8;
            } else {
                val k_0 = C_mid;
                val k_1 = 0.2 * C_mid * C_mid * 1.25 * 1.25 / C_0;
                val k_2 = (1 - (k_1) / (C_max - C_mid));

                val t = (C - k_0) / (k_1 + k_2 * (C - k_0));
                0.8 + 0.2 * t;
            }
            val l = toe(L);
            return ColorOKHSLa(h, s, l, c.a)
        }
    }

    override fun toRGBa(): ColorRGBa {
        if (l == 1.0) {
            ColorRGBa(1.0, 1.0, 1.0, a)
        } else if (l == 0.0) {
            ColorRGBa(0.0, 0.0, 0.0, a)
        }
        val a_ = cos(2 * PI * h);
        val b_ = sin(2 * PI * h);
        val L = toe_inv(l);

        val Cs = get_Cs(L, a_, b_);
        val C_0 = Cs[0];
        val C_mid = Cs[1];
        val C_max = Cs[2];

        //let C, t, k_0, k_1, k_2;
        val C: Double
        val t: Double
        val k_0: Double
        val k_1: Double
        val k_2: Double
        if (s < 0.8) {
            t = 1.25 * s;
            k_0 = 0.0
            k_1 = 0.8 * C_0;
            k_2 = (1 - k_1 / C_mid);
        } else {
            t = 5 * (s - 0.8);
            k_0 = C_mid;
            k_1 = 0.2 * C_mid * C_mid * 1.25 * 1.25 / C_0;
            k_2 = (1 - (k_1) / (C_max - C_mid));
        }

        C = k_0 + t * k_1 / (1 - k_2 * t);

        // If we would only use one of the Cs:
        //C = s*C_0;
        //C = s*1.25*C_mid;
        //C = s*C_max;

//        let rgb = oklab_to_linear_srgb(L, C*a_, C*b_);
//        return [
//            255*srgb_transfer_function(rgb[0]),
//            255*srgb_transfer_function(rgb[1]),
//            255*srgb_transfer_function(rgb[2]),
//        ]
        return ColorOKLABa(L, C * a_, C * b_).toRGBa().toSRGB()
    }

    override fun shiftHue(shiftInDegrees: Double): ColorOKHSLa {
        val normalizedShift = shiftInDegrees / 360.0
        return copy(h = h + normalizedShift)
    }

    override fun opacify(factor: Double): ColorOKHSLa {
        return copy(a = a * factor)
    }

    override fun saturate(factor: Double): ColorOKHSLa {
        return copy(s = s * factor)
    }

    override fun shade(factor: Double): ColorOKHSLa {
        return copy(l = l * factor)
    }

    override fun minus(right: ColorOKHSLa) =
        copy(h = h - right.h, s = s - right.s, l = l - right.l, a = a - right.a)

    override fun plus(right: ColorOKHSLa) =
        copy(h = h + right.h, s = s + right.s, l = l + right.l, a = a + right.a)

    override fun times(scale: Double): ColorOKHSLa = copy(h = h * scale, s = s * scale, l = l * scale, a = a * scale)

    override fun mix(other: ColorOKHSLa, factor: Double): ColorOKHSLa {
        val sx = factor.coerceIn(0.0, 1.0)
        return ColorOKHSLa(
            mixAngle(h * 360.0, other.h * 360.0, sx) / 360.0,
            (1.0 - sx) * s + sx * other.s,
            (1.0 - sx) * l + sx * other.l,
            (1.0 - sx) * a + sx * other.a
        )
    }

}

fun ColorRGBa.toOKHSLa(): ColorOKHSLa = ColorOKHSLa.fromColorRGBa(this)