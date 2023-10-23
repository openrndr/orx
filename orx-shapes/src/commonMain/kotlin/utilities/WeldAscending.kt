package org.openrndr.extra.shapes.utilities

/**
 * Weld values if their distance is less than [epsilon]
 */
fun List<Double>.weldAscending(epsilon: Double = 1E-6): List<Double> {
    return if (size <= 1) {
        this
    } else {
        val result = mutableListOf(first())
        var lastPassed = first()
        for (i in 1 until size) {
            require(this[i] >= lastPassed) { "input list is not in ascending order" }
            if (this[i] - lastPassed > epsilon) {
                result.add(this[i])
                lastPassed = this[i]
            }
        }
        result
    }
}