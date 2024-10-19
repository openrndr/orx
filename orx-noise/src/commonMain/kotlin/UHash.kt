package org.openrndr.extra.noise

/**
 * uniform hash function
 * https://nullprogram.com/blog/2018/07/31/
 */
fun uhash11(x: UInt): UInt {
    var a = x
    a = a xor (a shr 16)
    a *= 0x7feb352du
    a = a xor (a shr 15)
    a *= 0x846ca68bu
    a = a xor (a shr 16)
    return a
}

fun uhash1D(seed: Int, x: Int): UInt = uhash11(seed.toUInt() + uhash11(x.toUInt()))
fun uhash2D(seed: Int, x: Int, y: Int): UInt = uhash11(seed.toUInt() + uhash11(y.toUInt() + uhash11(x.toUInt())))
fun uhash3D(seed: Int, x: Int, y: Int, z: Int): UInt =
    uhash11(seed.toUInt() + uhash11(z.toUInt() + uhash11(x.toUInt() + uhash11(y.toUInt()))))

fun uhash4D(seed: Int, x: Int, y: Int, z: Int, w: Int): UInt =
    uhash11(seed.toUInt() + uhash11(z.toUInt() + uhash11(z.toUInt() + uhash11(x.toUInt() + uhash11(y.toUInt())))))


fun fhash1D(seed: Int, x: Int): Double = uhash1D(seed, x).toDouble() / UInt.MAX_VALUE.toDouble()
fun fhash2D(seed: Int, x: Int, y: Int): Double = uhash2D(seed, x, y).toDouble() / UInt.MAX_VALUE.toDouble()
fun fhash3D(seed: Int, x: Int, y: Int, z: Int): Double =
    uhash3D(seed, x, y, z).toDouble() / UInt.MAX_VALUE.toDouble()

fun fhash4D(seed: Int, x: Int, y: Int, z: Int, w :Int): Double =
    uhash4D(seed, x, y, z, w).toDouble() / UInt.MAX_VALUE.toDouble()


fun fshash1D(seed: Int, x: Int): Double = 2.0 * uhash1D(seed, x).toDouble() / UInt.MAX_VALUE.toDouble() - 1.0
fun fshash2D(seed: Int, x: Int, y: Int): Double = 2.0 * uhash2D(seed, x, y).toDouble() / UInt.MAX_VALUE.toDouble() - 1.0
fun fshash3D(seed: Int, x: Int, y: Int, z: Int): Double =
    2.0*uhash3D(seed, x, y, z).toDouble() / UInt.MAX_VALUE.toDouble() - 1.0

fun fshash4D(seed: Int, x: Int, y: Int, z: Int, w :Int): Double =
    2.0 * uhash4D(seed, x, y, z, w).toDouble() / UInt.MAX_VALUE.toDouble() - 1.0
