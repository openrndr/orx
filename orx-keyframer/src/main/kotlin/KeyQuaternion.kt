package org.openrndr.extra.keyframer

import org.openrndr.extras.easing.Easing
import org.openrndr.extras.easing.EasingFunction
import org.openrndr.math.Quaternion
import org.openrndr.math.slerp

class KeyQuaternion(val time: Double, val value: Quaternion, val easing: EasingFunction)

class KeyframerChannelQuaternion {
    val keys = mutableListOf<KeyQuaternion>()

    operator fun invoke() : Double {
        return 0.0
    }

    fun add(time: Double, value: Quaternion?, easing: EasingFunction = Easing.Linear.function, jump: Hold = Hold.HoldNone) {
        if (jump == Hold.HoldAll || (jump == Hold.HoldSet && value != null)) {
            lastValue()?.let {
                keys.add(KeyQuaternion(time, it, Easing.Linear.function))
            }
        }
        value?.let {
            keys.add(KeyQuaternion(time, it, easing))
        }
    }

    fun lastValue(): Quaternion? {
        return keys.lastOrNull()?.value
    }

    fun duration(): Double {
        return keys.last().time
    }

    fun value(time: Double): Quaternion? {
        if (keys.size == 0) {
            return null
        }
        if (keys.size == 1) {
            return if (time < keys.first().time) {
                keys[0].value.normalized
            } else {
                keys[0].value.normalized
            }
        }

        if (time < keys.first().time) {
            return null
        }

        val rightIndex = keys.indexOfFirst { it.time > time }
        return if (rightIndex == -1) {
            keys.last().value.normalized
        } else {
            val leftIndex = (rightIndex - 1).coerceAtLeast(0)
            val rightKey = keys[rightIndex]
            val leftKey = keys[leftIndex]
            val t0 = (time - leftKey.time) / (rightKey.time - leftKey.time)
            val e0 = rightKey.easing(t0, 0.0, 1.0, 1.0)
            slerp(leftKey.value, rightKey.value, e0).normalized
        }
    }
}