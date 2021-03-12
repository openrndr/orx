package org.openrndr.extras.color.spaces

import org.openrndr.color.*
import org.openrndr.math.mixAngle
import java.util.*
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

private val m = arrayOf(
        doubleArrayOf(3.240969941904521, -1.537383177570093, -0.498610760293),
        doubleArrayOf(-0.96924363628087, 1.87596750150772, 0.041555057407175),
        doubleArrayOf(0.055630079696993, -0.20397695888897, 1.056971514242878))

private val kappa = 903.2962962
private val epsilon = 0.0088564516

private fun getBounds(L: Double): List<DoubleArray>? {
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
    val length = line[1] / (Math.sin(theta) - line[0] * Math.cos(theta))
    return Length(length)
}

private class Length(val length: Double) {
    val greaterEqualZero: Boolean = length >= 0
}

private fun maxSafeChromaForL(L100: Double): Double {
    val bounds = getBounds(L100)
    var min = Double.MAX_VALUE
    for (i in 0..1) {
        val m1 = bounds!![i][0]
        val b1 = bounds[i][1]
        val line = doubleArrayOf(m1, b1)
        val x = intersectLineLine(line, doubleArrayOf(-1 / m1, 0.0))
        val length = distanceFromPole(doubleArrayOf(x, b1 + x * m1))
        min = min(min, length)
    }
    return min
}

fun maxChromaForLH(L100: Double, H: Double): Double {
    val hrad = H / 360 * Math.PI * 2
    val bounds = getBounds(L100)
    var min = Double.MAX_VALUE
    for (bound in bounds!!) {
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
data class ColorHSLUVa(val h: Double, val s: Double, val l: Double, val a: Double = 1.0) :
        ConvertibleToColorRGBa,
        HueShiftableColor<ColorHSLUVa>,
        SaturatableColor<ColorHSLUVa>,
        ShadableColor<ColorHSLUVa>,
        OpacifiableColor<ColorHSLUVa>,
        AlgebraicColor<ColorHSLUVa> {

    fun toLCHUVa(): ColorLCHUVa {

        val l100 = l * 100.0
        val s100 = s * 100.0

        if (l100 > 99.9999999) {
            ColorLCHUVa(100.0, 0.0, h, a)
        }

        if (l100 < 0.00000001) {
            ColorLCHUVa(0.0, 0.0, h, a)
        }
        val max100 = maxChromaForLH(l100, h)

        val c: Double = max100 / 100 * s100

        return ColorLCHUVa(l100, c, h, a)
    }

    fun toXSLUVa() : ColorXSLUVa {
        return ColorXSLUVa(hueToX(h), s, l, a)
    }

    override fun shiftHue(shiftInDegrees: Double) = copy(h = h + (shiftInDegrees))

    override fun shade(factor: Double) = copy(l = l * factor)

    override fun saturate(factor: Double) = copy(s = s * factor)

    override fun toRGBa(): ColorRGBa {
        return toLCHUVa().toRGBa()
    }

    override fun opacify(factor: Double) = copy(a = a * factor)

    override fun minus(other: ColorHSLUVa) = copy(h = h - other.h, s = s - other.s, l = l - other.l, a = a - other.a)

    override fun plus(other: ColorHSLUVa) = copy(h = h + other.h, s = s + other.s, l = l + other.l, a = a + other.a)

    override fun times(factor: Double) = copy(h = h * factor, s = s * factor, l = l * factor, a = a * factor)

    override fun mix(other: ColorHSLUVa, factor: Double) = mix(this, other, factor)

}

fun mix(left: ColorHSLUVa, right: ColorHSLUVa, x: Double): ColorHSLUVa {
    val sx = x.coerceIn(0.0, 1.0)
    return ColorHSLUVa(
            mixAngle(left.h, right.h, sx),
            (1.0 - sx) * left.s + sx * right.s,
            (1.0 - sx) * left.l + sx * right.l,
            (1.0 - sx) * left.a + sx * right.a)
}

data class ColorXSLUVa(val x: Double, val s: Double, val l: Double, val a: Double):
        ConvertibleToColorRGBa,
        HueShiftableColor<ColorXSLUVa>,
        SaturatableColor<ColorXSLUVa>,
        ShadableColor<ColorXSLUVa>,
        OpacifiableColor<ColorXSLUVa>,
        AlgebraicColor<ColorXSLUVa> {
    override fun shiftHue(shiftInDegrees: Double) = copy(x = x + (shiftInDegrees))

    override fun shade(factor: Double) = copy(l = l * factor)

    override fun saturate(factor: Double) = copy(s = s * factor)

    override fun toRGBa(): ColorRGBa {
        return toHSLUVa().toRGBa()
    }

    fun toHSLUVa(): ColorHSLUVa = ColorHSLUVa(xToHue(x), s, l, a)

    override fun opacify(factor: Double) = copy(a = a * factor)

    override fun minus(other: ColorXSLUVa) = copy(x = x - other.x, s = s - other.s, l = l - other.l, a = a - other.a)

    override fun plus(other: ColorXSLUVa) = copy(x = x + other.x, s = s + other.s, l = l + other.l, a = a + other.a)

    override fun times(factor: Double) = copy(x = x * factor, s = s * factor, l = l * factor, a = a * factor)

    override fun mix(other: ColorXSLUVa, factor: Double) = mix(this, other, factor)
}

fun ColorRGBa.toXSLUVa() = toHSLUVa().toXSLUVa()

fun mix(left: ColorXSLUVa, right: ColorXSLUVa, x: Double): ColorXSLUVa {
    val sx = x.coerceIn(0.0, 1.0)
    return ColorXSLUVa(
            mixAngle(left.x, right.x, sx),
            (1.0 - sx) * left.s + sx * right.s,
            (1.0 - sx) * left.l + sx * right.l,
            (1.0 - sx) * left.a + sx * right.a)
}

private fun map(x: Double, a: Double, b: Double, c: Double, d: Double): Double {
    return ((x - a) / (b - a)) * (d - c) + c
}

private fun hueToX(hue:Double): Double {
    val h = ((hue % 360.0) + 360.0) % 360.0
    return if (0 <= h && h < 35) {
        map(h, 0.0, 35.0, 0.0, 60.0)
    } else if (35 <= h && h < 60) {
        map(h, 35.0, 60.0, 60.0, 120.0)
    } else if (60 <= h && h < 135.0) {
        map(h, 60.0, 135.0, 120.0, 180.0)
    } else if (135.0 <= h && h < 225.0) {
        map(h, 135.0, 225.0, 180.0, 240.0)
    } else if (225.0 <= h && h < 275.0) {
        map(h, 225.0, 275.0, 240.0, 300.0)
    } else {
        map(h, 276.0, 360.0, 300.0, 360.0)
    }
}

private fun xToHue(x:Double) : Double {
    val x = x % 360.0
    return if (0.0 <= x && x < 60.0) {
        map(x, 0.0, 60.0, 0.0, 35.0)
    } else if (60.0 <= x && x < 120.0) {
        map(x, 60.0, 120.0, 35.0, 60.0)
    } else if (120.0 <= x && x < 180.0) {
        map(x, 120.0, 180.0, 60.0, 135.0)
    } else if (180.0 <= x && x < 240.0) {
        map(x, 180.0, 240.0, 135.0, 225.0)
    } else if (240.0 <= x && x < 300.0) {
        map(x, 240.0, 300.0, 225.0, 275.0)
    } else {
        map(x, 300.0, 360.0, 276.0, 360.0)
    }
}

data class ColorHPLUVa(val h: Double, val s: Double, val l: Double, val a: Double = 1.0) :
        ConvertibleToColorRGBa,
        HueShiftableColor<ColorHPLUVa>,
        SaturatableColor<ColorHPLUVa>,
        ShadableColor<ColorHPLUVa>,
        OpacifiableColor<ColorHPLUVa>,
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

    override fun shiftHue(shiftInDegrees: Double): ColorHPLUVa {
        return copy(h = h + (shiftInDegrees))
    }

    override fun shade(factor: Double): ColorHPLUVa = copy(l = l * factor)

    override fun saturate(factor: Double): ColorHPLUVa = copy(s = s * factor)

    override fun toRGBa(): ColorRGBa = toLCHUVa().toRGBa()

    override fun opacify(factor: Double) = copy(a = a * factor)

    override fun minus(other: ColorHPLUVa) = copy(h = h - other.h, s = s - other.s, l = l - other.l, a = a - other.a)

    override fun plus(other: ColorHPLUVa) = copy(h = h + other.h, s = s + other.s, l = l + other.l, a = a + other.a)

    override fun times(factor: Double) = copy(h = h * factor, s = s * factor, l = l * factor, a = a * factor)

    override fun mix(other: ColorHPLUVa, factor: Double) = mix(this, other, factor)

}

fun mix(left: ColorHPLUVa, right: ColorHPLUVa, x: Double): ColorHPLUVa {
    val sx = x.coerceIn(0.0, 1.0)
    return ColorHPLUVa(
            mixAngle(left.h, right.h, sx),
            (1.0 - sx) * left.s + sx * right.s,
            (1.0 - sx) * left.l + sx * right.l,
            (1.0 - sx) * left.a + sx * right.a)
}


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
fun ColorRGBa.toHPLUVa(): ColorHPLUVa = toLCHUVa().toHPLUVa()