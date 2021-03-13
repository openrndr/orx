package org.openrndr.extras.color.spaces

import org.openrndr.color.*
import kotlin.math.pow

/**
 * Color in OKLab color space
 */
data class ColorOKLABa(val l: Double, val a: Double, val b: Double, val alpha: Double = 1.0) :
    ConvertibleToColorRGBa,
    ShadableColor<ColorOKLABa>,
    OpacifiableColor<ColorOKLABa>,
    AlgebraicColor<ColorOKLABa> {

    companion object {
        fun fromRGBa(rgba: ColorRGBa): ColorOKLABa {
            // based on https://bottosson.github.io/posts/oklab/
            val c = rgba.toLinear()
            val l = 0.4122214708 * c.r + 0.5363325363 * c.g + 0.0514459929f * c.b
            val m = 0.2119034982 * c.r + 0.6806995451 * c.g + 0.1073969566f * c.b
            val s = 0.0883024619 * c.r + 0.2817188376 * c.g + 0.6299787005f * c.b

            val lnl = l.pow(1.0 / 3.0)
            val mnl = m.pow(1.0 / 3.0)
            val snl = s.pow(1.0 / 3.0)

            val L = 0.2104542553f * lnl + 0.7936177850f * mnl - 0.0040720468f * snl
            val a = 1.9779984951f * lnl - 2.4285922050f * mnl + 0.4505937099f * snl
            val b = 0.0259040371f * lnl + 0.7827717662f * mnl - 0.8086757660f * snl

            return ColorOKLABa(L, a, b, alpha = c.a)
        }
    }

    override fun toRGBa(): ColorRGBa {
        // based on https://bottosson.github.io/posts/oklab/
        val lnl = l + 0.3963377774 * a + 0.2158037573 * b
        val mnl = l - 0.1055613458 * a - 0.0638541728 * b
        val snl = l - 0.0894841775 * a - 1.2914855480 * b

        val l = lnl * lnl * lnl
        val m = mnl * mnl * mnl
        val s = snl * snl * snl

        return ColorRGBa(
            4.0767416621 * l - 3.3077115913 * m + 0.2309699292f * s,
            -1.2684380046 * l + 2.6097574011 * m - 0.3413193965f * s,
            -0.0041960863 * l - 0.7034186147 * m + 1.7076147010f * s,
            alpha, linearity = Linearity.LINEAR
        )
    }

    fun toOKLCHa() = ColorOKLCHa.fromColorOKLABa(this)

    override fun shade(factor: Double) = ColorOKLABa(l * factor, a, b, alpha)
    override fun opacify(factor: Double) = ColorOKLABa(l, a, b, alpha * factor)
    override fun minus(right: ColorOKLABa) = ColorOKLABa(l - right.l, a - right.a, b - right.b, alpha - right.alpha)
    override fun plus(right: ColorOKLABa) = ColorOKLABa(l + right.l, a + right.a, b + right.b, alpha + right.alpha)
    override fun times(scale: Double) = ColorOKLABa(l * scale, a * scale, b * scale, alpha * scale)
}

fun ColorRGBa.toOKLABa() = ColorOKLABa.fromRGBa(this)