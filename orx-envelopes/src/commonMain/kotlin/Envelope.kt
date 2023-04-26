package org.openrndr.extra.envelopes

abstract class Envelope {
    abstract fun value(t: Double, tOff: Double): Double

    abstract fun position(t: Double, tOff: Double): Double

    abstract fun isActive(t: Double, tOff: Double): Boolean

    var objectFunction: ((time: Double, value: Double, position: Double) -> Unit) = { _, _, _ -> }
}