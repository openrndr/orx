// Adapted from the numeric.js gradient and uncmin functions
// Numeric Javascript
// Copyright (C) 2011 by Sébastien Loisel

// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:

// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.

// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
//

package org.openrndr.extra.gradientdescent

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

fun ten(a: DoubleArray, b: DoubleArray): Array<DoubleArray> = Array(a.size) { mul(b, a[it]) }
fun max(a: Double, b: Double, c: Double): Double = max(max(a, b), c)

fun max(a: Double, b: Double, c: Double, d: Double, e: Double, f: Double, g: Double, h: Double): Double {
    return max(max(max(max(max(max(max(a, b), c), d), e), f), g), h)
}

fun gradient(x: DoubleArray, objective: (parameters: DoubleArray) -> Double): DoubleArray {
    var k = 0
    val tempX = x.copyOf()
    val f1 = objective(x)
    val grad = DoubleArray(x.size)
    require(f1 == f1)
    for (i in 0 until x.size) {
        var delta = max(1e-6 * f1, 1e-8)

        while (true) {
            require(k != 20) { "gradient failed" }
            tempX[i] = x[i] + delta
            val f0 = objective(tempX)
            tempX[i] = x[i] - delta
            val f2 = objective(tempX)
            tempX[i] = x[i]

            if (f0 == f0 && f2 == f2) {
                grad[i] = (f0 - f2) / (2 * delta)
                val t0 = x[i] - delta
                val t1 = x[i]
                val t2 = x[i] + delta
                val d1 = (f0 - f1) / delta
                val d2 = (f1 - f2) / delta
                val err = min(max(abs(d1 - grad[i]), abs(d2 - grad[i]), abs(d1 - d2)), delta)
                val normalize = max(abs(grad[i]), abs(f0), abs(f1), abs(f2), abs(t0), abs(t1), abs(t2), 1e-8)
                if (err / normalize < 1e-3)
                    break
            }
            delta /= 16.0
            k++
        }
    }
    //println("gradient at (${x.contentToString()}) -> (${grad.contentToString()}) ")
    return grad
}

private fun identity(n: Int): Array<DoubleArray> = Array(n) { j ->
    DoubleArray(n) { i -> if (i == j) 1.0 else 0.0 }
}

private fun neg(x: DoubleArray): DoubleArray = DoubleArray(x.size) { -x[it] }
private fun add(x: DoubleArray, y: DoubleArray): DoubleArray = DoubleArray(x.size) { x[it] + y[it] }
private fun sub(x: DoubleArray, y: DoubleArray): DoubleArray = DoubleArray(x.size) { x[it] - y[it] }
private fun add(x: Array<DoubleArray>, y: Array<DoubleArray>) = Array(x.size) { add(x[it], y[it]) }
private fun sub(x: Array<DoubleArray>, y: Array<DoubleArray>) = Array(x.size) { sub(x[it], y[it]) }
private fun mul(x: Array<DoubleArray>, y: Double) = Array(x.size) { mul(x[it], y) }
private fun mul(x: DoubleArray, y: Double) = DoubleArray(x.size) { x[it] * y }
private fun div(x: Array<DoubleArray>, y: Double) = Array(x.size) { div(x[it], y) }
private fun div(x: DoubleArray, y: Double) = DoubleArray(x.size) { x[it] / y }
private fun norm2(x: DoubleArray): Double {
    return sqrt(x.sumByDouble { it * it })
}

fun dot(x: DoubleArray, y: DoubleArray): Double = (x.mapIndexed { index, it -> it * y[index] }).sum()

fun dot(x: Array<DoubleArray>, y: DoubleArray): DoubleArray = DoubleArray(x.size) { dot(x[it], y) }

class MinimizationResult(val solution: DoubleArray, val value: Double, val gradient: DoubleArray,
                         val inverseHessian: Array<DoubleArray>, val iterations: Int)

fun minimize(_x0: DoubleArray, endOnLineSearch: Boolean = true, tol: Double = 1e-8, maxIterations: Int = 1000, f: (DoubleArray) -> Double): MinimizationResult {
    val grad = { a: DoubleArray -> gradient(a, f) }
    var x0 = _x0.copyOf()
    var g0 = grad(x0)
    var f0 = f(x0)
    require(f0 == f0)

    var H1 = identity(_x0.size)
    var iteration = 0
    while (iteration < maxIterations) {
        require(g0.all { it == it && it != Double.POSITIVE_INFINITY && it != Double.NEGATIVE_INFINITY })
        val pstep = dot(H1, g0)
        require(pstep.all { it == it }) { "pstep contains NaNs"}
        require(pstep.all { it != Double.POSITIVE_INFINITY && it != Double.NEGATIVE_INFINITY }) { "pstep contains infs"  }
        val step = neg(pstep)

        val nstep = norm2(step)
        require(nstep == nstep)
        if (nstep < tol) {
            break
        }
        var t = 1.0
        val df0 = dot(g0, step)
        var x1 = x0
        var s = DoubleArray(0)
        var f1 = Double.POSITIVE_INFINITY
        while (iteration < maxIterations && t * nstep >= tol) {
            s = mul(step, t)
            x1 = add(x0, s)
            f1 = f(x1)

            require(f1 == f1) { "f1 is NaN"}
            if (!(f1 - f0 >= 0.1 * t * df0)) {
                break
            }
            t *= 0.5
            iteration++

        }
        require(s.isNotEmpty())
        if (t * nstep < tol && endOnLineSearch) {
            break
        }
        if (iteration >= maxIterations) break
        val g1 = grad(x1)
        require(g1.all { it == it })
        val y = sub(g1, g0)
        val ys = dot(y, s)
        if (ys==0.0) {
            break
        }
        val Hy = dot(H1, y)
        H1 = sub(
                add(
                        H1,
                        mul(
                                ten(s, s),
                                (ys + dot(y, Hy)) / (ys * ys)
                        )
                ),
                div(
                        add(
                                ten(Hy, s),
                                ten(s, Hy)
                        ),
                        ys
                )
        )
        x0 = x1
        f0 = f1
        g0 = g1
        iteration++
    }

    return MinimizationResult(x0, f0, g0, H1, iteration)
}
