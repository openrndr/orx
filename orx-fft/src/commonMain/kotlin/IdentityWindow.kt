package org.openrndr.extra.fft

class IdentityWindow
    : WindowFunction() {
    override fun value(length: Int, index: Int): Float = 1.0f
}