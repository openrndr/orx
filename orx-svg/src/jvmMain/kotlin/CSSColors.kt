package org.openrndr.extra.svg

import org.openrndr.color.ColorRGBa
import org.openrndr.color.Linearity
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The color functions of CSS Color 4 and 5 that are not rgb() and hsl(): hwb(), lab(), lch(), oklab(), oklch(),
 * color() with the predefined color spaces, color-mix() and light-dark(). Colors are converted through CIE XYZ, as in
 * the specification, and clipped to sRGB.
 */
internal object CSSColors {
    /** a 3×3 matrix, row by row */
    private class M3(val m: DoubleArray) {
        operator fun times(v: DoubleArray) = DoubleArray(3) { i -> m[i * 3] * v[0] + m[i * 3 + 1] * v[1] + m[i * 3 + 2] * v[2] }
        operator fun times(o: M3) = M3(DoubleArray(9) { k ->
            val i = k / 3
            val j = k % 3
            m[i * 3] * o.m[j] + m[i * 3 + 1] * o.m[3 + j] + m[i * 3 + 2] * o.m[6 + j]
        })

        fun inverse(): M3 {
            val (a, b, c, d, e) = m.toList()
            val f = m[5]
            val g = m[6]
            val h = m[7]
            val i = m[8]
            val det = a * (e * i - f * h) - b * (d * i - f * g) + c * (d * h - e * g)
            return M3(doubleArrayOf(
                (e * i - f * h) / det, (c * h - b * i) / det, (b * f - c * e) / det,
                (f * g - d * i) / det, (a * i - c * g) / det, (c * d - a * f) / det,
                (d * h - e * g) / det, (b * g - a * h) / det, (a * e - b * d) / det
            ))
        }
    }

    private fun xyz(x: Double, y: Double) = doubleArrayOf(x / y, 1.0, (1.0 - x - y) / y)
    private val d65 = xyz(0.3127, 0.3290)
    private val d50 = xyz(0.3457, 0.3585)

    /** the matrix from linear RGB to XYZ of RGB primaries with chromaticities and a white point */
    private fun rgbToXyz(rx: Double, ry: Double, gx: Double, gy: Double, bx: Double, by: Double, white: DoubleArray): M3 {
        val r = xyz(rx, ry)
        val g = xyz(gx, gy)
        val b = xyz(bx, by)
        val p = M3(doubleArrayOf(r[0], g[0], b[0], r[1], g[1], b[1], r[2], g[2], b[2]))
        val s = p.inverse() * white
        return M3(doubleArrayOf(
            r[0] * s[0], g[0] * s[1], b[0] * s[2],
            r[1] * s[0], g[1] * s[1], b[1] * s[2],
            r[2] * s[0], g[2] * s[1], b[2] * s[2]
        ))
    }

    /** chromatic adaptation from one white point to another, with the Bradford transform */
    private fun bradford(from: DoubleArray, to: DoubleArray): M3 {
        val cone = M3(doubleArrayOf(0.8951, 0.2664, -0.1614, -0.7502, 1.7135, 0.0367, 0.0389, -0.0685, 1.0296))
        val f = cone * from
        val t = cone * to
        val scale = M3(doubleArrayOf(t[0] / f[0], 0.0, 0.0, 0.0, t[1] / f[1], 0.0, 0.0, 0.0, t[2] / f[2]))
        return cone.inverse() * scale * cone
    }

    private val d50ToD65 = bradford(d50, d65)
    private val d65ToD50 = bradford(d65, d50)

    private val srgbToXyz = rgbToXyz(0.64, 0.33, 0.30, 0.60, 0.15, 0.06, d65)
    private val xyzToSrgb = srgbToXyz.inverse()

    /** an RGB color space of CSS: the matrix to XYZ (D65) and its transfer function */
    private class RgbSpace(val toXyz: M3, val toLinear: (Double) -> Double, val fromLinear: (Double) -> Double) {
        val fromXyz = toXyz.inverse()
    }

    private fun srgbToLinear(c: Double): Double = sign(c) * if (abs(c) <= 0.04045) abs(c) / 12.92 else ((abs(c) + 0.055) / 1.055).pow(2.4)
    private fun srgbFromLinear(c: Double): Double = sign(c) * if (abs(c) <= 0.0031308) abs(c) * 12.92 else 1.055 * abs(c).pow(1.0 / 2.4) - 0.055

    private val spaces: Map<String, RgbSpace> by lazy {
        val identity: (Double) -> Double = { it }
        val proPhoto = rgbToXyz(0.734699, 0.265301, 0.159597, 0.840403, 0.036598, 0.000105, d50)
        val rec2020a = 1.09929682680944
        val rec2020b = 0.018053968510807
        mapOf(
            "srgb" to RgbSpace(srgbToXyz, ::srgbToLinear, ::srgbFromLinear),
            "srgb-linear" to RgbSpace(srgbToXyz, identity, identity),
            "display-p3" to RgbSpace(rgbToXyz(0.680, 0.320, 0.265, 0.690, 0.150, 0.060, d65), ::srgbToLinear, ::srgbFromLinear),
            "a98-rgb" to RgbSpace(rgbToXyz(0.64, 0.33, 0.21, 0.71, 0.15, 0.06, d65),
                { sign(it) * abs(it).pow(563.0 / 256.0) }, { sign(it) * abs(it).pow(256.0 / 563.0) }),
            "prophoto-rgb" to RgbSpace(d50ToD65 * proPhoto,
                { sign(it) * if (abs(it) <= 16.0 / 512.0) abs(it) / 16.0 else abs(it).pow(1.8) },
                { sign(it) * if (abs(it) >= 1.0 / 512.0) abs(it).pow(1.0 / 1.8) else 16.0 * abs(it) }),
            "rec2020" to RgbSpace(rgbToXyz(0.708, 0.292, 0.170, 0.797, 0.131, 0.046, d65),
                { sign(it) * if (abs(it) < rec2020b * 4.5) abs(it) / 4.5 else ((abs(it) + rec2020a - 1.0) / rec2020a).pow(1.0 / 0.45) },
                { sign(it) * if (abs(it) < rec2020b) 4.5 * abs(it) else rec2020a * abs(it).pow(0.45) - (rec2020a - 1.0) }),
            "xyz" to RgbSpace(M3(doubleArrayOf(1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0)), identity, identity),
            "xyz-d65" to RgbSpace(M3(doubleArrayOf(1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0)), identity, identity),
            "xyz-d50" to RgbSpace(d50ToD65, identity, identity)
        )
    }

    // OKLab, with the matrices of Björn Ottosson between linear sRGB and LMS
    private fun oklabToXyz(l: Double, a: Double, b: Double): DoubleArray {
        val l_ = l + 0.3963377774 * a + 0.2158037573 * b
        val m_ = l - 0.1055613458 * a - 0.0638541728 * b
        val s_ = l - 0.0894841775 * a - 1.2914855480 * b
        val lc = l_ * l_ * l_
        val mc = m_ * m_ * m_
        val sc = s_ * s_ * s_
        val rgb = doubleArrayOf(
            4.0767416621 * lc - 3.3077115913 * mc + 0.2309699292 * sc,
            -1.2684380046 * lc + 2.6097574011 * mc - 0.3413193965 * sc,
            -0.0041960863 * lc - 0.7034186147 * mc + 1.7076147010 * sc
        )
        return srgbToXyz * rgb
    }

    private fun xyzToOklab(xyz: DoubleArray): DoubleArray {
        val (r, g, b) = (xyzToSrgb * xyz).toList()
        val l = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
        val m = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
        val s = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
        return doubleArrayOf(
            0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
            1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
            0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
        )
    }

    // CIE Lab, relative to D50 as in CSS
    private const val KAPPA = 24389.0 / 27.0
    private const val EPSILON = 216.0 / 24389.0

    private fun labToXyz(l: Double, a: Double, b: Double): DoubleArray {
        val fy = (l + 16.0) / 116.0
        val fx = a / 500.0 + fy
        val fz = fy - b / 200.0
        val x = if (fx * fx * fx > EPSILON) fx * fx * fx else (116.0 * fx - 16.0) / KAPPA
        val y = if (l > KAPPA * EPSILON) fy * fy * fy else l / KAPPA
        val z = if (fz * fz * fz > EPSILON) fz * fz * fz else (116.0 * fz - 16.0) / KAPPA
        return d50ToD65 * doubleArrayOf(x * d50[0], y * d50[1], z * d50[2])
    }

    private fun xyzToLab(xyz: DoubleArray): DoubleArray {
        val v = d65ToD50 * xyz
        fun f(t: Double) = if (t > EPSILON) cbrt(t) else (KAPPA * t + 16.0) / 116.0
        val fx = f(v[0] / d50[0])
        val fy = f(v[1] / d50[1])
        val fz = f(v[2] / d50[2])
        return doubleArrayOf(116.0 * fy - 16.0, 500.0 * (fx - fy), 200.0 * (fy - fz))
    }

    private fun polar(l: Double, c: Double, h: Double): DoubleArray =
        doubleArrayOf(l, c * cos(h * PI / 180.0), c * sin(h * PI / 180.0))

    private fun cartesianToPolar(v: DoubleArray): DoubleArray {
        val c = sqrt(v[1] * v[1] + v[2] * v[2])
        val h = (atan2(v[2], v[1]) * 180.0 / PI).mod(360.0)
        return doubleArrayOf(v[0], c, h)
    }

    private fun hslToSrgb(h: Double, s: Double, l: Double): DoubleArray {
        fun f(n: Double): Double {
            val k = (n + h / 30.0).mod(12.0)
            val a = s * minOf(l, 1.0 - l)
            return l - a * maxOf(-1.0, minOf(k - 3.0, 9.0 - k, 1.0))
        }
        return doubleArrayOf(f(0.0), f(8.0), f(4.0))
    }

    private fun srgbToHsl(rgb: DoubleArray): DoubleArray {
        val (r, g, b) = rgb.toList()
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val l = (max + min) / 2.0
        val d = max - min
        if (d == 0.0) return doubleArrayOf(0.0, 0.0, l)
        val s = if (l == 0.0 || l == 1.0) 0.0 else (max - l) / minOf(l, 1.0 - l)
        val h = when (max) {
            r -> (g - b) / d + (if (g < b) 6.0 else 0.0)
            g -> (b - r) / d + 2.0
            else -> (r - g) / d + 4.0
        } * 60.0
        return doubleArrayOf(h, s, l)
    }

    private fun hwbToSrgb(h: Double, w: Double, b: Double): DoubleArray {
        if (w + b >= 1.0) return DoubleArray(3) { w / (w + b) }
        val rgb = hslToSrgb(h, 1.0, 0.5)
        return DoubleArray(3) { rgb[it] * (1.0 - w - b) + w }
    }

    private fun srgbToHwb(rgb: DoubleArray): DoubleArray {
        val h = srgbToHsl(rgb)[0]
        val w = minOf(rgb[0], rgb[1], rgb[2])
        val b = 1.0 - maxOf(rgb[0], rgb[1], rgb[2])
        return doubleArrayOf(h, w, b)
    }

    /** a color as XYZ (D65) and alpha */
    class Xyz(val xyz: DoubleArray, val alpha: Double)

    /** [color] in sRGB, clipped */
    fun toRgba(color: Xyz): ColorRGBa {
        val rgb = (xyzToSrgb * color.xyz).map { srgbFromLinear(it).coerceIn(0.0, 1.0) }
        return ColorRGBa(rgb[0], rgb[1], rgb[2], color.alpha.coerceIn(0.0, 1.0), linearity = Linearity.SRGB)
    }

    fun fromRgba(color: ColorRGBa): Xyz {
        val c = color.toSRGB()
        return Xyz(srgbToXyz * doubleArrayOf(srgbToLinear(c.r), srgbToLinear(c.g), srgbToLinear(c.b)), c.alpha)
    }

    /** the arguments of a function, split at commas or spaces, with the alpha after a slash, `none` is null */
    private class Arguments(val values: List<String?>, val alpha: String?)

    private fun arguments(text: String): Arguments {
        val (main, alpha) = text.split("/").let { it[0] to it.getOrNull(1)?.trim() }
        val values = main.split(Regex("[\\s,]+")).filter { it.isNotEmpty() }.map { if (it == "none") null else it }
        return Arguments(values, alpha?.takeIf { it != "none" })
    }

    /** a number, or a percentage where 100% is [full] */
    private fun number(value: String?, full: Double): Double? = when {
        value == null -> 0.0
        value.endsWith("%") -> value.dropLast(1).toDoubleOrNull()?.let { it / 100.0 * full }
        else -> value.toDoubleOrNull()
    }

    private fun hue(value: String?): Double? = if (value == null) 0.0 else SVGParse.angle(value)

    private fun alpha(value: String?): Double = number(value ?: "1", 1.0)?.coerceIn(0.0, 1.0) ?: 1.0

    /** the color of a CSS color function [name] with [text] between its parentheses, null when it is not valid */
    fun function(name: String, text: String, parse: (String) -> ColorRGBa?): ColorRGBa? {
        when (name) {
            "color-mix" -> return mix(text, parse)
            // light-dark() takes the light color, compositions do not have a dark color scheme
            "light-dark" -> return splitTopLevel(text).firstOrNull()?.let { parse(it.trim()) }
        }
        val a = arguments(text)
        val xyz = when (name) {
            "hwb" -> {
                if (a.values.size != 3) return null
                val rgb = hwbToSrgb(hue(a.values[0]) ?: return null, number(a.values[1], 1.0) ?: return null, number(a.values[2], 1.0) ?: return null)
                srgbToXyz * DoubleArray(3) { srgbToLinear(rgb[it]) }
            }
            "lab" -> {
                if (a.values.size != 3) return null
                labToXyz((number(a.values[0], 100.0) ?: return null).coerceAtLeast(0.0), number(a.values[1], 125.0) ?: return null, number(a.values[2], 125.0) ?: return null)
            }
            "lch" -> {
                if (a.values.size != 3) return null
                val p = polar((number(a.values[0], 100.0) ?: return null).coerceAtLeast(0.0), (number(a.values[1], 150.0) ?: return null).coerceAtLeast(0.0), hue(a.values[2]) ?: return null)
                labToXyz(p[0], p[1], p[2])
            }
            "oklab" -> {
                if (a.values.size != 3) return null
                oklabToXyz((number(a.values[0], 1.0) ?: return null).coerceAtLeast(0.0), number(a.values[1], 0.4) ?: return null, number(a.values[2], 0.4) ?: return null)
            }
            "oklch" -> {
                if (a.values.size != 3) return null
                val p = polar((number(a.values[0], 1.0) ?: return null).coerceAtLeast(0.0), (number(a.values[1], 0.4) ?: return null).coerceAtLeast(0.0), hue(a.values[2]) ?: return null)
                oklabToXyz(p[0], p[1], p[2])
            }
            "color" -> {
                val space = spaces[a.values.firstOrNull() ?: return null] ?: return null
                if (a.values.size != 4) return null
                val components = DoubleArray(3) { number(a.values[it + 1], 1.0) ?: return null }
                space.toXyz * DoubleArray(3) { space.toLinear(components[it]) }
            }
            else -> return null
        }
        return toRgba(Xyz(xyz, alpha(a.alpha)))
    }

    /** [text] split at commas that are not in parentheses */
    private fun splitTopLevel(text: String): List<String> {
        val parts = mutableListOf<String>()
        var depth = 0
        var start = 0
        for ((i, c) in text.withIndex()) {
            when (c) {
                '(' -> depth++
                ')' -> depth--
                ',' -> if (depth == 0) {
                    parts.add(text.substring(start, i)); start = i + 1
                }
            }
        }
        parts.add(text.substring(start))
        return parts
    }

    /** a color in the coordinates of an interpolation space, with the index of its hue component, or -1 */
    private class Coordinates(val values: DoubleArray, val hueIndex: Int)

    private fun toSpace(space: String, c: Xyz): Coordinates? = when (space) {
        "lab" -> Coordinates(xyzToLab(c.xyz), -1)
        "oklab" -> Coordinates(xyzToOklab(c.xyz), -1)
        "lch" -> Coordinates(cartesianToPolar(xyzToLab(c.xyz)), 2)
        "oklch" -> Coordinates(cartesianToPolar(xyzToOklab(c.xyz)), 2)
        "hsl" -> Coordinates(srgbToHsl((xyzToSrgb * c.xyz).map(::srgbFromLinear).toDoubleArray()), 0)
        "hwb" -> Coordinates(srgbToHwb((xyzToSrgb * c.xyz).map(::srgbFromLinear).toDoubleArray()), 0)
        else -> spaces[space]?.let { s -> Coordinates((s.fromXyz * c.xyz).map(s.fromLinear).toDoubleArray(), -1) }
    }

    private fun fromSpace(space: String, v: DoubleArray): DoubleArray? = when (space) {
        "lab" -> labToXyz(v[0], v[1], v[2])
        "oklab" -> oklabToXyz(v[0], v[1], v[2])
        "lch" -> polar(v[0], v[1], v[2]).let { labToXyz(it[0], it[1], it[2]) }
        "oklch" -> polar(v[0], v[1], v[2]).let { oklabToXyz(it[0], it[1], it[2]) }
        "hsl" -> hslToSrgb(v[0], v[1], v[2]).let { rgb -> srgbToXyz * DoubleArray(3) { srgbToLinear(rgb[it]) } }
        "hwb" -> hwbToSrgb(v[0], v[1], v[2]).let { rgb -> srgbToXyz * DoubleArray(3) { srgbToLinear(rgb[it]) } }
        else -> spaces[space]?.let { s -> s.toXyz * DoubleArray(3) { s.toLinear(v[it]) } }
    }

    /**
     * color-mix(in <space> [<hue> hue], <color> [<percentage>], <color> [<percentage>]): the colors interpolated
     * with premultiplied alpha, as in CSS Color 5
     */
    private fun mix(text: String, parse: (String) -> ColorRGBa?): ColorRGBa? {
        val parts = splitTopLevel(text).map { it.trim() }
        if (parts.size != 3) return null
        val method = Regex("^in\\s+([a-z0-9-]+)(?:\\s+(shorter|longer|increasing|decreasing)\\s+hue)?$").matchEntire(parts[0]) ?: return null
        val space = method.groupValues[1]
        val hueMethod = method.groupValues[2].ifEmpty { "shorter" }
        fun colorAndPercentage(part: String): Pair<ColorRGBa, Double?>? {
            val percentage = Regex("(-?[0-9.]+)%").findAll(part).lastOrNull()?.takeIf { m ->
                // the percentage is outside of the parentheses of the color
                part.substring(m.range.last + 1).count { it == ')' } == part.substring(m.range.last + 1).count { it == '(' } &&
                        part.substring(0, m.range.first).count { it == '(' } == part.substring(0, m.range.first).count { it == ')' }
            }
            val colorText = if (percentage == null) part else part.removeRange(percentage.range).trim()
            val color = parse(colorText) ?: return null
            return color to percentage?.groupValues?.get(1)?.toDoubleOrNull()?.div(100.0)
        }
        val (c1, p1) = colorAndPercentage(parts[1]) ?: return null
        val (c2, p2) = colorAndPercentage(parts[2]) ?: return null
        // the percentages are normalized to add up to one, the sum below one scales the alpha
        val (w1, w2) = when {
            p1 == null && p2 == null -> 0.5 to 0.5
            p1 == null -> (1.0 - p2!!) to p2
            p2 == null -> p1 to (1.0 - p1)
            else -> p1 to p2
        }
        val sum = w1 + w2
        if (sum <= 0.0) return null
        val t = w2 / sum
        val a = toSpace(space, fromRgba(c1)) ?: return null
        val b = toSpace(space, fromRgba(c2)) ?: return null
        val alpha1 = c1.alpha
        val alpha2 = c2.alpha
        val alpha = alpha1 * (1.0 - t) + alpha2 * t
        val result = DoubleArray(3) { i ->
            if (i == a.hueIndex) {
                var h1 = a.values[i]
                var h2 = b.values[i]
                val d = h2 - h1
                when (hueMethod) {
                    "shorter" -> if (d > 180.0) h1 += 360.0 else if (d < -180.0) h2 += 360.0
                    "longer" -> if (d in 0.0..180.0 && d > 0.0) h1 += 360.0 else if (d in -180.0..0.0 && d < 0.0) h2 += 360.0
                    "increasing" -> if (d < 0.0) h2 += 360.0
                    "decreasing" -> if (d > 0.0) h1 += 360.0
                }
                (h1 * (1.0 - t) + h2 * t).mod(360.0)
            } else {
                // premultiplied interpolation
                val v = a.values[i] * alpha1 * (1.0 - t) + b.values[i] * alpha2 * t
                if (alpha > 0.0) v / alpha else 0.0
            }
        }
        val xyz = fromSpace(space, result) ?: return null
        return toRgba(Xyz(xyz, alpha * minOf(sum, 1.0)))
    }
}
