package org.openrndr.extra.noise

import org.openrndr.math.Vector2

fun ((Int, Double) -> Double).vector2(): (seed: Int, x: Double) -> Vector2 {
    val ref = this
    return { seed:Int, x:Double ->
        Vector2(ref(-seed, x), ref(seed, -x))
    }
}

private fun exampleVector() {
    ::simplex2D
}
