package org.openrndr.extra.noise

import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.mix

/**
 * Applies fractal gradient perturbation to a 3D position vector.
 *
 * This method perturbs the input position vector using a fractal noise pattern based on multiple
 * octaves of gradient noise. It combines parameters such as amplitude, frequency, lacunarity, and gain
 * to control the noise characteristics, while supporting custom interpolation.
 *
 * @param seed The initial seed value to generate the noise.
 * @param amplitude The initial magnitude of the displacement during the perturbation.
 * @param frequency The base frequency for the noise generation.
 * @param lacunarity The frequency multiplier between successive octaves.
 * @param gain The amplitude multiplier between successive octaves.
 * @param octaves The number of noise layers (octaves) to combine in the fractal calculation.
 * @param position The input 3D vector representing the position to perturb.
 * @param interpolator A function to apply smooth interpolation, typically used for gradient noise transitions.
 * @return The perturbed 3D position vector after applying the fractal gradient perturbation.
 */
fun gradientPerturbFractal(
    seed: Int, amplitude: Double = 1.0, frequency: Double = 2.0,
    lacunarity: Double = 2.0, gain: Double = 0.5,
    octaves: Int = 4, position: Vector3, interpolator: (Double) -> Double = ::quintic
): Vector3 {

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

/**
 * Perturbs a position vector in 3D space by applying a gradient noise algorithm.
 *
 * @param seed The seed value used for generating deterministic patterns.
 * @param amplitude The amplitude of the perturbation, which controls the scale of displacement.
 * @param frequency The frequency of the perturbation, which determines the scale of the noise.
 * @param position The original position vector to be perturbed.
 * @param interpolator The interpolation function used to smooth the noise transitions,
 *                     defaulting to the quintic function.
 * @return A new position vector that has been perturbed by the gradient noise algorithm.
 */
fun gradientPerturb(
    seed: Int,
    amplitude: Double,
    frequency: Double,
    position: Vector3,
    interpolator: (Double) -> Double = ::quintic
): Vector3 {
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

    var vec0 = Cell3D[(uhash3D(seed, x0, y0, z0) and 0xffU).toInt()]
    var vec1 = Cell3D[(uhash3D(seed, x1, y0, z0) and 0xffU).toInt()]

    var lx0x = mix(vec0.x, vec1.x, xs)
    var ly0x = mix(vec0.y, vec1.y, xs)
    var lz0x = mix(vec0.z, vec1.z, xs)

    vec0 = Cell3D[(uhash3D(seed, x0, y1, z0) and 0xffU).toInt()]
    vec1 = Cell3D[(uhash3D(seed, x1, y1, z0) and 0xffU).toInt()]

    var lx1x = mix(vec0.x, vec1.x, xs)
    var ly1x = mix(vec0.y, vec1.y, xs)
    var lz1x = mix(vec0.z, vec1.z, xs)

    val lx0y = mix(lx0x, lx1x, ys)
    val ly0y = mix(ly0x, ly1x, ys)
    val lz0y = mix(lz0x, lz1x, ys)

    vec0 = Cell3D[(uhash3D(seed, x0, y0, z1) and 0xffU).toInt()]
    vec1 = Cell3D[(uhash3D(seed, x1, y0, z1) and 0xffU).toInt()]

    lx0x = mix(vec0.x, vec1.x, xs)
    ly0x = mix(vec0.y, vec1.y, xs)
    lz0x = mix(vec0.z, vec1.z, xs)

    vec0 = Cell3D[(uhash3D(seed, x0, y1, z1) and 0xffU).toInt()]
    vec1 = Cell3D[(uhash3D(seed, x1, y1, z1) and 0xffU).toInt()]

    lx1x = mix(vec0.x, vec1.x, xs)
    ly1x = mix(vec0.y, vec1.y, xs)
    lz1x = mix(vec0.z, vec1.z, xs)

    return position + Vector3(
        mix(lx0y, mix(lx0x, lx1x, ys), zs),
        mix(ly0y, mix(ly0x, ly1x, ys), zs),
        mix(lz0y, mix(lz0x, lz1x, ys), zs)
    ) * amplitude
}

/**
 * Applies fractal gradient perturbation to the given position vector using the specified parameters.
 * This method introduces multiple layers of noise to create a fractal effect by perturbing the position iteratively
 * based on the number of octaves, frequency, and amplitude adjustments.
 *
 * @param seed An integer seed used to initialize the random number generator for noise generation.
 * @param amplitude The initial amplitude of the perturbation. Higher values result in larger displacements.
 * @param frequency The initial frequency of the noise. Higher values increase the density of the noise variation.
 * @param lacunarity The rate at which the frequency increases with each octave.
 * @param gain The rate at which the amplitude decreases with each octave.
 * @param octaves The number of fractal noise layers to apply. More octaves increase detail.
 * @param position A 2D vector representing the original point to be perturbed.
 * @param interpolator A function that defines how to interpolate values smoothly. Defaults to the quintic function.
 * @return A 2D vector representing the perturbed position after applying the fractal gradient noise.
 */
fun gradientPerturbFractal(
    seed: Int, amplitude: Double = 1.0, frequency: Double = 2.0,
    lacunarity: Double = 2.0, gain: Double = 0.5,
    octaves: Int = 4, position: Vector2, interpolator: (Double) -> Double = ::quintic
): Vector2 {

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

/**
 * Calculates a perturbed position based on gradient noise.
 *
 * @param seed An integer seed value used to initialize the pseudo-random number generator.
 * @param amplitude A double value that determines the strength of the perturbation applied to the position.
 * @param frequency A double value that defines how frequent the perturbation occurs in the space.
 * @param position A 2D vector specifying the initial position to perturb.
 * @param interpolator A function used for interpolation between gradient values, defaults to the quintic interpolation function.
 * @return A 2D vector that represents the new perturbed position.
 */
fun gradientPerturb(
    seed: Int,
    amplitude: Double,
    frequency: Double,
    position: Vector2,
    interpolator: (Double) -> Double = ::quintic
): Vector2 {
    val xf = position.x * frequency
    val yf = position.y * frequency
    val x0 = xf.fastFloor()
    val y0 = yf.fastFloor()
    val x1 = x0 + 1
    val y1 = y0 + 1

    val xs = interpolator(xf - x0)
    val ys = interpolator(yf - y0)

    var vec0 = Cell2D[(uhash2D(seed, x0, y0) and 0xffU).toInt()]
    var vec1 = Cell2D[(uhash2D(seed, x1, y0) and 0xffU).toInt()]

    val lx0x = mix(vec0.x, vec1.x, xs)
    val ly0x = mix(vec0.y, vec1.y, xs)

    vec0 = Cell2D[(uhash2D(seed, x0, y1) and 0xffU).toInt()]
    vec1 = Cell2D[(uhash2D(seed, x1, y1) and 0xffU).toInt()]

    val lx1x = mix(vec0.x, vec1.x, xs)
    val ly1x = mix(vec0.y, vec1.y, xs)

    return position + Vector2(
        mix(lx0x, lx1x, ys),
        mix(ly0x, ly1x, ys)
    ) * amplitude

}