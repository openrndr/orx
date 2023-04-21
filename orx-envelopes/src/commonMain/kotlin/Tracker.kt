@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.envelopes

import org.openrndr.Program
import org.openrndr.animatable.Clock
import org.openrndr.extra.parameters.DoubleParameter

class Trigger(val on: Double, var off: Double, val envelope: Envelope)

class TrackerValue(val time: Double, val value: Double)
abstract class Tracker<T : Envelope>(val program: Program) {

    val triggers = mutableListOf<Trigger>()


    protected abstract fun createEnvelope(): T

    fun triggerOn() {
        val t = program.seconds
        triggers.removeAll { !it.envelope.isActive(t - it.on, it.off - it.on) }
        triggers.add(Trigger(program.seconds, 1E30, createEnvelope()))
    }

    fun triggerOff() {
        val t = program.seconds
        triggers.removeAll { !it.envelope.isActive(t - it.on, it.off - it.on) }
        triggers.lastOrNull()?.let {
            it.off = program.seconds
        }
    }

    fun values(): List<TrackerValue> {
        val t = program.seconds
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

class ADSRTracker(program: Program): Tracker<ADSR>(program) {

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