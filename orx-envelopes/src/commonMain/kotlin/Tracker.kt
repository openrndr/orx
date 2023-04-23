@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.envelopes

import org.openrndr.Clock
import org.openrndr.extra.parameters.DoubleParameter

class Trigger(val on: Double, var off: Double, val envelope: Envelope)

class TrackerValue(val time: Double, val value: Double)
abstract class Tracker<T : Envelope>(val clock: Clock) {

    val triggers = mutableListOf<Trigger>()


    protected abstract fun createEnvelope(): T

    fun triggerOn() {
        val t = clock.seconds
        triggers.removeAll { !it.envelope.isActive(t - it.on, it.off - it.on) }
        triggers.add(Trigger(clock.seconds, 1E30, createEnvelope()))
    }

    fun triggerOff() {
        val t = clock.seconds
        triggers.removeAll { !it.envelope.isActive(t - it.on, it.off - it.on) }
        triggers.lastOrNull()?.let {
            it.off = clock.seconds
        }
    }

    fun values(): List<TrackerValue> {
        val t = clock.seconds
        return triggers.mapNotNull {
            val tOn = t - it.on
            val tOff = it.off - it.on

            if (it.envelope.isActive(tOn, tOff)) {
                val v = it.envelope.value(tOn, tOff)

                TrackerValue(t, v)
            } else {
                null
            }
        }
    }

    fun value(): Double {
        return values().sumOf { it.value }
    }

}

class ADSRTracker(clock: Clock): Tracker<ADSR>(clock) {

    @DoubleParameter("attack", 0.0, 20.0, order = 1)
    var attack: Double = 0.1
    @DoubleParameter("decay", 0.0, 20.0, order = 2)
    var decay: Double = 0.1
    @DoubleParameter("sustain", 0.0, 1.0, order = 3)
    var sustain: Double = 0.9
    @DoubleParameter("release", 0.0, 20.0, order = 4)
    var release: Double = 0.9

    override fun createEnvelope(): ADSR {
        return ADSR(attack, decay, sustain, release)
    }
}