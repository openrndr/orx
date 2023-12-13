package org.openrndr.extra.color.tools

import org.openrndr.color.ChromaColor
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

fun <T> T.findMaxChroma(): Double
        where T : ChromaColor<T>,
              T : ConvertibleToColorRGBa {
    return binarySearchMax(0.0, 200.0, chroma, 1E-5) {
        val c = withChroma(it).toRGBa()
        !c.isOutOfGamut
    }
}

fun<T> T.clipChroma(): T
        where T : ChromaColor<T>,
              T : ConvertibleToColorRGBa {

    val maxChroma = findMaxChroma()
    return withChroma(maxChroma)
}