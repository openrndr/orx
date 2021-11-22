package org.openrndr.extra.color.fettepalette

import org.openrndr.color.ColorHSLa
import org.openrndr.color.ColorHSVa
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.color.spaces.ColorOKHSLa
import org.openrndr.extra.color.spaces.ColorOKHSVa
import org.openrndr.extra.color.spaces.toOKHSLa
import org.openrndr.extra.parameters.*
import org.openrndr.math.Vector2
import org.openrndr.math.clamp
import org.openrndr.math.mod_
import kotlin.math.*

/*
Converted to Kotlin from https://github.com/meodai/fettepalette/blob/main/src/index.ts

MIT License

Copyright (c) 2021 David Aerne

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

interface Curve {
    fun pointOnCurve(
        i: Double,
        total: Double,
        curveAccent: Double,
        min: Vector2 = Vector2.ZERO,
        max: Vector2 = Vector2.ZERO
    ): Vector2

    fun remap(v: Vector2, min: Vector2, max: Vector2): Vector2 {
        var x = v.x
        var y = v.y

        x = min.x + x.coerceIn(0.0, 1.0) * (max.x - min.x)
        y = min.y + y.coerceIn(0.0, 1.0) * (max.y - min.y)

        return Vector2(x, y)
    }
}

object Lamé : Curve {
    override fun pointOnCurve(i: Double, total: Double, curveAccent: Double, min: Vector2, max: Vector2): Vector2 {
        val limit = PI / 2
        val percentile = i / total
        val t = percentile * limit
        val exp = 2 / (2 + 20 * curveAccent)
        val cosT = cos(t)
        val sinT = sin(t)
        val x = sign(cosT) * abs(cosT).pow(exp)
        val y = sign(sinT) * abs(sinT).pow(exp)
        return remap(Vector2(x, y), min, max)
    }
}

object Arc : Curve {
    override fun pointOnCurve(i: Double, total: Double, curveAccent: Double, min: Vector2, max: Vector2): Vector2 {
        val limit = PI / 2
        val percentile = i / total
        val t = percentile * limit
        val slice = limit / total
        val y = cos(-PI / 2 + i * slice + curveAccent)
        val x = sin(PI / 2 + i * slice - curveAccent)
        return remap(Vector2(x, y), min, max)
    }
}

class ColorRamp(val baseColors: List<ColorRGBa>, val darkColors: List<ColorRGBa>, val lightColors: List<ColorRGBa>)

@Description("Color ramp parameters")
class ColorRampParameters {
    @IntParameter("total", 3, 16, 0)
    var total = 3

    @DoubleParameter("center hue", -180.0, 180.0, 3, 1)
    var centerHue = 0.0

    @DoubleParameter("hue cycle", 0.0, 1.0, 3, 2)
    var hueCycle = 0.3

    @DoubleParameter("offset tint", 0.0, 1.0, 3, 3)
    var offsetTint = 0.1

    @DoubleParameter("offset shade", 0.0, 1.0, 3, 4)
    var offsetShade = 0.1

    @DoubleParameter("curve accent", 0.0, 1.0, 3, 5)
    var curveAccent = 0.0

    @DoubleParameter("tint shade hue shift", 0.0, 1.0, 3, 6)
    var tintShadeHueShift = 0.1

    @DoubleParameter("offset mod tint", 0.0, 1.0, 3, 7)
    var offsetCurveModTint = 0.03

    @DoubleParameter("offset mod shade", 0.0, 1.0, 3, 8)
    var offsetCurveModShade = 0.03

    @Vector2Parameter("min saturation/light", min = 0.0, max = 1.0, precision = 3, order = 9)
    var minSaturationLight = Vector2.ZERO

    @Vector2Parameter("max saturation/light", min = 0.0, max = 1.0, precision = 3, order = 10)
    var maxSaturationLight = Vector2.ONE

    @BooleanParameter("use OKHSV", order = 11)
    var useOK = false
}


/**
 * @param total total of base colors in the ramp
 * @param centerHue at what hue should the generation start at
 * @param hueCycle hsl spins how much should the hue change over the curve, 0: not at all, 1: one full rainbow
 * @param offsetTint offset for the tints
 * @param offsetShade offset of the shades
 * @param curveAccent how accentuated is the curve (depends heavily on curveMethod)
 * @param tintShadeHueShift defines how shifted the hue is for the shades and the tints
 * @param offsetCurveModTint modifies the tint curve
 * @param offsetCurveModShade modifies the shade curve
 * @param minSaturationLight defines the min saturation and light of all the colors
 * @param maxSaturationLight defines the max saturation and light of all the colors
 * @param useOK use OKHSV and OKHSL spaces
 */
fun generateColorRamp(
    total: Int = 3,
    centerHue: Double = 0.0,
    hueCycle: Double = 0.3,
    offsetTint: Double = 0.1,
    offsetShade: Double = 0.1,
    curveAccent: Double = 0.0,
    tintShadeHueShift: Double = 0.1,
    curveMethod: Curve = Lamé,
    offsetCurveModTint: Double = 0.03,
    offsetCurveModShade: Double = 0.03,
    minSaturationLight: Vector2 = Vector2.ZERO,
    maxSaturationLight: Vector2 = Vector2.ONE,
    useOK: Boolean = false,
): ColorRamp {
    val baseColors = mutableListOf<ColorRGBa>()
    val lightColors = mutableListOf<ColorRGBa>()
    val darkColors = mutableListOf<ColorRGBa>()

    val okHueAdjust = if (useOK) 30.0 else 0.0

    for (i in 1 until total + 1) {
        val (x, y) = curveMethod.pointOnCurve(
            i.toDouble(),
            total + 1.0,
            curveAccent,
            minSaturationLight,
            maxSaturationLight
        )
        val h = (okHueAdjust + 360.0 +
                (-180.0 * hueCycle + (centerHue + i * (360 / (total + 1)) * hueCycle))
                ) % 360

        val hsv = if (useOK) {
            ColorOKHSVa(h, x, y)
        } else ColorHSVa(h, x, y)
        val hsl = if (useOK) {
            hsv.toRGBa().toOKHSLa()
        } else hsv.toRGBa().toHSLa()
        baseColors.add(hsl.toRGBa().toSRGB())

        val (xl, yl) = curveMethod.pointOnCurve(
            i.toDouble(), total + 1.0, curveAccent + offsetCurveModTint,
            minSaturationLight,
            maxSaturationLight
        )

        val hslLight = if (useOK) ColorOKHSVa(h, xl, yl).toRGBa().toOKHSLa() else ColorHSVa(h, xl, yl).toRGBa().toHSLa()

        if (useOK) {
            hslLight as ColorOKHSLa
            lightColors.add(
                ColorOKHSLa(
                    (hslLight.h + 360.0 * tintShadeHueShift).mod_(360.0),
                    (hslLight.s - offsetTint).clamp(0.0, 1.0),
                    (hslLight.l + offsetTint).clamp(0.0, 1.0)
                ).toRGBa().toSRGB()
            )
        } else {
            hslLight as ColorHSLa
            lightColors.add(
                ColorHSLa(
                    (hslLight.h + 360.0 * tintShadeHueShift).mod_(360.0),
                    (hslLight.s - offsetTint).clamp(0.0, 1.0),
                    (hslLight.l + offsetTint).clamp(0.0, 1.0)
                ).toRGBa().toSRGB()
            )
        }

        val (xd, yd) = curveMethod.pointOnCurve(
            i.toDouble(), total + 1.0, curveAccent - offsetCurveModShade,
            minSaturationLight,
            maxSaturationLight
        )

        val hslDark = if (useOK) ColorOKHSVa(h, xd, yd).toRGBa().toOKHSLa() else ColorHSVa(h, xd, yd).toRGBa().toHSLa()

        if (useOK) {
            hslDark as ColorOKHSLa
            darkColors.add(
                ColorOKHSLa(
                    (hslDark.h - 360.0 * tintShadeHueShift).mod_(360.0),
                    (hslDark.s - offsetShade).clamp(0.0, 1.0),
                    (hslDark.l - offsetShade).clamp(0.0, 1.0)
                ).toRGBa().toSRGB()
            )
        } else {
            hslDark as ColorHSLa
            darkColors.add(
                ColorHSLa(
                    (hslDark.h - 360.0 * tintShadeHueShift).mod_(360.0),
                    (hslDark.s - offsetShade).clamp(0.0, 1.0),
                    (hslDark.l - offsetShade).clamp(0.0, 1.0)
                ).toRGBa().toSRGB()
            )
        }
    }
    return ColorRamp(baseColors, darkColors, lightColors)
}

fun generateColorRamp(parameters: ColorRampParameters)
    = generateColorRamp(total = parameters.total,
    centerHue = parameters.centerHue,
    hueCycle = parameters.hueCycle,
    offsetTint = parameters.offsetTint,
    offsetShade = parameters.offsetShade,
    curveAccent = parameters.curveAccent,
    tintShadeHueShift = parameters.tintShadeHueShift,
    curveMethod = Lamé,
    offsetCurveModTint = parameters.offsetCurveModTint,
    offsetCurveModShade = parameters.offsetCurveModShade,
    minSaturationLight = parameters.minSaturationLight,
    maxSaturationLight = parameters.maxSaturationLight,
    useOK = parameters.useOK
)