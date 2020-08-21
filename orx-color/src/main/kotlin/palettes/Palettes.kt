package org.openrndr.extras.color.palettes

import org.openrndr.color.*
import org.openrndr.extras.color.spaces.ColorHPLUVa
import org.openrndr.extras.color.spaces.ColorHSLUVa
import org.openrndr.extras.color.spaces.toHPLUVa
import org.openrndr.extras.color.spaces.toHSLUVa



fun <T> colorSequence(vararg offsets: Pair<Double, T>): ColorSequence
        where T : ConvertibleToColorRGBa {
    return ColorSequence(offsets.sortedBy { it.first })
}

class ColorSequence(val colors: List<Pair<Double, ConvertibleToColorRGBa>>) {
    infix fun blend(steps: Int): List<ColorRGBa> = index(0.0, 1.0, steps)

    fun index(t0: Double, t1: Double, steps: Int) = (0 until steps).map {
        val f = (it / (steps - 1.0))
        val t = t0 * (1.0 - f) + t1 * f
        index(t)
    }

    fun index(t: Double): ColorRGBa {
        if (colors.size == 1) {
            return colors.first().second.toRGBa().toSRGB()
        }
        if (t < colors[0].first) {
            return colors[0].second.toRGBa().toSRGB()
        }
        if (t >= colors.last().first) {
            return colors.last().second.toRGBa().toSRGB()
        }
        val rightIndex = colors.indexOfLast { it.first <= t }
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
            is ColorLCHUVa -> right.second.toRGBa().toLCHUVa().mix(l, nt).toRGBa()
            is ColorLCHABa -> right.second.toRGBa().toLCHABa().mix(l, nt).toRGBa()
            else -> error("unsupported color space: ${l::class}")
        }.toSRGB()
    }
}

operator fun ConvertibleToColorRGBa.rangeTo(end: ConvertibleToColorRGBa) = colorSequence(0.0 to this, 1.0 to end)
