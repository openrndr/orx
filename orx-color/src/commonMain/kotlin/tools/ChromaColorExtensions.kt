package org.openrndr.extra.color.tools

import org.openrndr.color.ChromaColor
import org.openrndr.color.ColorRGBa
import org.openrndr.color.ConvertibleToColorRGBa


private fun binarySearchMax(min: Double, max: Double, start: Double, threshold: Double = 1E-5, f: (Double) -> Boolean): Double {
    var low = min
    var high = max

    var best = min
    var mid = start
    while (low <= high) {

        val res = f(mid)

        if (res) {
            best = mid
            low = mid
        } else {
            high = mid
        }

        if (high - low < threshold) {
            return best
        }
        mid = (low + high) / 2.0
    }
    return best
}

// isOutOfGamut tolerates components down to -1E-3, which is too loose for finding the gamut boundary: clipping the
// result to the gamut afterwards visibly shifts its hue, the tolerance here only covers conversion round-off
private const val GAMUT_TOLERANCE = 1E-5

private fun ColorRGBa.isInGamutStrict(): Boolean {
    val range = -GAMUT_TOLERANCE..1.0 + GAMUT_TOLERANCE
    return r in range && g in range && b in range
}

/**
 * Finds the largest chroma for the color's luminosity and hue that is inside the RGB gamut.
 */
fun <T> T.findMaxChroma(): Double
        where T : ChromaColor<T>,
              T : ConvertibleToColorRGBa {
    return binarySearchMax(0.0, 200.0, chroma, 1E-5) {
        withChroma(it).toRGBa().isInGamutStrict()
    }
}

fun<T> T.clipChroma(): T
        where T : ChromaColor<T>,
              T : ConvertibleToColorRGBa {

    val maxChroma = findMaxChroma()
    return withChroma(maxChroma)
}