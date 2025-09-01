package org.openrndr.extra.shapes.utilities

/**
 * Removes values from a list of doubles that are within a specified tolerance (`epsilon`) of the last added value,
 * while preserving the ascending order of the list. The input list must already be in ascending order.
 *
 * @param epsilon the minimum difference between consecutive values in the output list; defaults to 1E-6
 * @return a new list containing the filtered values in the same order, preserving ascending order
 * while eliminating near-duplicates
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