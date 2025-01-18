package org.openrndr.extra.color.statistics

import org.openrndr.color.ColorLABa
import org.openrndr.color.ConvertibleToColorRGBa
import org.openrndr.math.Vector3

/**
 * Computes the CIE76 color difference (ΔE*76) between this color and another color.
 * The method calculates the Euclidean distance between the two colors in the LAB color space.
 * If either of the colors is not in LAB format, it will be converted to LAB before computation.
 *
 * @param other The second color to compare, which should implement the ConvertibleToColorRGBa interface.
 * @return The calculated CIE76 color difference as a Double.
 */
fun <T: ConvertibleToColorRGBa> T.deltaE76(other: T): Double {
    return if (this is ColorLABa && other is ColorLABa) {
        val tv = Vector3(l, a, b)
        val ov = Vector3(other.l, other.a, other.b)
        tv.distanceTo(ov)
    } else {
        val tLab = if (this is ColorLABa) this else this.toRGBa().toLABa()
        val oLab = if (other is ColorLABa) other else other.toRGBa().toLABa()
        tLab.deltaE76(oLab)
    }
}