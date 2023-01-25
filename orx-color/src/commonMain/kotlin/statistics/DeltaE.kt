package org.openrndr.extra.color.statistics

import org.openrndr.color.ColorLABa
import org.openrndr.color.ConvertibleToColorRGBa
import org.openrndr.math.Vector3

/**
 * Computes delta E between two colors.
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