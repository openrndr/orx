@file:JvmName("HistogramJvm")
package org.openrndr.extra.color.statistics

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import kotlin.jvm.JvmName
import kotlin.random.Random

internal fun ColorRGBa.binIndex(binCount: Int): Triple<Int, Int, Int> {
    val rb = (r * binCount).toInt().coerceIn(0, binCount - 1)
    val gb = (g * binCount).toInt().coerceIn(0, binCount - 1)
    val bb = (b * binCount).toInt().coerceIn(0, binCount - 1)
    return Triple(rb, gb, bb)
}

