package org.openrndr.extra.keyframer

import org.openrndr.extras.easing.Easing
import org.openrndr.extras.easing.EasingFunction

class Key(val time: Double, val value: Double, val easing: EasingFunction)

enum class Hold {
    HoldNone,
    HoldSet,
    HoldAll
}

class KeyframerChannel {
    val keys = mutableListOf<Key>()

    operator fun invoke() : Double {
        return 0.0
    }

    fun add(time: Double, value: Double?, easing: EasingFunction = Easing.Linear.function, jump: Hold = Hold.HoldNone) {
        if (jump == Hold.HoldAll || (jump == Hold.HoldSet && value != null)) {
            lastValue()?.let {
                keys.add(Key(time, it, Easing.Linear.function))
            }
        }
        value?.let {
            keys.add(Key(time, it, easing))
        }
    }

    fun lastValue(): Double? {
        return keys.lastOrNull()?.value
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
            val e0 = rightKey.easing(t0, 0.0, 1.0, 1.0)
            leftKey.value * (1.0 - e0) + rightKey.value * (e0)
        }
    }
}