package org.openrndr.extra.color.colormaps

import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector3
import org.openrndr.math.saturate

/**
 * Accurate spectral colormap developed by Alan Zucconi.
 *
 * @see spectralZucconi6Vector
 * @see ColormapPhraseBook.spectralZucconi6
 */
fun spectralZucconi6(
    x: Double
): ColorRGBa = ColorRGBa.fromVector(
    spectralZucconi6Vector(x)
)

/**
 * Accurate spectral colormap developed by Alan Zucconi.
 *
 * @see ColormapPhraseBook.spectralZucconi6
 */
fun spectralZucconi6Vector(x: Double): Vector3 {
    val v = Vector3(x)
    return bump3y(c1 * (v - x1), y1) + bump3y(c2 * (v - x2), y2)
}

private fun bump3y(
    x: Vector3,
    yOffset: Vector3
) = (Vector3.ONE - x * x - yOffset).saturate()

private val c1 = Vector3(3.54585104, 2.93225262, 2.41593945)
private val x1 = Vector3(0.69549072, 0.49228336, 0.27699880)
private val y1 = Vector3(0.02312639, 0.15225084, 0.52607955)

private val c2 = Vector3(3.90307140, 3.21182957, 3.96587128)
private val x2 = Vector3(0.11748627, 0.86755042, 0.66077860)
private val y2 = Vector3(0.84897130, 0.88445281, 0.73949448)
