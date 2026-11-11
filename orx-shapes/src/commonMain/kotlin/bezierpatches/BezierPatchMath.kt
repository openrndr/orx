package org.openrndr.extra.shapes.bezierpatches

/** the cubic Bernstein polynomial [k] at [t] */
internal fun bernstein3(t: Double, k: Int): Double {
    val s = 1.0 - t
    return when (k) {
        0 -> s * s * s
        1 -> 3.0 * s * s * t
        2 -> 3.0 * s * t * t
        else -> t * t * t
    }
}

/** the derivative of the cubic Bernstein polynomial [k] at [t] */
internal fun dbernstein3(t: Double, k: Int): Double {
    val s = 1.0 - t
    return when (k) {
        0 -> -3.0 * s * s
        1 -> 3.0 * s * s - 6.0 * s * t
        2 -> 6.0 * s * t - 3.0 * t * t
        else -> 3.0 * t * t
    }
}

/**
 * The weights of the control points of a cubic Bezier curve that give the control points of its part from [a] to
 * [b]: the control point k of the part is the sum over m of `weights[k][m]` times the control point m of the curve,
 * by blossoming
 */
internal fun subWeights(a: Double, b: Double): Array<DoubleArray> = Array(4) { k ->
    // the blossom at a (3 - k times) and b (k times), for every control point of the curve on its own
    val ts = DoubleArray(3) { if (it < 3 - k) a else b }
    DoubleArray(4) { m ->
        val c = DoubleArray(4) { if (it == m) 1.0 else 0.0 }
        for (level in 0 until 3) {
            val t = ts[level]
            for (i in 0 until 3 - level) c[i] = c[i] * (1 - t) + c[i + 1] * t
        }
        c[0]
    }
}

/**
 * The control grid of the part of a tensor product patch from u0 to u1 and v0 to v1, for any [value] that can be
 * scaled and added: `grid[j][i]` is the control value at row j (v) and column i (u)
 */
internal fun <T> subGrid(
    grid: List<List<T>>, u0: Double, v0: Double, u1: Double, v1: Double,
    times: (T, Double) -> T, plus: (T, T) -> T
): List<List<T>> {
    val wu = subWeights(u0, u1)
    val wv = subWeights(v0, v1)
    return List(4) { j ->
        List(4) { i ->
            var sum: T? = null
            for (l in 0 until 4) for (m in 0 until 4) {
                val w = wv[j][l] * wu[i][m]
                val term = times(grid[l][m], w)
                sum = if (sum == null) term else plus(sum, term)
            }
            sum!!
        }
    }
}
