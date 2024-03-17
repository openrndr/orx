package org.openrndr.extra.fft

import kotlin.math.PI
import kotlin.math.cos

class HannWindow : WindowFunction() {
    override fun value(length: Int, index: Int): Float = 0.5f * (1f - cos((PI * 2.0 * index / (length - 1f)))
        .toFloat())
}