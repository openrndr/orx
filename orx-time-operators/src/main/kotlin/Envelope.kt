package org.openrndr.extra.timeoperators

import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.math.clamp
import org.openrndr.math.mix
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow


enum class EnvelopePhase {
    Rest, Attack, Decay
}

// Exponential Ease-In and Ease-Out by Golan Levin
// http://www.flong.com/texts/code/shapers_exp/
private fun exponentialEasing(x: Double, a: Double): Double {
    var a = a
    val epsilon = 0.00001
    val minParamA = (0.0 + epsilon)
    val maxParamA = (1.0 - epsilon)
    a = max(minParamA, min(maxParamA, a))

    return if (a < 0.5) {
        // emphasis
        a = (2.0 * a)
        x.pow(a)
    } else {
        // de-emphasis
        a = (2.0 * (a - 0.5))
        x.pow(1.0 / (1 - a))
    }
}

class Envelope(
        var restValue: Double = 0.0,
        var targetValue: Double = 1.0,
        @DoubleParameter("Attack Duration", 0.0, 5.0, 3, 0)
        var attack: Double = 0.3,
        @DoubleParameter("Decay Duration", 0.0, 5.0, 3, 1)
        var decay: Double = 0.5,
        @DoubleParameter("Easing Factor", 0.0, 1.0, 3, 2)
        var easingFactor: Double = 0.3,
        @BooleanParameter("Re-trigger", 3)
        var reTrigger: Boolean = false
) : TimeTools
{
    var phase = EnvelopePhase.Rest
        set(value) {
            if (value == EnvelopePhase.Rest) {
                initialTime = Double.NEGATIVE_INFINITY
                current = initialRestValue
            }

            field = value
        }

    val value: Double
        get() {
            return current
        }

    private var initialTime = Double.NEGATIVE_INFINITY
    private var current = restValue
    private var initialRestValue = restValue
    private val cycleDuration: Double
        get() {
            return attack + decay
        }

    override fun tick(seconds: Double, deltaTime: Double, frameCount: Int) {
        if (phase == EnvelopePhase.Rest) return

        // TODO: what happens if deltaTime < cycleDuration?

        if (initialTime == Double.NEGATIVE_INFINITY) {
            initialTime = seconds
        }

        val cycleTime = seconds - initialTime

        if (cycleTime < 0) {
            phase = EnvelopePhase.Rest
            return
        }

        if (cycleTime <= attack) {
            phase = EnvelopePhase.Attack
        } else if (cycleTime > attack && cycleTime < cycleDuration) {
            phase = EnvelopePhase.Decay
        } else {
            phase = EnvelopePhase.Rest
            return
        }

        if (phase == EnvelopePhase.Attack) {
            current = if (attack == 0.0) {
                targetValue
            } else {
                val t = clamp(cycleTime / attack, 0.0, 1.0)

                mix(restValue, targetValue, exponentialEasing(t, easingFactor))
            }
        }

        if (phase == EnvelopePhase.Decay) {
            current = if (decay == 0.0) {
                initialRestValue
            } else {
                val t = clamp((cycleTime - attack) / decay, 0.0, 1.0)

                mix(targetValue, initialRestValue, exponentialEasing(t, easingFactor))
            }
        }

        if (current.isNaN()) {
            println("current is NaN, $phase")
        }
    }

    fun trigger(value: Double = targetValue) {
        restValue = if (initialTime != Double.NEGATIVE_INFINITY) {
            current
        } else {
            initialRestValue
        }

        if (reTrigger) {
            restValue = initialRestValue
        }

        initialTime = Double.NEGATIVE_INFINITY
        phase = EnvelopePhase.Attack

        targetValue = value
    }
}