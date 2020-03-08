package org.openrndr.extra.noise

private val SIMPLEX_4D = byteArrayOf(
    0, 1, 2, 3, 0, 1, 3, 2, 0, 0, 0, 0, 0, 2, 3, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 0,
    0, 2, 1, 3, 0, 0, 0, 0, 0, 3, 1, 2, 0, 3, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 3, 2, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    1, 2, 0, 3, 0, 0, 0, 0, 1, 3, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 3, 0, 1, 2, 3, 1, 0,
    1, 0, 2, 3, 1, 0, 3, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 3, 1, 0, 0, 0, 0, 2, 1, 3, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    2, 0, 1, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 1, 2, 3, 0, 2, 1, 0, 0, 0, 0, 3, 1, 2, 0,
    2, 1, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 1, 0, 2, 0, 0, 0, 0, 3, 2, 0, 1, 3, 2, 1, 0
)

private val F4 = ((2.23606797 - 1.0) / 4.0)
private val G4 = ((5.0 - 2.23606797) / 20.0)

fun simplex(seed: Int, x: Double, y: Double, z: Double, w: Double): Double {

    var t = (x + y + z + w) * F4
    val i = (x + t).fastFloor()
    val j = (y + t).fastFloor()
    val k = (z + t).fastFloor()
    val l = (w + t).fastFloor()

    val t2 = (i + j + k + l) * G4
    val x0 = x - (i - t2)
    val y0 = y - (j - t2)
    val z0 = z - (k - t2)
    val w0 = w - (l - t2)

    var c = if (x0 > y0) 32 else 0
    c += if (x0 > z0) 16 else 0
    c += if (y0 > z0) 8 else 0
    c += if (x0 > w0) 4 else 0
    c += if (y0 > w0) 2 else 0
    c += if (z0 > w0) 1 else 0
    c = c shl 2

    val i1 = if (SIMPLEX_4D[c] >= 3) 1 else 0
    val i2 = if (SIMPLEX_4D[c] >= 2) 1 else 0
    val i3 = if (SIMPLEX_4D[c++] >= 1) 1 else 0
    val j1 = if (SIMPLEX_4D[c] >= 3) 1 else 0
    val j2 = if (SIMPLEX_4D[c] >= 2) 1 else 0
    val j3 = if (SIMPLEX_4D[c++] >= 1) 1 else 0
    val k1 = if (SIMPLEX_4D[c] >= 3) 1 else 0
    val k2 = if (SIMPLEX_4D[c] >= 2) 1 else 0
    val k3 = if (SIMPLEX_4D[c++] >= 1) 1 else 0
    val l1 = if (SIMPLEX_4D[c] >= 3) 1 else 0
    val l2 = if (SIMPLEX_4D[c] >= 2) 1 else 0
    val l3 = if (SIMPLEX_4D[c] >= 1) 1 else 0


    val x1 = x0 - i1 + G4
    val y1 = y0 - j1 + G4
    val z1 = z0 - k1 + G4
    val w1 = w0 - l1 + G4
    val x2 = x0 - i2 + 2 * G4
    val y2 = y0 - j2 + 2 * G4
    val z2 = z0 - k2 + 2 * G4
    val w2 = w0 - l2 + 2 * G4
    val x3 = x0 - i3 + 3 * G4
    val y3 = y0 - j3 + 3 * G4
    val z3 = z0 - k3 + 3 * G4
    val w3 = w0 - l3 + 3 * G4
    val x4 = x0 - 1 + 4 * G4
    val y4 = y0 - 1 + 4 * G4
    val z4 = z0 - 1 + 4 * G4
    val w4 = w0 - 1 + 4 * G4

    val n0: Double
    val n1: Double
    val n2: Double
    val n3: Double
    val n4: Double

    t = 0.6 - x0 * x0 - y0 * y0 - z0 * z0 - w0 * w0
    if (t < 0) n0 = 0.0 else {
        t *= t
        n0 = t * t * gradCoord4D(seed, i, j, k, l, x0, y0, z0, w0)
    }
    t = 0.6 - x1 * x1 - y1 * y1 - z1 * z1 - w1 * w1
    if (t < 0) n1 = 0.0 else {
        t *= t
        n1 = t * t * gradCoord4D(seed, i + i1, j + j1, k + k1, l + l1, x1, y1, z1, w1)
    }
    t = 0.6 - x2 * x2 - y2 * y2 - z2 * z2 - w2 * w2
    if (t < 0) n2 = 0.0 else {
        t *= t
        n2 = t * t * gradCoord4D(seed, i + i2, j + j2, k + k2, l + l2, x2, y2, z2, w2)
    }
    t = 0.6 - x3 * x3 - y3 * y3 - z3 * z3 - w3 * w3
    if (t < 0) n3 = 0.0 else {
        t *= t
        n3 = t * t * gradCoord4D(seed, i + i3, j + j3, k + k3, l + l3, x3, y3, z3, w3)
    }
    t = 0.6 - x4 * x4 - y4 * y4 - z4 * z4 - w4 * w4
    if (t < 0) n4 = 0.0 else {
        t *= t
        n4 = t * t * gradCoord4D(seed, i + 1, j + 1, k + 1, l + 1, x4, y4, z4, w4)
    }

    return 27 * (n0 + n1 + n2 + n3 + n4)
}
