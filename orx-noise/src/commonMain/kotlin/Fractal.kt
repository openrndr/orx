package org.openrndr.extra.noise

import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import kotlin.math.abs

/**
 * Generates a fractal Brownian motion (fbm) value based on the provided noise function.
 *
 * @param seed The seed value for the noise function, used to ensure consistent output for the same inputs.
 * @param position The 4D vector representing the coordinates for evaluating noise.
 * @param noise A function that evaluates 4D noise based on a seed and coordinates (x, y, z, w).
 * @param octaves The number of noise layers to combine. Higher values produce more detailed noise patterns.
 * @param lacunarity The frequency multiplier for each successive octave.
 * @param gain The amplitude multiplier for each successive octave.
 *
 * @return A Double value representing the combined noise, with frequency and amplitude adjusted per octave.
 */
inline fun fbm(
    seed: Int, position: Vector4, crossinline noise: (Int, Double, Double, Double, Double) -> Double,
    octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5
) =
    fbm(seed, position.x, position.y, position.z, position.w, noise, octaves, lacunarity, gain)

/**
 * Computes fractal Brownian motion (fBm) using the given noise function.
 *
 * @param seed An integer seed that initializes the noise generation.
 * @param x The x-coordinate for the initial noise function.
 * @param y The y-coordinate for the initial noise function.
 * @param z The z-coordinate for the initial noise function.
 * @param w The w-coordinate for the initial noise function.
 * @param noise A higher-order function that generates noise based on the seed and coordinate inputs.
 * @param octaves The number of iterations to apply the noise function for calculating fBm. Default is 8.
 * @param lacunarity The frequency multiplier applied at each octave. Default is 0.5.
 * @param gain The amplitude multiplier applied at each octave. Default is 0.5.
 * @return The resultant fractal Brownian motion value as a Double.
 */
inline fun fbm(
    seed: Int,
    x: Double,
    y: Double,
    z: Double,
    w: Double,
    crossinline noise: (Int, Double, Double, Double, Double) -> Double,
    octaves: Int = 8,
    lacunarity: Double = 0.5,
    gain: Double = 0.5
): Double {
    var sum = noise(seed, x, y, z, w)
    var amp = 1.0

    var lx = x
    var ly = y
    var lz = z
    var lw = w
    for (i in 1 until octaves) {
        lx *= lacunarity
        ly *= lacunarity
        lz *= lacunarity
        lw *= lacunarity
        amp *= gain
        sum += noise(seed + i, lx, ly, lz, lw) * amp
    }
    return sum
}

/**
 * Computes fractal Brownian motion (fBm) at a specific 3D position using a given noise function.
 *
 * @param seed The base seed for the noise function, used to initialize the random sequence.
 * @param position The 3D position at which to evaluate the noise.
 * @param noise A lambda function that generates noise based on the seed and 3D coordinates.
 * @param octaves The number of iterations to compute the fBm. Defaults to 8.
 * @param lacunarity Controls the frequency of successive octaves. Defaults to 0.5.
 * @param gain Controls the amplitude of successive octaves. Defaults to 0.5.
 * @return A Double representing the computed fractal Brownian motion value at the given position.
 */
inline fun fbm(
    seed: Int, position: Vector3, crossinline noise: (Int, Double, Double, Double) -> Double,
    octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5
) =
    fbm(seed, position.x, position.y, position.z, noise, octaves, lacunarity, gain)

/**
 * Computes fractional Brownian motion (fBm) for a given seed, coordinates, and noise function.
 *
 * fBm is a procedural noise function that generates continuous, fractal-like patterns by
 * combining multiple octaves of noise at different frequencies and amplitudes.
 *
 * @param seed The base seed value for the noise function.
 * @param x The x-coordinate in the noise space.
 * @param y The y-coordinate in the noise space.
 * @param z The z-coordinate in the noise space.
 * @param noise A function that generates noise values given a seed and coordinates x, y, z.
 * @param octaves The number of noise octaves to combine. Defaults to 8.
 * @param lacunarity The factor by which the frequency is increased for each octave. Defaults to 0.5.
 * @param gain The factor by which the amplitude is decreased for each octave. Defaults to 0.5.
 * @return The computed fBm value as a Double.
 */
inline fun fbm(
    seed: Int, x: Double, y: Double, z: Double, crossinline noise: (Int, Double, Double, Double) -> Double,
    octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5
): Double {
    var sum = noise(seed, x, y, z)
    var amp = 1.0

    var lx = x
    var ly = y
    var lz = z
    for (i in 1 until octaves) {
        lx *= lacunarity
        ly *= lacunarity
        lz *= lacunarity
        amp *= gain
        sum += noise(seed + i, lx, ly, lz) * amp
    }
    return sum
}

/**
 * Computes a fractal Brownian motion (FBM) value for a given 2D position using the provided noise function.
 *
 * @param seed The base seed for the noise function.
 * @param position The 2D vector representing the position in space.
 * @param noise A function that generates noise values, taking the seed, x-coordinate, and y-coordinate as inputs.
 * @param octaves The number of layers of noise to generate. Defaults to 8.
 * @param lacunarity The frequency multiplier for each layer of noise. Defaults to 0.5.
 * @param gain The amplitude multiplier for each layer of noise. Defaults to 0.5.
 * @return The computed FBM value for the given inputs.
 */
inline fun fbm(
    seed: Int, position: Vector2, crossinline noise: (Int, Double, Double) -> Double,
    octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5
) =
    fbm(seed, position.x, position.y, noise, octaves, lacunarity, gain)

/**
 * Generates fractional Brownian motion (fBm) using a base noise function.
 * fBm is a technique to produce fractal-like procedural textures or terrains.
 *
 * @param seed Seed value for the noise function to ensure reproducibility.
 * @param x The x-coordinate input for the noise function.
 * @param y The y-coordinate input for the noise function.
 * @param noise A base noise function that takes a seed, x, and y, and returns a noise value.
 * @param octaves The number of noise layers (also referred to as octaves) to combine. Default is 8.
 * @param lacunarity The frequency multiplier for each successive octave. Default is 0.5.
 * @param gain The amplitude multiplier for each successive octave. Default is 0.5.
 * @return The resulting fractional Brownian motion value.
 */
inline fun fbm(
    seed: Int, x: Double, y: Double, crossinline noise: (Int, Double, Double) -> Double,
    octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5
): Double {
    var sum = noise(seed, x, y)
    var amp = 1.0

    var lx = x
    var ly = y
    for (i in 1 until octaves) {
        lx *= lacunarity
        ly *= lacunarity
        amp *= gain
        sum += noise(seed + i, lx, ly) * amp
    }
    return sum
}

/**
 * Computes the fractional Brownian motion (FBM) value for a given input using a specified noise function.
 *
 * @param seed The seed value to initialize the noise function.
 * @param x The input value for which the FBM is calculated.
 * @param noise The noise function that generates noise values based on the seed and input.
 * @param octaves The number of successive noise layers to combine. Default is 8.
 * @param lacunarity The factor by which the frequency increases for each successive octave. Default is 0.5.
 * @param gain The factor by which the amplitude decreases for each successive octave. Default is 0.5.
 * @return The computed FBM value as a Double.
 */
inline fun fbm(
    seed: Int, x: Double, crossinline noise: (Int, Double) -> Double,
    octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5
): Double {
    var sum = noise(seed, x)
    var amp = 1.0

    var lx = x
    for (i in 1 until octaves) {
        lx *= lacunarity
        amp *= gain
        sum += noise(seed + i, lx) * amp
    }
    return sum
}

internal inline fun fbmFunc1D(
    crossinline noise: (Int, Double) -> Double,
    octaves: Int = 8,
    lacunarity: Double = 0.5,
    gain: Double = 0.5
): (Int, Double) -> Double {
    return { seed, x ->
        fbm(seed, x, noise, octaves, lacunarity, gain)
    }
}

internal inline fun fbmFunc2D(
    crossinline noise: (Int, Double, Double) -> Double,
    octaves: Int = 8,
    lacunarity: Double = 0.5,
    gain: Double = 0.5
): (Int, Double, Double) -> Double {
    return { seed, x, y ->
        fbm(seed, x, y, noise, octaves, lacunarity, gain)
    }
}

internal inline fun fbmFunc3D(
    crossinline noise: (Int, Double, Double, Double) -> Double,
    octaves: Int = 8,
    lacunarity: Double = 0.5,
    gain: Double = 0.5
): (Int, Double, Double, Double) -> Double {
    return { seed, x, y, z ->
        fbm(seed, x, y, z, noise, octaves, lacunarity, gain)
    }
}

internal inline fun fbmFunc4D(
    crossinline noise: (Int, Double, Double, Double, Double) -> Double,
    octaves: Int = 8,
    lacunarity: Double = 0.5,
    gain: Double = 0.5
): (Int, Double, Double, Double, Double) -> Double {
    return { seed, x, y, z, w ->
        fbm(seed, x, y, z, w, noise, octaves, lacunarity, gain)
    }
}

/**
 * Generates a 4D Billow noise value, which is a type of fractal noise that emphasizes the absolute value of noise layers.
 *
 * @param seed The seed value to initialize the noise generator.
 * @param position The 4D position vector (x, y, z, w) where the noise will be sampled.
 * @param noise A function that represents the noise generation algorithm. Accepts the seed and 4D coordinates as input.
 * @param octaves The number of noise layers applied to produce the final output. Defaults to 8.
 * @param lacunarity The frequency multiplier for each successive octave. Defaults to 0.5.
 * @param gain The amplitude multiplier for each successive octave. Defaults to 0.5.
 * @return The resulting 4D Billow noise value.
 */
inline fun billow(
    seed: Int, position: Vector4, crossinline noise: (Int, Double, Double, Double, Double) -> Double,
    octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5
) = billow(
    seed, position.x, position.y,
    position.z, position.w, noise, octaves, lacunarity, gain
)

/**
 * Generates a 4D billow noise value, which is a type of fractal noise that emphasizes the absolute value of noise layers.
 *
 * @param seed The seed value to initialize the noise generation.
 * @param x The x-coordinate in the noise space.
 * @param y The y-coordinate in the noise space.
 * @param z The z-coordinate in the noise space.
 * @param w The w-coordinate in the noise space.
 * @param noise A function that generates the base noise value given a seed and coordinates.
 * @param octaves The number of noise layers to combine. Default is 8.
 * @param lacunarity The factor by which the frequency of each octave is scaled. Default is 0.5.
 * @param gain The factor by which the amplitude of each octave is scaled. Default is 0.5.
 * @return A `Double` representing the combined billow noise value based on the input parameters.
 */
inline fun billow(
    seed: Int,
    x: Double,
    y: Double,
    z: Double,
    w: Double,
    crossinline noise: (Int, Double, Double, Double, Double) -> Double,
    octaves: Int = 8,
    lacunarity: Double = 0.5,
    gain: Double = 0.5
): Double {
    var sum = abs(noise(seed, x, y, z, w) * 2.0 - 1.0)
    var amp = 1.0

    var x = x
    var y = y
    var z = z
    var w = w
    for (i in 1 until octaves) {
        x *= lacunarity
        y *= lacunarity
        z *= lacunarity
        w *= lacunarity
        amp *= gain
        sum += abs(noise(seed + i, x, y, z, w) * 2.0 - 1.0) * amp
    }
    return sum
}

/**
 * Generates a 3D Billow noise value, which is a type of fractal noise that emphasizes the absolute value of noise layers.
 *
 * @param seed The seed value for the noise function to ensure deterministic results.
 * @param position A 3D vector representing the position for which the noise is calculated.
 * @param noise A function that generates 3D noise given a seed and coordinates.
 * @param octaves The number of layers of noise applied for detail (default is 8).
 * @param lacunarity The frequency multiplier for successive noise layers (default is 0.5).
 * @param gain The amplitude multiplier for successive noise layers (default is 0.5).
 * @return The resultant billow noise value at the specified position.
 */
inline fun billow(
    seed: Int, position: Vector3, crossinline noise: (Int, Double, Double, Double) -> Double,
    octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5
) =
    billow(seed, position.x, position.y, position.z, noise, octaves, lacunarity, gain)

/**
 * Generates a fractal noise pattern using the Billow algorithm.
 *
 * The Billow algorithm is a variation of the Perlin/simplex noise pattern, but it produces ridged
 * patterns by taking the absolute value of the noise. This method combines multiple layers of noise
 * (octaves) to produce a fractal appearance.
 *
 * @param seed The initial seed for the noise generation. Used as a basis for reproducibility.
 * @param x The x-coordinate of the point to generate noise for.
 * @param y The y-coordinate of the point to generate noise for.
 * @param z The z-coordinate of the point to generate noise for.
 * @param noise A function that takes a seed and three coordinates (x, y, z) and generates a noise value for that point.
 * @param octaves The number of noise layers to combine. Default value is 8.
 * @param lacunarity The scaling factor for the input coordinates between octaves. Higher values increase detail. Default value is 0.5.
 * @param gain The amplitude reduction factor between octaves. Lower values reduce the influence of higher octaves. Default value is 0.5.
 * @return A Double representing the fractal noise value at the specified coordinates using the Billow algorithm.
 */
inline fun billow(
    seed: Int, x: Double, y: Double, z: Double, crossinline noise: (Int, Double, Double, Double) -> Double,
    octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5
): Double {
    var sum = abs(noise(seed, x, y, z) * 2.0 - 1.0)
    var amp = 1.0

    var x = x
    var y = y
    var z = z
    for (i in 1 until octaves) {
        x *= lacunarity
        y *= lacunarity
        z *= lacunarity
        amp *= gain
        sum += abs(noise(seed + i, x, y, z) * 2.0 - 1.0) * amp
    }
    return sum
}

/**
 * Generates a 2D Billow noise value, which is a type of fractal noise that emphasizes the absolute value of noise layers.
 *
 * @param seed The seed for the noise function, used to generate repeatable patterns.
 * @param position A 2D vector representing the coordinates at which noise is generated.
 * @param noise A function that computes noise for a given seed and coordinates (x, y).
 * @param octaves The number of noise layers to combine. Higher values provide more detail. Default is 8.
 * @param lacunarity The frequency multiplier for successive noise layers. Default is 0.5.
 * @param gain The amplitude multiplier for successive noise layers. Default is 0.5.
 * @return A combined noise value for the specified position.
 */
inline fun billow(
    seed: Int, position: Vector2, crossinline noise: (Int, Double, Double) -> Double,
    octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5
) =
    billow(seed, position.x, position.y, noise, octaves, lacunarity, gain)

/**
 * Generates a 2D Billow noise value, which is a type of fractal noise that emphasizes the absolute value of noise layers.
 *
 * @param seed An integer seed value used to initialize the noise generation.
 * @param x The x-coordinate for the noise generation.
 * @param y The y-coordinate for the noise generation.
 * @param noise A function that generates noise values given a seed and coordinates.
 * @param octaves The number of iterations to perform to calculate the noise. Default is 8.
 * @param lacunarity A multiplier applied to the coordinates at each octave to adjust frequency. Default is 0.5.
 * @param gain A multiplier applied to the amplitude at each octave to adjust magnitude. Default is 0.5.
 * @return The computed billow noise value as a Double.
 */
inline fun billow(
    seed: Int, x: Double, y: Double, crossinline noise: (Int, Double, Double) -> Double,
    octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5
): Double {
    var sum = abs(noise(seed, x, y) * 2.0 - 1.0)
    var amp = 1.0

    var x = x
    var y = y
    for (i in 1 until octaves) {
        x *= lacunarity
        y *= lacunarity
        amp *= gain
        sum += abs(noise(seed + i, x, y) * 2.0 - 1.0) * amp
    }
    return sum
}

/**
 * Generates a Billow noise value, which is a type of fractal noise that emphasizes the absolute value of noise layers.
 *
 * @param seed The initial seed value used for noise generation.
 * @param x The input value, typically representing a point in space or time.
 * @param noise A function that generates noise based on the given seed and x value.
 * @param octaves The number of layers of noise to generate. Higher values result in more detail. Defaults to 8.
 * @param lacunarity The factor by which the frequency of the noise increases with each octave. Defaults to 0.5.
 * @param gain The factor by which the amplitude of the noise decreases with each octave. Defaults to 0.5.
 * @return A Double value representing the generated fractal noise.
 */
inline fun billow(
    seed: Int, x: Double, crossinline noise: (Int, Double) -> Double,
    octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5
): Double {
    var sum = abs(noise(seed, x) * 2.0 - 1.0)
    var amp = 1.0

    var x = x
    for (i in 1 until octaves) {
        x *= lacunarity
        amp *= gain
        sum += abs(noise(seed + i, x) * 2.0 - 1.0) * amp
    }
    return sum
}

internal inline fun billowFunc1D(
    crossinline noise: (Int, Double) -> Double,
    octaves: Int = 8,
    lacunarity: Double = 0.5,
    gain: Double = 0.5
): (Int, Double) -> Double {
    return { seed, x ->
        billow(seed, x, noise, octaves, lacunarity, gain)
    }
}

internal inline fun billowFunc2D(
    crossinline noise: (Int, Double, Double) -> Double,
    octaves: Int = 8,
    lacunarity: Double = 0.5,
    gain: Double = 0.5
): (Int, Double, Double) -> Double {
    return { seed, x, y ->
        billow(seed, x, y, noise, octaves, lacunarity, gain)
    }
}

internal inline fun billowFunc3D(
    crossinline noise: (Int, Double, Double, Double) -> Double,
    octaves: Int = 8,
    lacunarity: Double = 0.5,
    gain: Double = 0.5
): (Int, Double, Double, Double) -> Double {
    return { seed, x, y, z ->
        billow(seed, x, y, z, noise, octaves, lacunarity, gain)
    }
}

internal inline fun billowFunc4D(
    crossinline noise: (Int, Double, Double, Double, Double) -> Double,
    octaves: Int = 8,
    lacunarity: Double = 0.5,
    gain: Double = 0.5
): (Int, Double, Double, Double, Double) -> Double {
    return { seed, x, y, z, w ->
        billow(seed, x, y, z, w, noise, octaves, lacunarity, gain)
    }
}

/**
 * Generates a 4D rigid multi-octave noise value based on the provided parameters and noise function.
 *
 * @param seed A base value used to seed the noise function.
 * @param position A 4D vector specifying the coordinates (x, y, z, w) of the input point.
 * @param noise A callback function that generates a noise value based on the given parameters: seed, x, y, z, and w.
 * @param octaves The number of noise layers (octaves) to combine to achieve the rigid appearance. Defaults to 8.
 * @param lacunarity The factor by which the frequency increases between successive octaves. Defaults to 0.5.
 * @param gain The factor by which the amplitude decreases between successive octaves. Defaults to 0.5.
 */
inline fun rigid(
    seed: Int, position: Vector4, crossinline noise: (Int, Double, Double, Double, Double) -> Double,
    octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5
) =
    rigid(seed, position.x, position.y, position.z, position.w, noise, octaves, lacunarity, gain)

/**
 * Generates a 4D rigid multi-octave noise value based on the provided parameters and noise function.
 *
 * @param seed A base value used to seed the noise function.
 * @param x The x-coordinate of the input point.
 * @param y The y-coordinate of the input point.
 * @param z The z-coordinate of the input point.
 * @param w The w-coordinate of the input point.
 * @param noise A callback function that generates a noise value based on the given parameters: seed, x, y, z, and w.
 * @param octaves The number of noise layers (octaves) to combine to achieve the rigid appearance. Defaults to 8.
 * @param lacunarity The factor by which the frequency increases between successive octaves. Defaults to 0.5.
 * @param gain The factor by which the amplitude decreases between successive octaves. Defaults to 0.5.
 * @return A double precision value representing the computed rigid multi-octave noise.
 */
inline fun rigid(
    seed: Int,
    x: Double,
    y: Double,
    z: Double,
    w: Double,
    crossinline noise: (Int, Double, Double, Double, Double) -> Double,
    octaves: Int = 8,
    lacunarity: Double = 0.5,
    gain: Double = 0.5
): Double {
    var sum = 1.0 - abs(noise(seed, x, y, z, w))
    var amp = 1.0

    var x = x
    var y = y
    var z = z
    var w = w
    for (i in 1 until octaves) {
        x *= lacunarity
        y *= lacunarity
        z *= lacunarity
        w *= lacunarity
        amp *= gain
        sum -= (1.0 - abs(noise(seed + i, x, y, z, w))) * amp
    }
    return sum
}

/**
 * Computes a multi-octave rigid noise value for a given position.
 *
 * @param seed The seed value for the noise generation.
 * @param position A 3D vector representing the spatial coordinates.
 * @param noise A function for generating noise, taking seed and three Double coordinates (x, y, z).
 * @param octaves The number of noise octaves to compute. Defaults to 8.
 * @param lacunarity The frequency multiplier between successive octaves. Defaults to 0.5.
 * @param gain The amplitude multiplier between successive octaves. Defaults to 0.5.
 * @return The computed rigid noise value.
 */
inline fun rigid(
    seed: Int, position: Vector3, crossinline noise: (Int, Double, Double, Double) -> Double,
    octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5
) =
    rigid(seed, position.x, position.y, position.z, noise, octaves, lacunarity, gain)

/**
 * Generates a rigid multi-fractal noise value based on the given parameters.
 *
 * @param seed The seed value for the noise generation.
 * @param x The x-coordinate of the point for noise calculation.
 * @param y The y-coordinate of the point for noise calculation.
 * @param z The z-coordinate of the point for noise calculation.
 * @param noise A function that generates noise values based on the given seed and coordinates.
 * @param octaves The number of iterations or layers to apply for generating the noise. Default is 8.
 * @param lacunarity The frequency multiplier for each octave. Default is 0.5.
 * @param gain The amplitude multiplier for each octave. Default is 0.5.
 * @return The computed rigid multi-fractal noise value.
 */
inline fun rigid(
    seed: Int, x: Double, y: Double, z: Double, crossinline noise: (Int, Double, Double, Double) -> Double,
    octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5
): Double {
    var sum = 1.0 - abs(noise(seed, x, y, z))
    var amp = 1.0

    var x = x
    var y = y
    var z = z
    for (i in 1 until octaves) {
        x *= lacunarity
        y *= lacunarity
        z *= lacunarity
        amp *= gain
        sum -= (1.0 - abs(noise(seed + i, x, y, z))) * amp
    }
    return sum
}

/**
 * Computes a value based on a rigid multi-fractal noise function using a 2D position.
 *
 * @param seed The seed value for the noise function.
 * @param position The 2D vector representing the position for the noise function.
 * @param noise A higher-order function that generates noise values based on a seed, x-coordinate, and y-coordinate.
 * @param octaves The number of iterations to compute the rigid fractal noise, default is 8.
 * @param lacunarity The frequency multiplier for each octave, default is 0.5.
 * @param gain The amplitude multiplier for each octave, default is 0.5.
 * @return The computed rigid fractal noise value.
 */
inline fun rigid(
    seed: Int, position: Vector2, crossinline noise: (Int, Double, Double) -> Double,
    octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5
) =
    rigid(seed, position.x, position.y, noise, octaves, lacunarity, gain)

/**
 * Computes a value based on a rigid multi-fractal noise function.
 *
 * @param seed The seed value for the noise function.
 * @param x The x-coordinate for the noise function.
 * @param y The y-coordinate for the noise function.
 * @param noise A higher-order function that generates noise values based on a seed, x, and y.
 * @param octaves The number of iterations to compute the rigid fractal noise, default is 8.
 * @param lacunarity The frequency multiplier for each octave, default is 0.5.
 * @param gain The amplitude multiplier for each octave, default is 0.5.
 * @return The computed rigid fractal noise value.
 */
inline fun rigid(
    seed: Int, x: Double, y: Double, crossinline noise: (Int, Double, Double) -> Double,
    octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5
): Double {
    var sum = 1.0 - abs(noise(seed, x, y))
    var amp = 1.0

    var x = x
    var y = y
    for (i in 1 until octaves) {
        x *= lacunarity
        y *= lacunarity
        amp *= gain
        sum -= (1.0 - abs(noise(seed + i, x, y))) * amp
    }
    return sum
}

/**
 * Generates a rigid noise value using fractional Brownian motion (fBm) by combining multiple layers of noise.
 *
 * @param seed An integer value used to seed the noise generation for reproducibility.
 * @param x The input coordinate for which the noise value is computed.
 * @param noise A function that generates the base noise value, taking an integer seed and a double coordinate as input.
 * @param octaves The number of layers of noise to combine. Default is 8.
 * @param lacunarity The factor by which the frequency increases for each subsequent layer of noise. Default is 0.5.
 * @param gain The factor by which the amplitude decreases for each subsequent layer of noise. Default is 0.5.
 * @return A double value representing the computed rigid noise value.
 */
inline fun rigid(
    seed: Int, x: Double, crossinline noise: (Int, Double) -> Double,
    octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5
): Double {
    var sum = 1.0 - abs(noise(seed, x))
    var amp = 1.0

    var x = x
    for (i in 1 until octaves) {
        x *= lacunarity
        amp *= gain
        sum -= (1.0 - abs(noise(seed + i, x))) * amp
    }
    return sum
}

internal inline fun rigidFunc1D(
    crossinline noise: (Int, Double) -> Double,
    octaves: Int = 8,
    lacunarity: Double = 0.5,
    gain: Double = 0.5
): (Int, Double) -> Double {
    return { seed, x ->
        rigid(seed, x, noise, octaves, lacunarity, gain)
    }
}

internal inline fun rigidFunc2D(
    crossinline noise: (Int, Double, Double) -> Double,
    octaves: Int = 8,
    lacunarity: Double = 0.5,
    gain: Double = 0.5
): (Int, Double, Double) -> Double {
    return { seed, x, y ->
        rigid(seed, x, y, noise, octaves, lacunarity, gain)
    }
}

internal inline fun rigidFunc3D(
    crossinline noise: (Int, Double, Double, Double) -> Double,
    octaves: Int = 8,
    lacunarity: Double = 0.5,
    gain: Double = 0.5
): (Int, Double, Double, Double) -> Double {
    return { seed, x, y, z ->
        rigid(seed, x, y, z, noise, octaves, lacunarity, gain)
    }
}

internal inline fun rigidFunc4D(
    crossinline noise: (Int, Double, Double, Double, Double) -> Double,
    octaves: Int = 8,
    lacunarity: Double = 0.5,
    gain: Double = 0.5
): (Int, Double, Double, Double, Double) -> Double {
    return { seed, x, y, z, w ->
        rigid(seed, x, y, z, w, noise, octaves, lacunarity, gain)
    }
}

// functional composition tools

fun ((Int, Double) -> Double).fbm(
    octaves: Int = 8,
    lacunarity: Double = 0.5,
    gain: Double = 0.5
): (Int, Double) -> Double =
    fbmFunc1D(this, octaves, lacunarity, gain)

fun ((Int, Double, Double) -> Double).fbm(
    octaves: Int = 8,
    lacunarity: Double = 0.5,
    gain: Double = 0.5
): (Int, Double, Double) -> Double =
    fbmFunc2D(this, octaves, lacunarity, gain)

fun ((Int, Double, Double, Double) -> Double).fbm(
    octaves: Int = 8,
    lacunarity: Double = 0.5,
    gain: Double = 0.5
): (Int, Double, Double, Double) -> Double =
    fbmFunc3D(this, octaves, lacunarity, gain)

fun ((Int, Double, Double, Double, Double) -> Double).fbm(
    octaves: Int = 8,
    lacunarity: Double = 0.5,
    gain: Double = 0.5
): (Int, Double, Double, Double, Double) -> Double =
    fbmFunc4D(this, octaves, lacunarity, gain)

fun ((Int, Double) -> Double).billow(
    octaves: Int = 8,
    lacunarity: Double = 0.5,
    gain: Double = 0.5
): (Int, Double) -> Double =
    billowFunc1D(this, octaves, lacunarity, gain)

fun ((Int, Double, Double) -> Double).billow(
    octaves: Int = 8,
    lacunarity: Double = 0.5,
    gain: Double = 0.5
): (Int, Double, Double) -> Double =
    billowFunc2D(this, octaves, lacunarity, gain)

fun ((Int, Double, Double, Double) -> Double).billow(
    octaves: Int = 8,
    lacunarity: Double = 0.5,
    gain: Double = 0.5
): (Int, Double, Double, Double) -> Double =
    billowFunc3D(this, octaves, lacunarity, gain)

fun ((Int, Double, Double, Double, Double) -> Double).billow(
    octaves: Int = 8,
    lacunarity: Double = 0.5,
    gain: Double = 0.5
): (Int, Double, Double, Double, Double) -> Double =
    billowFunc4D(this, octaves, lacunarity, gain)

fun ((Int, Double) -> Double).rigid(
    octaves: Int = 8,
    lacunarity: Double = 0.5,
    gain: Double = 0.5
): (Int, Double) -> Double =
    rigidFunc1D(this, octaves, lacunarity, gain)

fun ((Int, Double, Double) -> Double).rigid(
    octaves: Int = 8,
    lacunarity: Double = 0.5,
    gain: Double = 0.5
): (Int, Double, Double) -> Double =
    rigidFunc2D(this, octaves, lacunarity, gain)

fun ((Int, Double, Double, Double) -> Double).rigid(
    octaves: Int = 8,
    lacunarity: Double = 0.5,
    gain: Double = 0.5
): (Int, Double, Double, Double) -> Double =
    rigidFunc3D(this, octaves, lacunarity, gain)

fun ((Int, Double, Double, Double, Double) -> Double).rigid(
    octaves: Int = 8,
    lacunarity: Double = 0.5,
    gain: Double = 0.5
): (Int, Double, Double, Double, Double) -> Double =
    rigidFunc4D(this, octaves, lacunarity, gain)
