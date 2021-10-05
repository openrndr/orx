package org.openrndr.extra.noise

import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.mix

fun gradientPerturbFractal(seed: Int, amplitude: Double = 1.0, frequency: Double = 2.0,
                           lacunarity: Double = 2.0, gain: Double = 0.5,
                           octaves: Int = 4, position: Vector3, interpolator: (Double) -> Double = ::quintic): Vector3 {

    var seed_ = seed
    var amplitude_ = amplitude
    var frequency_ = frequency

    var p = gradientPerturb(seed_, amplitude_, frequency_, position, interpolator)
    for (i in 0 until octaves) {
        frequency_ *= lacunarity
        amplitude_ *= gain
        seed_++
        p = gradientPerturb(seed_, amplitude_, frequency_, p, interpolator)
    }
    return p
}

fun gradientPerturb(seed: Int, amplitude: Double, frequency: Double, position: Vector3, interpolator: (Double) -> Double = ::quintic): Vector3 {
    val xf = position.x * frequency
    val yf = position.y * frequency
    val zf = position.z * frequency

    val x0 = xf.fastFloor()
    val y0 = yf.fastFloor()
    val z0 = zf.fastFloor()
    val x1 = x0 + 1
    val y1 = y0 + 1
    val z1 = z0 + 1

    val xs = interpolator(xf - x0)
    val ys = interpolator(yf - y0)
    val zs = interpolator(zf - z0)

    var vec0 = Cell3D[hash3D(seed, x0, y0, z0) and 0xff]
    var vec1 = Cell3D[hash3D(seed, x1, y0, z0) and 0xff]

    var lx0x = mix(vec0.x, vec1.x, xs)
    var ly0x = mix(vec0.y, vec1.y, xs)
    var lz0x = mix(vec0.z, vec1.z, xs)

    vec0 = Cell3D[hash3D(seed, x0, y1, z0) and 0xff]
    vec1 = Cell3D[hash3D(seed, x1, y1, z0) and 0xff]

    var lx1x = mix(vec0.x, vec1.x, xs)
    var ly1x = mix(vec0.y, vec1.y, xs)
    var lz1x = mix(vec0.z, vec1.z, xs)

    val lx0y = mix(lx0x, lx1x, ys)
    val ly0y = mix(ly0x, ly1x, ys)
    val lz0y = mix(lz0x, lz1x, ys)

    vec0 = Cell3D[hash3D(seed, x0, y0, z1) and 0xff]
    vec1 = Cell3D[hash3D(seed, x1, y0, z1) and 0xff]

    lx0x = mix(vec0.x, vec1.x, xs)
    ly0x = mix(vec0.y, vec1.y, xs)
    lz0x = mix(vec0.z, vec1.z, xs)

    vec0 = Cell3D[hash3D(seed, x0, y1, z1) and 0xff]
    vec1 = Cell3D[hash3D(seed, x1, y1, z1) and 0xff]

    lx1x = mix(vec0.x, vec1.x, xs)
    ly1x = mix(vec0.y, vec1.y, xs)
    lz1x = mix(vec0.z, vec1.z, xs)

    return position + Vector3(
            mix(lx0y, mix(lx0x, lx1x, ys), zs),
            mix(ly0y, mix(ly0x, ly1x, ys), zs),
            mix(lz0y, mix(lz0x, lz1x, ys), zs)
    ) * amplitude
}

fun gradientPerturbFractal(seed: Int, amplitude: Double = 1.0, frequency: Double = 2.0,
                           lacunarity: Double = 2.0, gain: Double = 0.5,
                           octaves: Int = 4, position: Vector2, interpolator: (Double) -> Double = ::quintic): Vector2 {

    var seed_ = seed
    var amplitude_ = amplitude
    var frequency_ = frequency

    var p = gradientPerturb(seed_, amplitude_, frequency_, position, interpolator)
    for (i in 0 until octaves) {
        frequency_ *= lacunarity
        amplitude_ *= gain
        seed_++
        p = gradientPerturb(seed_, amplitude_, frequency_, p, interpolator)
    }
    return p
}

fun gradientPerturb(seed: Int, amplitude: Double, frequency: Double, position: Vector2, interpolator: (Double) -> Double = ::quintic): Vector2 {
    val xf = position.x * frequency
    val yf = position.y * frequency
    val x0 = xf.fastFloor()
    val y0 = yf.fastFloor()
    val x1 = x0 + 1
    val y1 = y0 + 1

    val xs = interpolator(xf - x0)
    val ys = interpolator(yf - y0)

    var vec0 = Cell2D[hash2D(seed, x0, y0) and 0xff]
    var vec1 = Cell2D[hash2D(seed, x1, y0) and 0xff]

    val lx0x = mix(vec0.x, vec1.x, xs)
    val ly0x = mix(vec0.y, vec1.y, xs)

    vec0 = Cell2D[hash2D(seed, x0, y1) and 0xff]
    vec1 = Cell2D[hash2D(seed, x1, y1) and 0xff]

    val lx1x = mix(vec0.x, vec1.x, xs)
    val ly1x = mix(vec0.y, vec1.y, xs)

    return position + Vector2(
            mix(lx0x, lx1x, ys),
            mix(ly0x, ly1x, ys)
    ) * amplitude

}