package org.openrndr.extra.color.spaces

import kotlinx.serialization.Serializable
import org.openrndr.color.*
import org.openrndr.math.Vector4
import org.openrndr.math.map
import org.openrndr.math.mixAngle
import kotlin.math.*

private val m = arrayOf(
        doubleArrayOf(3.240969941904521, -1.537383177570093, -0.498610760293),
        doubleArrayOf(-0.96924363628087, 1.87596750150772, 0.041555057407175),
        doubleArrayOf(0.055630079696993, -0.20397695888897, 1.056971514242878))

private const val kappa = 903.2962962
private const val epsilon = 0.0088564516

private fun getBounds(L: Double): List<DoubleArray> {
    val result = ArrayList<DoubleArray>()
    val sub1 = (L + 16).pow(3.0) / 1560896
    val sub2 = if (sub1 > epsilon) sub1 else L / kappa
    for (c in 0..2) {
        val m1 = m[c][0]
        val m2 = m[c][1]
        val m3 = m[c][2]
        for (t in 0..1) {
            val top1 = (284517 * m1 - 94839 * m3) * sub2
            val top2 = (838422 * m3 + 769860 * m2 + 731718 * m1) * L * sub2 - 769860 * t * L
            val bottom = (632260 * m3 - 126452 * m2) * sub2 + 126452 * t
            result.add(doubleArrayOf(top1 / bottom, top2 / bottom))
        }
    }
    return result
}

private fun intersectLineLine(lineA: DoubleArray, lineB: DoubleArray): Double {
    return (lineA[1] - lineB[1]) / (lineB[0] - lineA[0])
}

private fun distanceFromPole(point: DoubleArray): Double {
    return sqrt(point[0].pow(2.0) + point[1].pow(2.0))
}

private fun lengthOfRayUntilIntersect(theta: Double, line: DoubleArray): Length {
    val length = line[1] / (sin(theta) - line[0] * cos(theta))
    return Length(length)
}

private class Length(val length: Double) {
    val greaterEqualZero: Boolean = length >= 0
}

internal fun maxSafeChromaForL(L100: Double): Double {
    val bounds = getBounds(L100)
    var min = Double.MAX_VALUE
    for (i in 0..1) {
        val m1 = bounds[i][0]
        val b1 = bounds[i][1]
        val line = doubleArrayOf(m1, b1)
        val x = intersectLineLine(line, doubleArrayOf(-1 / m1, 0.0))
        val length = distanceFromPole(doubleArrayOf(x, b1 + x * m1))
        min = min(min, length)
    }
    return min
}

private fun maxChromaForLH(L100: Double, H: Double): Double {
    val hrad = H / 360 * PI * 2
    val bounds = getBounds(L100)
    var min = Double.MAX_VALUE
    for (bound in bounds) {
        val length: Length = lengthOfRayUntilIntersect(hrad, bound)
        if (length.greaterEqualZero) {
            min = min(min, length.length)
        }
    }
    return min
}

/**
 * HSLUV color space
 */
@Serializable
data class ColorHSLUVa(val h: Double, val s: Double, val l: Double, override val alpha: Double = 1.0) :
        ColorModel<ColorHSLUVa>,
        HueShiftableColor<ColorHSLUVa>,
        SaturatableColor<ColorHSLUVa>,
        ShadableColor<ColorHSLUVa>,
        LuminosityColor<ColorHSLUVa>,
        AlgebraicColor<ColorHSLUVa> {

    @Deprecated("Legacy alpha parameter name", ReplaceWith("alpha"))
    val a = alpha

    fun toLCHUVa(): ColorLCHUVa {

        val l100 = l * 100.0
        val s100 = s * 100.0

        if (l100 > 99.9999999) {
            ColorLCHUVa(100.0, 0.0, h, alpha)
        }

        if (l100 < 0.00000001) {
            ColorLCHUVa(0.0, 0.0, h, alpha)
        }
        val max100 = maxChromaForLH(l100, h)

        val c: Double = max100 / 100 * s100

        return ColorLCHUVa(l100, c, h, alpha)
    }

    fun toXSLUVa() : ColorXSLUVa {
        return ColorXSLUVa(hueToX(h), s, l, alpha)
    }

    override val hue: Double
        get() = h

    override fun withHue(hue: Double): ColorHSLUVa = copy(h = hue)
    override fun shade(factor: Double) = copy(l = l * factor)
    override val saturation: Double
        get() = s

    override fun withSaturation(saturation: Double): ColorHSLUVa = copy(s = saturation)

    override fun toRGBa(): ColorRGBa {
        return toLCHUVa().toRGBa()
    }

    override fun opacify(factor: Double) = copy(alpha = alpha * factor)

    override fun minus(right: ColorHSLUVa) = copy(h = h - right.h, s = s - right.s, l = l - right.l, alpha = alpha - right.alpha)

    override fun plus(right: ColorHSLUVa) = copy(h = h + right.h, s = s + right.s, l = l + right.l, alpha = alpha + right.alpha)

    override fun times(scale: Double) = copy(h = h * scale, s = s * scale, l = l * scale, alpha = alpha * scale)

    override fun mix(other: ColorHSLUVa, factor: Double) = mix(this, other, factor)

    override fun toVector4(): Vector4 = Vector4(h, s, l, alpha)
    override val luminosity: Double
        get() = l

    override fun withLuminosity(luminosity: Double): ColorHSLUVa = copy(l = luminosity)

}

fun mix(left: ColorHSLUVa, right: ColorHSLUVa, x: Double): ColorHSLUVa {
    val sx = x.coerceIn(0.0, 1.0)
    return ColorHSLUVa(
            mixAngle(left.h, right.h, sx),
            (1.0 - sx) * left.s + sx * right.s,
            (1.0 - sx) * left.l + sx * right.l,
            (1.0 - sx) * left.alpha + sx * right.alpha)
}

internal fun map(x: Double, a: Double, b: Double, c: Double, d: Double): Double {
    return ((x - a) / (b - a)) * (d - c) + c
}

fun hueToX(hue:Double): Double {
    val h = hue.mod(360.0)
    return if (0 <= h && h < 35) {
        h.map(0.0, 35.0, 0.0, 60.0)
    } else if (35 <= h && h < 60) {
        h.map(35.0, 60.0, 60.0, 120.0)
    } else if (60 <= h && h < 135.0) {
        h.map(60.0, 135.0, 120.0, 180.0)
    } else if (135.0 <= h && h < 225.0) {
        h.map(135.0, 225.0, 180.0, 240.0)
    } else if (225.0 <= h && h < 275.0) {
        h.map( 225.0, 275.0, 240.0, 300.0)
    } else {
        h.map( 275.0, 360.0, 300.0, 360.0)
    }
}

fun ColorLCHUVa.toHSLUVa(): ColorHSLUVa {
    val l100 = l

    if (l100 > 99.99999) {
        return ColorHSLUVa(h, 0.0, 1.0)
    }
    if (l < 0.000001) {
        return ColorHSLUVa(h, 0.0, 0.0)
    }
    val max100 = maxChromaForLH(l100, h)
    val c100 = c
    val s1 = c100 / max100
    return ColorHSLUVa(h, s1, l100 / 100.0, alpha)
}

fun ColorRGBa.toHSLUVa(): ColorHSLUVa = toLCHUVa().toHSLUVa()
