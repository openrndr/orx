@file:JvmName("GradientDescentJvmKt")
package org.openrndr.extra.gradientdescent

fun <T : Any> minimizeModel(model: T, endOnLineSearch: Boolean = false, tol: Double = 1e-8, maxIterations: Int = 1000, function: (T) -> Double) {
    val doubles = modelToArray(model)
    val weights = DoubleArray(doubles.size) { 1.0 }
    val solution = minimize(doubles, weights, endOnLineSearch, tol, maxIterations) {
        arrayToModel(it, model)
        function(model)
    }
    arrayToModel(solution.solution, model)
}