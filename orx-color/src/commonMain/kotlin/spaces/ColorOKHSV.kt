package org.openrndr.extra.color.spaces

import org.openrndr.color.*
import org.openrndr.math.mixAngle
import kotlin.math.*


data class ColorOKHSVa(val h: Double, val s: Double, val v: Double, val a: Double = 1.0) :
    HueShiftableColor<ColorOKHSVa>,
    OpacifiableColor<ColorOKHSVa>,
    SaturatableColor<ColorOKHSVa>,
    ShadableColor<ColorOKHSVa>,
    AlgebraicColor<ColorOKHSVa>,
    ConvertibleToColorRGBa {

    companion object {
        fun fromColorRGBa(c: ColorRGBa): ColorOKHSVa {
            val lab = c.toOKLABa()
            var C = sqrt(lab.a * lab.a + lab.b * lab.b);
            val a_ = lab.a / C;
            val b_ = lab.b / C;

            var L = lab.l
            val h = 0.5 + 0.5 * atan2(-lab.b, -lab.a) / PI;

            val ST_max = get_ST_max(a_, b_);
            val S_max = ST_max[0];
            val S_0 = 0.5;
            val T = ST_max[1];
            val k = 1 - S_0 / S_max;

            val t = T / (C + L * T);
            val L_v = t * L;
            val C_v = t * C;

            val L_vt = toe_inv(L_v);
            val C_vt = C_v * L_vt / L_v;

            val rgb_scale = ColorOKLABa(L_vt, a_ * C_vt, b_ * C_vt, c.a).toRGBa().toLinear()
            val scale_L = (1.0 / (max(rgb_scale.r, rgb_scale.g, rgb_scale.b, 0.0))).pow(1.0 / 3.0)

            L = L / scale_L;
            C = C / scale_L;

            C = C * toe(L) / L;
            L = toe(L);

            val v = L / L_v;
            val s = (S_0 + T) * C_v / ((T * S_0) + T * k * C_v)

            return ColorOKHSVa(h, s, v, c.a)
        }
    }

    override fun toRGBa(): ColorRGBa {
        val a_ = cos(2 * PI * h)
        val b_ = sin(2 * PI * h)

        val ST_max = get_ST_max(a_, b_)
        val S_max = ST_max[0];
        val S_0 = 0.5;
        val T = ST_max[1];
        val k = 1 - S_0 / S_max;

        val L_v = 1 - s * S_0 / (S_0 + T - T * k * s)
        val C_v = s * T * S_0 / (S_0 + T - T * k * s)

        var L = v * L_v;
        var C = v * C_v;

        // to present steps along the way
        //L = v;
        //C = v*s*S_max;
        //L = v*(1 - s*S_max/(S_max+T));
        //C = v*s*S_max*T/(S_max+T);

        val L_vt = toe_inv(L_v);
        val C_vt = C_v * L_vt / L_v;

        val L_new = toe_inv(L); // * L_v/L_vt;
        C = C * L_new / L;
        L = L_new;

        val rgb_scale =
            ColorOKLABa(L_vt, a_ * C_vt, b_ * C_vt, a).toRGBa().toLinear()// oklab_to_linear_srgb(L_vt,a_*C_vt,b_*C_vt);
        val scale_L = (1.0 / (max(rgb_scale.r, rgb_scale.g, rgb_scale.b, 0.0))).pow(1.0 / 3.0)

        // remove to see effect without rescaling
        L *= scale_L;
        C *= scale_L;

        return ColorOKLABa(L, C * a_, C * b_).toRGBa().toSRGB()
    }

    override fun shiftHue(shiftInDegrees: Double): ColorOKHSVa {
        val normalizedShift = shiftInDegrees / 360.0
        return copy(h = h + normalizedShift)
    }

    override fun opacify(factor: Double): ColorOKHSVa {
        return copy(a = a * factor)
    }

    override fun saturate(factor: Double): ColorOKHSVa {
        return copy(s = s * factor)
    }

    override fun shade(factor: Double): ColorOKHSVa {
        return copy(v = v * factor)
    }

    override fun minus(right: ColorOKHSVa) =
        copy(h = h - right.h, s = s - right.s, v = v - right.v, a = a - right.a)

    override fun plus(right: ColorOKHSVa) =
        copy(h = h + right.h, s = s + right.s, v = v + right.v, a = a + right.a)

    override fun times(scale: Double): ColorOKHSVa = copy(h = h * scale, s = s * scale, v = v * scale, a = a * scale)

    override fun mix(other: ColorOKHSVa, factor: Double): ColorOKHSVa {
        val sx = factor.coerceIn(0.0, 1.0)
        return ColorOKHSVa(
            mixAngle(h * 360.0, other.h * 360.0, sx) / 360.0,
            (1.0 - sx) * s + sx * other.s,
            (1.0 - sx) * v + sx * other.v,
            (1.0 - sx) * a + sx * other.a
        )
    }
}

fun ColorRGBa.toOKHSVa(): ColorOKHSVa = ColorOKHSVa.fromColorRGBa(this)