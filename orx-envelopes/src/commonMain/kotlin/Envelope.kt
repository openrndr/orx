package org.openrndr.extra.envelopes

interface Envelope {
    fun value(t: Double, tOff: Double): Double

    fun isActive(t: Double, tOff:Double): Boolean
}