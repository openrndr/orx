package org.openrndr.extras.color.spaces

import org.openrndr.color.*
import org.openrndr.math.mixAngle
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Color in cylindrical OKLab space
 */
data class ColorOKLCHa(val l: Double, val c: Double, val h: Double, val a: Double = 1.0) : ConvertibleToColorRGBa,
    OpacifiableColor<ColorOKLCHa>,
    ShadableColor<ColorOKLCHa>,
    HueShiftableColor<ColorOKLCHa>,
    SaturatableColor<ColorOKLCHa>,
    AlgebraicColor<ColorOKLCHa> {

    companion object {
        fun fromColorOKLABa(oklaba: ColorOKLABa): ColorOKLCHa {
            val l = oklaba.l
            val c = sqrt(oklaba.a * oklaba.a + oklaba.b * oklaba.b)
            var h = atan2(oklaba.b, oklaba.a)

            if (h < 0) {
                h += Math.PI * 2
            }
            h = Math.toDegrees(h)
            return ColorOKLCHa(l, c, h, oklaba.alpha)
        }
    }

    override fun opacify(factor: Double) = copy(a = a * factor)
    override fun shade(factor: Double) = copy(l = l * factor)
    override fun shiftHue(shiftInDegrees: Double) = copy(h = h + shiftInDegrees)
    override fun saturate(factor: Double) = copy(c = c * factor)

    override fun plus(right: ColorOKLCHa) = copy(l = l + right.l, c = c + right.c, h = h + right.h, a = a + right.a)
    override fun minus(right: ColorOKLCHa) = copy(l = l - right.l, c = c - right.c, h = h - right.h, a = a - right.a)
    override fun times(scale: Double) = copy(l = l * scale, c = c * scale, h = h * scale, a = a * scale)
    override fun mix(other: ColorOKLCHa, factor: Double) = mix(this, other, factor)

    fun toOKLABa(): ColorOKLABa {
        val a = c * cos(Math.toRadians(h))
        val b = c * sin(Math.toRadians(h))
        return ColorOKLABa(l, a, b, alpha = this.a)
    }

    override fun toRGBa() = toOKLABa().toRGBa()
}

fun mix(left: ColorOKLCHa, right: ColorOKLCHa, x: Double): ColorOKLCHa {
    val sx = x.coerceIn(0.0, 1.0)
    return ColorOKLCHa(
        (1.0 - sx) * left.l + sx * right.l,
        (1.0 - sx) * left.c + sx * right.c,
        mixAngle(left.h, right.h, sx),
        (1.0 - sx) * left.a + sx * right.a
    )
}

fun ColorRGBa.toOKLCHa() = ColorOKLABa.fromRGBa(this).toOKLCHa()
