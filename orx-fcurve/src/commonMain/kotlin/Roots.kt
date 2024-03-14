/*
This is a direct port of the root finding code from https://pomax.github.io/bezierinfo/#extremities

A copy of the original license:
The following license terms apply to this repository and its derivative website and repositories:

- you may use any illustrative code found in the `docs/chapters` directories without crediting.
- you may use any illustrative graphics found in the `docs/images` directories without crediting.
- you may quote up to two paragraphs from the `docs/chapters` markdown documents without crediting.
- you may quote an entire section from any chapter, as long as it's credited and links back to the chapter that section is in on the official website.

If you wish to quote more than one section of a chapter (such as an entire chapter, or more than one chapter), you may do so only after requesting, and getting, explicit permission. Permission should be sought by filing an issue in this repository, which will act as permanent record of the granted permissions.

Outside of the above permissions, the following prohibitions and copyrights apply:

- You may not put up a clone of the entire work (meaning that if you fork the project, you may not turn on gh-pages to get it automatically hosted by github itself on your own account domain).
- The code in `docs/js/graphics-element/lib` consists of third party libraries governed by their own licenses.

Any other material not explicitly covered by this license is to be treated as having all rights reserved.
Please file an issue for license clarification questions.

 */

package org.openrndr.extra.fcurve
import kotlin.math.*

// A helper function to filter for values in the [0,1] interval:
private fun accept(t: Double): Boolean {
    return t in 0.0..1.0
}


// A real-cuberoots-only function:
private fun cuberoot(v: Double): Double {
    if (v < 0) return -(-v).pow(1.0 / 3);
    return v.pow(1.0 / 3)
}

internal fun approximately(a: Double, b: Double, epsilon: Double = 1E-8): Boolean {
    return abs(a - b) < epsilon
}

// Now then: given cubic coordinates {pa, pb, pc, pd} find all roots.
internal fun getCubicRoots(pa: Double, pb: Double, pc: Double, pd: Double): List<Double> {
    var a = (3 * pa - 6 * pb + 3 * pc)
    var b = (-3 * pa + 3 * pb)
    var c = pa
    val d = (-pa + 3 * pb - 3 * pc + pd);

    // do a check to see whether we even need cubic solving:
    if (approximately(d, 0.0)) {
        // this is not a cubic curve.
        if (approximately(a, 0.0)) {
            // in fact, this is not a quadratic curve either.
            if (approximately(b, 0.0)) {
                // in fact in fact, there are no solutions.
                return emptyList()
            }
            // linear solution
            return listOf(-c / b).filter(::accept)
        }
        // quadratic solution
        val q = sqrt(b * b - 4 * a * c)
        val twoA = 2 * a
        return listOf((q - b) / twoA, (-b - q) / twoA).filter(::accept)
    }

    // at this point, we know we need a cubic solution.

    a /= d
    b /= d
    c /= d

    val p = (3 * b - a * a) / 3
    val p3 = p / 3
    val q = (2 * a * a * a - 9 * a * b + 27 * c) / 27
    val q2 = q / 2
    val discriminant = q2 * q2 + p3 * p3 * p3

    // and some variables we're going to use later on:
    var u1 = 0.0
    var v1 = 0.0
    var root1 = 0.0
    var root2 = 0.0
    var root3 = 0.0

    // three possible real roots:
    if (discriminant < 0) {
        var mp3 = -p / 3
        val mp33 = mp3 * mp3 * mp3
        val r = sqrt(mp33)
        val t = -q / (2 * r)
        val cosphi = if (t < -1) -1.0 else if (t > 1) 1.0 else t
        val phi = acos(cosphi)
        val crtr = cuberoot(r)
        val t1 = 2 * crtr
        root1 = t1 * cos(phi / 3) - a / 3
        root2 = t1 * cos((phi + 2 * PI) / 3) - a / 3
        root3 = t1 * cos((phi + 4 * PI) / 3) - a / 3
        return listOf(root1, root2, root3).filter(::accept)
    }

    // three real roots, but two of them are equal:
    if (discriminant == 0.0) {
        u1 = if(q2 < 0) cuberoot(-q2) else -cuberoot(q2)
        root1 = 2 * u1 - a / 3
        root2 = -u1 - a / 3
        return listOf(root1, root2).filter(::accept)
    }

    // one real root, two complex roots
    val sd = sqrt(discriminant)
    u1 = cuberoot(sd - q2)
    v1 = cuberoot(sd + q2)
    root1 = u1 - v1 - a / 3
    return listOf(root1).filter(::accept)
}