package org.openrndr.extra.color.spaces

import org.openrndr.color.*
import org.openrndr.math.Vector4
import org.openrndr.math.mixAngle
import kotlin.math.*

data class ColorOKHSVa(val h: Double, val s: Double, val v: Double, override val alpha: Double = 1.0) :
    ColorModel<ColorOKHSVa>,
    HueShiftableColor<ColorOKHSVa>,
    SaturatableColor<ColorOKHSVa>,
    ShadableColor<ColorOKHSVa>,
    AlgebraicColor<ColorOKHSVa> {

    companion object {
        fun fromColorRGBa(c: ColorRGBa): ColorOKHSVa {
            val lab = c.toOKLABa()
            var C = sqrt(lab.a * lab.a + lab.b * lab.b);
            val a_ = if (C != 0.0) lab.a / C else 0.0
            val b_ = if (C != 0.0) lab.b / C else 0.0

            var L = lab.l
            val h = 0.5 + 0.5 * atan2(-lab.b, -lab.a) / PI;

            val ST_max = get_ST_max(a_, b_);
            val S_max = ST_max[0];
            val S_0 = 0.5;
            val T = ST_max[1];
            val k =  if (S_max != 0.0) (1 - S_0 / S_max) else 0.0

            val t = T / (C + L * T);
            val L_v = t * L;
            val C_v = t * C;

            val L_vt = toeInv(L_v);
            val C_vt = C_v * L_vt / L_v;

            val rgb_scale = ColorOKLABa(L_vt, a_ * C_vt, b_ * C_vt, c.a).toRGBa().toLinear()
            val scale_L = (1.0 / (max(rgb_scale.r, rgb_scale.g, rgb_scale.b, 0.0))).pow(1.0 / 3.0)

            L = L / scale_L;
            C = C / scale_L;

            C = C * toe(L) / L;
            L = toe(L);

            val v = L / L_v;
            val s = (S_0 + T) * C_v / ((T * S_0) + T * k * C_v)

            return ColorOKHSVa(h * 360.0, if (s == s) s else 0.0, if (v==v) v else 0.0, c.a)
        }
    }

    @Deprecated("Legacy alpha parameter name", ReplaceWith("alpha"))
    val a = alpha

    override fun toRGBa(): ColorRGBa {
        val a_ = cos(2 * PI * h / 360.0)
        val b_ = sin(2 * PI * h / 360.0)

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

        val L_vt = toeInv(L_v);
        val C_vt = C_v * L_vt / L_v;

        val L_new = toeInv(L); // * L_v/L_vt;
        C = C * L_new / L;
        L = L_new;

        val rgb_scale =
            ColorOKLABa(L_vt, a_ * C_vt, b_ * C_vt, alpha).toRGBa().toLinear()// oklab_to_linear_srgb(L_vt,a_*C_vt,b_*C_vt);
        val scale_L = (1.0 / (max(rgb_scale.r, rgb_scale.g, rgb_scale.b, 0.0))).pow(1.0 / 3.0)

        // remove to see effect without rescaling
        L *= scale_L;
        C *= scale_L;

        return ColorOKLABa(
            if (L == L) L else 0.0,
            if (C == C) C * a_ else 0.0,
            if (C == C) C * b_ else 0.0).toRGBa().toSRGB()
    }

    override fun shiftHue(shiftInDegrees: Double): ColorOKHSVa = copy(h = h + shiftInDegrees)
    override fun opacify(factor: Double): ColorOKHSVa = copy(alpha = alpha * factor)
    override fun saturate(factor: Double): ColorOKHSVa = copy(s = s * factor)
    override fun shade(factor: Double): ColorOKHSVa = copy(v = v * factor)
    override fun minus(right: ColorOKHSVa) =
        copy(h = h - right.h, s = s - right.s, v = v - right.v, alpha = alpha - right.alpha)
    override fun plus(right: ColorOKHSVa) =
        copy(h = h + right.h, s = s + right.s, v = v + right.v, alpha = alpha + right.alpha)
    override fun times(scale: Double): ColorOKHSVa = copy(h = h * scale, s = s * scale, v = v * scale, alpha = alpha * scale)

    override fun mix(other: ColorOKHSVa, factor: Double): ColorOKHSVa {
        val sx = factor.coerceIn(0.0, 1.0)
        return ColorOKHSVa(
            mixAngle(h, other.h, sx),
            (1.0 - sx) * s + sx * other.s,
            (1.0 - sx) * v + sx * other.v,
            (1.0 - sx) * alpha + sx * other.alpha
        )
    }

    override fun toVector4(): Vector4 = Vector4(h, s, v, alpha)
}

fun ColorRGBa.toOKHSVa(): ColorOKHSVa = ColorOKHSVa.fromColorRGBa(this)