package org.openrndr.extra.color.palettes

import org.openrndr.color.ColorModel
import org.openrndr.color.ColorRGBa
import org.openrndr.color.HueShiftableColor
import org.openrndr.extra.color.tools.shiftHue

/**
 * Generates an analogous color palette based on the current color.
 *
 * This function creates a sequence of colors by shifting the hue of the current color
 * gradually across a specified range of steps, using a particular color model that supports hue shifting.
 *
 * @param T The color model used for hue shifting.
 *          Must extend both `HueShiftableColor` and `ColorModel`.
 * @param hueShift The total degree shift in hue between the first color and the last color.
 *                 The hue shift is divided among the specified number of steps.
 * @param steps The number of colors to include in the palette, including the starting color.
 *              Defaults to 5.
 * @return A list of `ColorRGBa` instances forming the analogous palette.
 */
inline fun <reified T> ColorRGBa.analogous(hueShift: Double, steps: Int = 5): List<ColorRGBa>
        where T : HueShiftableColor<T>,
              T : ColorModel<T> = (0 until steps).map {
    shiftHue<T>(hueShift * it / (steps - 1.0))
}

/**
 * Generates a split complementary color palette based on the current `ColorRGBa`.
 *
 * The method calculates complementary colors that are spread around the complementary
 * hue axis of the original color. Depending on the parameters, the result may include
 * two or four additional colors in addition to the original color.
 *
 * @param T The color model and hue shifting capability of the colors to generate.
 * @param splitFactor A value between 0.0 and 1.0 that controls the spread of the complementary colors
 *                    around the complementary hue. A higher value increases the angle between
 *                    the colors on the hue wheel, while a lower value decreases it.
 * @param double If `true`, the method will generate two additional colors derived by more granular
 *               shifts within the complementary range. If `false`, a simpler complementary palette
 *               is returned.
 * @return A list of `ColorRGBa` objects representing the split complementary palette, with the original
 *         color as the first element in the list.
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
 * Generates a triadic color palette based on the current `ColorRGBa`.
 *
 * Triadic colors are evenly spaced on the color wheel, forming a triangle.
 * This method generates two additional colors by evenly shifting the hue of the given color
 * at 120° intervals around the hue circle.
 *
 * @param T The color model and hue shifting capability of the colors to generate.
 * @return A list of `ColorRGBa` objects representing the triadic color palette.
 */
inline fun <reified T> ColorRGBa.triadic(): List<ColorRGBa>
        where T : HueShiftableColor<T>,
              T : ColorModel<T> = splitComplementary<T>(1.0 / 3.0)


/**
 * Generates a tetradic color scheme based on the current color.
 * A tetradic color scheme consists of four colors that are equidistant on the color wheel.
 *
 * @param aspectRatio A double value representing the aspect ratio of the tetradic scheme.
 * The aspect ratio determines the angular separation between the colors in the scheme.
 * Default is 1.0, resulting in equidistant colors.
 * @return A list of `ColorRGBa` instances representing the tetradic color scheme.
 * The list includes the original color and three additional colors derived by shifting the hue.
 */
inline fun <reified T> ColorRGBa.tetradic(aspectRatio: Double = 1.0): List<ColorRGBa>
        where T : HueShiftableColor<T>,
              T : ColorModel<T> {
    val a0 = 180 / (aspectRatio + 1)
    val a1 = 180 - a0
    return listOf(this, shiftHue<T>(a0), shiftHue<T>(a0 + a1), shiftHue<T>(a0 + a1 + a0))
}
