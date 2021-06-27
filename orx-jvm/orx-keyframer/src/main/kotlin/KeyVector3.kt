package org.openrndr.extra.keyframer

import org.openrndr.extras.easing.Easing
import org.openrndr.extras.easing.EasingFunction
import org.openrndr.math.Vector3

class KeyVector3(val time: Double, val value: Vector3, val easing: EasingFunction)

class KeyframerChannelVector3 {
    val keys = mutableListOf<KeyVector3>()

    operator fun invoke() : Double {
        return 0.0
    }

    fun add(time: Double, value: Vector3?, easing: EasingFunction = Easing.Linear.function) {
        value?.let {
            keys.add(KeyVector3(time, it, easing))
        }
    }

    fun lastValue(): Vector3? {
        return keys.lastOrNull()?.value
    }

    fun duration(): Double {
        return keys.last().time
    }

    fun value(time: Double): Vector3? {
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