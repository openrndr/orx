package org.openrndr.extra.color.palettes

import org.openrndr.color.ColorModel
import org.openrndr.color.ColorRGBa
import org.openrndr.color.HueShiftableColor
import org.openrndr.extra.color.tools.shiftHue

/**
 * Generate an analogous palette
 * @param T the color model to use
 */
inline fun <reified T> ColorRGBa.analogous(hueShift: Double, steps: Int = 5): List<ColorRGBa>
        where T : HueShiftableColor<T>,
              T : ColorModel<T> = (0 until steps).map {
    shiftHue<T>(hueShift * it / (steps - 1.0))
}

/**
 * Generate a split complementary palette in which the receiver is the seed color
 * @param T the color model to use
 * @param splitFactor a value between 0 and 1 that indicates how much the complementary color should be split
 * @param double should a double complementary palette be generated
 */
inline fun <reified T> ColorRGBa.splitComplementary(splitFactor: Double, double: Boolean = false): List<ColorRGBa>
        where T : HueShiftableColor<T>,
              T : ColorModel<T> {
    val c0 = shiftHue<T>(180 - 180.0 * splitFactor)
    val c1 = shiftHue<T>(180 + 180.0 * splitFactor)

    if (!double) {
        return listOf(this, c0, c1)
    } else {
        val c2 = shiftHue<T>(180.0 * splitFactor)
        val c3 = shiftHue<T>(-180.0 * splitFactor)
        return listOf(this, c0, c1, c2, c3)
    }
}

/**
 * Generate a triadic palette in which the receiver is the seed color.
 * @param T the color model to use
 */
inline fun <reified T> ColorRGBa.triadic(): List<ColorRGBa>
        where T : HueShiftableColor<T>,
              T : ColorModel<T> = splitComplementary<T>(1.0 / 3.0)


/**
 * Generate a tetradic palette in which the receiver is the seed color.
 * @param T the color model to use
 * @param aspectRatio the aspect ratio between even and odd sides
 */
inline fun <reified T> ColorRGBa.tetradic(aspectRatio: Double = 1.0): List<ColorRGBa>
        where T : HueShiftableColor<T>,
              T : ColorModel<T> {
    val a0 = 180 / (aspectRatio + 1)
    val a1 = 180 - a0
    return listOf(this, shiftHue<T>(a0), shiftHue<T>(a0 + a1), shiftHue<T>(a0 + a1 + a0))
}
