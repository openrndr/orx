package org.openrndr.extra.keyframer

import org.openrndr.extra.easing.Easing
import org.openrndr.extra.easing.EasingFunction
import org.openrndr.math.map

internal val defaultEnvelope = doubleArrayOf(0.0, 1.0)

class Key(val time: Double, val value: Double, val easing: EasingFunction, val envelope: DoubleArray = defaultEnvelope)


class KeyframerChannel {
    val keys = mutableListOf<Key>()

    operator fun invoke() : Double {
        return 0.0
    }

    fun add(
            time: Double,
            value: Double?,
            easing: EasingFunction = Easing.Linear.function,
            envelope: DoubleArray = defaultEnvelope
    ) {
        require(envelope.size >= 2) {
            "envelope should contain at least 2 entries"
        }
        value?.let {
            keys.add(Key(time, it, easing, envelope))
        }
    }

    fun lastValue(): Double? {
        return keys.lastOrNull()?.value
    }

    fun lastTime(): Double? {
        return keys.lastOrNull()?.time
    }

    fun duration(): Double {
        return keys.last().time
    }

    fun value(time: Double): Double? {
        if (keys.size == 0) {
            return null
        }
        if (keys.size == 1) {
            return if (time < keys.first().time) {
                null
            } else {
                keys[0].value
            }
        }

        if (time < keys.first().time) {
            return null
        }

        val rightIndex = keys.indexOfFirst { it.time > time }
        return if (rightIndex == -1) {
            keys.last().value
        } else {
            val leftIndex = (rightIndex - 1).coerceAtLeast(0)
            val rightKey = keys[rightIndex]
            val leftKey = keys[leftIndex]
            val t0 = (time - leftKey.time) / (rightKey.time - leftKey.time)
            val te = t0.map(rightKey.envelope[0], rightKey.envelope[1], 0.0, 1.0, clamp = true)
            val e0 = rightKey.easing(te, 0.0, 1.0, 1.0)
            leftKey.value * (1.0 - e0) + rightKey.value * (e0)
        }
    }
}

