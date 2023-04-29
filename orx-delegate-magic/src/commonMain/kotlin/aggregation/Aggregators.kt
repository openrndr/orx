@file:Suppress("PackageDirectoryMismatch")

package org.openrndr.extra.delegatemagic.aggregation

import kotlin.math.abs

/**
 * Return element with largest magnitude
 * @since 0.4.3
 */
fun List<Double>.maxMag(): Double {

    this.max()
    if (isEmpty()) {
        error("list is empty")
    }
    var maxMag = Double.NEGATIVE_INFINITY
    var maxMagWithSign = 0.0

    for (i in indices) {
        val a = abs(this[i])
        if (a > maxMag) {
            maxMag = a
            maxMagWithSign = this[i]
        }
    }
    return maxMagWithSign
}

/**
 * Return element with smallest magnitude
 * @since 0.4.3
 */
fun List<Double>.minMag(): Double {
    if (isEmpty()) {
        error("list is empty")
    }
    var minMag = Double.POSITIVE_INFINITY
    var minMagWithSign = 0.0

    for (i in indices) {
        val a = abs(this[i])
        if (a < minMag) {
            minMag = a
            minMagWithSign = this[i]
        }
    }
    return minMagWithSign
}