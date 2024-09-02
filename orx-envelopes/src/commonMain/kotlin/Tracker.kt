@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.envelopes

import org.openrndr.Clock
import org.openrndr.extra.parameters.DoubleParameter

class Trigger(val id: Int, val on: Double, var off: Double, val envelope: Envelope)

class TrackerValue(
    val time: Double,
    val value: Double,
    val position: Double,
    val envelope: Envelope
) {
    operator fun invoke() {
        draw()
    }

    fun draw() {
        envelope.objectFunction(time, value, position)
    }
}

abstract class Tracker<T : Envelope>(val clock: Clock) {
    val triggers = mutableListOf<Trigger>()

    protected abstract fun createEnvelope(objectFunction: (time: Double, value: Double, position: Double) -> Unit): T

    fun triggerOn(
        triggerId: Int = 0,
        objectFunction: (time: Double, value: Double, position: Double) -> Unit = { _, _, _ -> }
    ) {
        mppSynchronized(triggers) {
            val t = clock.seconds
            triggers.removeAll { !it.envelope.isActive(t - it.on, it.off - it.on) }
            triggers.add(Trigger(triggerId, clock.seconds, 1E30, createEnvelope(objectFunction)))
        }
    }

    fun triggerOff(triggerId: Int = 0) {
        mppSynchronized(triggers) {
            val t = clock.seconds
            triggers.removeAll { !it.envelope.isActive(t - it.on, it.off - it.on) }
            triggers.findLast { it.id == triggerId }?.let {
                it.off = clock.seconds
            }
        }
    }

    fun values(): List<TrackerValue> {
        val t = clock.seconds
        return mppSynchronized(triggers) {
            triggers.mapNotNull {
                val tOn = t - it.on
                val tOff = it.off - it.on

                if (it.envelope.isActive(tOn, tOff)) {
                    val v = it.envelope.value(tOn, tOff)
                    TrackerValue(t, v, it.envelope.position(tOn, tOff), it.envelope)
                } else {
                    null
                }
            }
        }
    }

    fun value(): Double {
        return values().sumOf { it.value }
    }
}


class ADSRTracker(clock: Clock) : Tracker<ADSR>(clock) {

    /**
     * The time it takes to transition to 1.0 when calling [triggerOn], usually in seconds.
     */
    @DoubleParameter("attack", 0.0, 20.0, order = 1)
    var attack: Double = 0.1

    /**
     * The time it takes to transition from 1.0 to the [sustain] level, usually in seconds.
     * The decay happens immediately after the attack.
     */
    @DoubleParameter("decay", 0.0, 20.0, order = 2)
    var decay: Double = 0.1

    /**
     * The sustain level, between 0.0 and 1.0.
     * The tracker will keep this value until [triggerOff] is called.
     */
    @DoubleParameter("sustain", 0.0, 1.0, order = 3)
    var sustain: Double = 0.9

    /**
     * The time it takes to transition back to 0.0 when calling [triggerOff], usually in seconds.
     */
    @DoubleParameter("release", 0.0, 20.0, order = 4)
    var release: Double = 0.9

    override fun createEnvelope(objectFunction: (time: Double, value: Double, position: Double) -> Unit): ADSR {
        return ADSR(attack, decay, sustain, release).apply {
            this.objectFunction = objectFunction
        }
    }
}

