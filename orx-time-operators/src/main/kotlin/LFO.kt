package org.openrndr.extra.timeoperators

import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.OptionParameter
import org.openrndr.math.clamp
import org.openrndr.math.mod
import kotlin.math.*

internal const val TAU = 2.0 * PI

enum class LFOWave {
    Saw, Sine, Square, Triangle
}

@Suppress("UNUSED")
@Description("LFO")
class LFO(wave: LFOWave = LFOWave.Saw) : TimeTools {
    @OptionParameter("Wave")
    var wave = wave

    private var current = 0.0
        set(value) {
            field = clamp(value, 0.0, 1.0)
        }

    private var initialTime = Double.NEGATIVE_INFINITY
    private var dt = 0.0
    private var time = 0.0

    override fun tick(seconds: Double, deltaTime: Double, frameCount: Int) {
        //time += deltaTime
        time = seconds
    }

    fun sample(frequency: Double = 1.0, phase: Double = 0.0): Double {
        return when(wave) {
            LFOWave.Saw -> saw(frequency, phase)
            LFOWave.Sine -> sine(frequency, phase)
            LFOWave.Square -> square(frequency, phase)
            LFOWave.Triangle -> triangle(frequency, phase)
        }
    }

    fun saw(frequency: Double = 1.0, phase: Double = 0.0): Double {
        val cycleFreq = 1.0 / frequency
        val cycleTime = mod(time + (phase * frequency), cycleFreq)
        current = (cycleTime) / cycleFreq
        return current
    }

    fun sine(frequency: Double = 1.0, phase: Double = 0.0): Double {
        current = sin((phase * TAU) + time * frequency * TAU) * 0.5 + 0.5
        return current
    }

    fun square(frequency: Double = 1.0, phase: Double = 0.0): Double {
        current = max(sign(sin((phase * TAU) + time * frequency * TAU)), 0.0)
        return current
    }

    fun triangle(frequency: Double = 1.0, phase: Double = 0.0): Double {
        val t = (time * frequency) + (phase * frequency)
        current = 1.0 - 2.0 * abs(mod(t, 1.0) - 0.5)
        return current
    }
}