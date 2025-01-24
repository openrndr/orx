package org.openrndr.extra.color.palettes

import org.openrndr.color.*
import org.openrndr.draw.*
import org.openrndr.extra.color.spaces.*


/**
 * Creates a `ColorSequence` by accepting a variable number of pairs, where each pair consists of
 * a position (Double) and a color (T). The positions represent the normalized range `[0.0, 1.0]`.
 * The resulting `ColorSequence` can be used for creating interpolated colors between the specified positions.
 *
 * @param offsets Vararg parameter of pairs, where each pair includes a position (Double) and a color (of type T).
 * The position defines the location along a normalized sequence `[0.0, 1.0]`, and the color must implement `ConvertibleToColorRGBa`.
 * Typically, positions must be sorted, but the function will sort them internally based on their position values.
 * @return A `ColorSequence` containing the sorted sequence of colors and positions.
 */
fun <T> colorSequence(vararg offsets: Pair<Double, T>): ColorSequence
        where T : ConvertibleToColorRGBa {
    return ColorSequence(offsets.sortedBy { it.first })
}

/**
 * Represents a sequence of colors along with their corresponding positions in a normalized range [0.0, 1.0].
 * The `ColorSequence` allows for creating interpolated colors between the specified color points.
 *
 * @property colors A list of pairs where the first value is a position (ranging from 0.0 to 1.0)
 * and the second value is a color that implements `ConvertibleToColorRGBa`.
 */
class ColorSequence(val colors: List<Pair<Double, ConvertibleToColorRGBa>>) {
    infix fun blend(steps: Int): List<ColorRGBa> = color(0.0, 1.0, steps)

    /**
     * Converts a color sequence into a color buffer with a gradient representation.
     *
     * @param drawer The Drawer used to render the gradient into the color buffer.
     * @param width The width of the resulting color buffer in pixels. Defaults to 256.
     * @param height The height of the resulting color buffer in pixels. Defaults to 16.
     * @param type The ColorType of the resulting color buffer. Defaults to UINT8_SRGB.
     * @param format The ColorFormat of the resulting color buffer. Defaults to RGBa.
     * @return A ColorBuffer containing the rendered color gradient.
     */
    fun toColorBuffer(
        drawer: Drawer,
        width: Int = 256,
        height: Int = 16,
        type: ColorType = ColorType.UINT8_SRGB,
        format: ColorFormat = ColorFormat.RGBa
    ): ColorBuffer {
        val cb = colorBuffer(width, height, type = type, format = format)
        val rt = renderTarget(width, height) {
            colorBuffer(cb)
        }

        drawer.isolatedWithTarget(rt) {
            defaults()
            ortho(rt)
            drawer.rectangles {
                for (i in 0 until width) {
                    fill = color(i / (width.toDouble() - 1.0))
                    stroke = null
                    rectangle(i * 1.0, 0.0, 1.0, height.toDouble())
                }
            }
        }

        rt.destroy()
        return cb
    }

    /**
     * Generates a sequence of interpolated colors between two specified values.
     *
     * @param t0 A Double representing the start value for interpolation.
     * @param t1 A Double representing the end value for interpolation.
     * @param steps An Int representing the number of colors to generate in the sequence.
     * @return A List of interpolated colors.
     */
    fun color(t0: Double, t1: Double, steps: Int) = (0 until steps).map {
        val f = (it / (steps - 1.0))
        val t = t0 * (1.0 - f) + t1 * f
        color(t)
    }

    /**
     * Calculates a color using interpolation based on the provided parameter `t`.
     *
     * @param t A Double representing the position along the color sequence, typically ranging from 0.0 to 1.0.
     * It indicates how far between the sequence colors the interpolation should occur,
     * with 0.0 being the start of the sequence and 1.0 being the end.
     * @return A ColorRGBa instance representing the interpolated color in the sRGB color space.
     * If the provided `t` is outside the range of the sequence, the color at the nearest boundary will be returned.
     */
    fun color(t: Double): ColorRGBa {
        if (colors.size == 1) {
            return colors.first().second.toRGBa().toSRGB()
        }
        if (t < colors[0].first) {
            return colors[0].second.toRGBa().toSRGB()
        }
        if (t >= colors.last().first) {
            return colors.last().second.toRGBa().toSRGB()
        }
        val rightIndex = colors.binarySearch { it.first.compareTo(t) }.let { if (it < 0) -it - 2 else it }
        val leftIndex = (rightIndex + 1).coerceIn(0, colors.size - 1)

        val right = colors[rightIndex]
        val left = colors[leftIndex]

        val rt = t - right.first
        val dt = left.first - right.first
        val nt = rt / dt

        return when (val l = left.second) {
            is ColorRGBa -> right.second.toRGBa().mix(l, nt)
            is ColorHSVa -> right.second.toRGBa().toHSVa().mix(l, nt).toRGBa()
            is ColorHSLa -> right.second.toRGBa().toHSLa().mix(l, nt).toRGBa()
            is ColorXSVa -> right.second.toRGBa().toXSVa().mix(l, nt).toRGBa()
            is ColorXSLa -> right.second.toRGBa().toXSLa().mix(l, nt).toRGBa()
            is ColorLABa -> right.second.toRGBa().toLABa().mix(l, nt).toRGBa()
            is ColorLUVa -> right.second.toRGBa().toLUVa().mix(l, nt).toRGBa()
            is ColorHSLUVa -> right.second.toRGBa().toHSLUVa().mix(l, nt).toRGBa()
            is ColorHPLUVa -> right.second.toRGBa().toHPLUVa().mix(l, nt).toRGBa()
            is ColorXSLUVa -> right.second.toRGBa().toXSLUVa().mix(l, nt).toRGBa()
            is ColorLCHUVa -> right.second.toRGBa().toLCHUVa().mix(l, nt).toRGBa()
            is ColorLCHABa -> right.second.toRGBa().toLCHABa().mix(l, nt).toRGBa()
            is ColorOKLABa -> right.second.toRGBa().toOKLABa().mix(l, nt).toRGBa()
            is ColorOKLCHa -> right.second.toRGBa().toOKLCHa().mix(l, nt).toRGBa()
            is ColorOKHSLa -> right.second.toRGBa().toOKHSLa().mix(l, nt).toRGBa()
            is ColorOKHSVa -> right.second.toRGBa().toOKHSVa().mix(l, nt).toRGBa()
            else -> error("unsupported color space: ${l::class}")
        }.toSRGB()
    }
}

/**
 * Defines a range between two colors by creating a sequence of colors
 * that transition smoothly from the start color to the end color.
 *
 * @param end The end color of the range. Both start and end colors must implement `ConvertibleToColorRGBa`.
 * The start color is implicitly the color on which this operator is called.
 */
operator fun ConvertibleToColorRGBa.rangeTo(end: ConvertibleToColorRGBa) = colorSequence(0.0 to this, 1.0 to end)
