package org.openrndr.extra.timer

import kotlinx.coroutines.yield
import org.openrndr.Program
import org.openrndr.launch

fun Program.timeOut(delayInSeconds: Double, action: () -> Unit) = repeat(1.0, 1, delayInSeconds, action)

fun Program.repeat(intervalInSeconds: Double, count: Int? = null, initialDelayInSeconds: Double = 0.0, action: () -> Unit) {
    val start = seconds + initialDelayInSeconds
    var repetitions = 0

    launch {
        while (count == null || repetitions < count) {
            val launchTime = start + repetitions * intervalInSeconds
            while (seconds < launchTime) {
                yield()
            }
            action()
            repetitions++
        }
    }
}