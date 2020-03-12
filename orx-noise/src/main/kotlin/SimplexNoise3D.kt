package org.openrndr.extra.noise


private const val F3 = (1.0 / 3.0).toFloat()
private const val G3 = (1.0 / 6.0).toFloat()
private const val G33 = G3 * 3 - 1

fun simplex(seed: Int, x: Double, y: Double, z: Double): Double {

    val t = (x + y + z) / 3.0
    val i = (x + t).fastFloor()
    val j = (y + t).fastFloor()
    val k = (z + t).fastFloor()

    val t2 = (i + j + k) / 6.0
    val x0 = x - (i - t2)
    val y0 = y - (j - t2)
    val z0 = z - (k - t2)

    val i1: Int
    val j1: Int
    val k1: Int

    val i2: Int
    val j2: Int
    val k2: Int

    if (x0 >= y0) {
        when {
            y0 >= z0 -> {
                i1 = 1; j1 = 0; k1 = 0; i2 = 1; j2 = 1; k2 = 0; }
            x0 >= z0 -> {
                i1 = 1; j1 = 0; k1 = 0; i2 = 1; j2 = 0; k2 = 1; }
            else -> {
                i1 = 0; j1 = 0; k1 = 1; i2 = 1; j2 = 0; k2 = 1; }
        }
    } else {
        when {
            y0 < z0 -> {
                i1 = 0; j1 = 0; k1 = 1; i2 = 0; j2 = 1; k2 = 1; }
            x0 < z0 -> {
                i1 = 0; j1 = 1; k1 = 0; i2 = 0; j2 = 1; k2 = 1; }
            else -> {
                i1 = 0; j1 = 1; k1 = 0; i2 = 1; j2 = 1; k2 = 0; }
        }
    }
    val x1 = x0 - i1 + 1.0 / 6.0
    val y1 = y0 - j1 + 1.0 / 6.0
    val z1 = z0 - k1 + 1.0 / 6.0
    val x2 = x0 - i2 + 1.0 / 3.0
    val y2 = y0 - j2 + 1.0 / 3.0
    val z2 = z0 - k2 + 1.0 / 3.0
    val x3 = x0 + G33
    val y3 = y0 + G33
    val z3 = z0 + G33

    val n0: Double
    run {
        var t = 0.6 - x0 * x0 - y0 * y0 - z0 * z0
        if (t < 0) {
            n0 = 0.0
        } else {
            t *= t
            n0 = t * t * gradCoord3D(seed, i, j, k, x0, y0, z0)
        }
    }
    val n1: Double
    run {
        var t = 0.6 - x1 * x1 - y1 * y1 - z1 * z1
        if (t < 0) {
            n1 = 0.0
        } else {
            t *= t
            n1 = t * t * gradCoord3D(seed, i + i1, j + j1, k + k1, x1, y1, z1)
        }
    }
    val n2: Double
    run {
        var t = 0.6 - x2 * x2 - y2 * y2 - z2 * z2
        if (t < 0) {
            n2 = 0.0
        } else {
            t *= t
            n2 = t * t * gradCoord3D(seed, i + i2, j + j2, k + k2, x2, y2, z2)
        }
    }

    val n3: Double
    run {
        var t = 0.6 - x3 * x3 - y3 * y3 - z3 * z3
        if (t < 0)
            n3 = 0.0
        else {
            t *= t
            n3 = t * t * gradCoord3D(seed, i + 1, j + 1, k + 1, x3, y3, z3)
        }
    }
    return 32 * (n0 + n1 + n2 + n3)
}