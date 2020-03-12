package org.openrndr.extra.noise

import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4

inline fun gradient(
        crossinline noise: (seed: Int, x: Double) -> Double,
        seed: Int,
        x: Double,
        epsilon: Double = 0.01
): Double {
    val xn = noise(seed, x - epsilon)
    val xp = noise(seed, x + epsilon)
    return (xp - xn) / (2.0 * epsilon)
}

inline fun gradient(
        crossinline noise: (seed: Int, x: Double, y: Double) -> Double,
        seed: Int,
        x: Double,
        y: Double,
        epsilon: Double = 0.01
): Vector2 {
    val xn = noise(seed, x - epsilon, y)
    val xp = noise(seed, x + epsilon, y)
    val yn = noise(seed, x, y - epsilon)
    val yp = noise(seed, x, y + epsilon)
    return Vector2((xp - xn) / (2.0 * epsilon), (yp - yn) / (2.0 * epsilon))
}

inline fun gradient(
        crossinline noise: (seed: Int, x: Double, y: Double, z: Double) -> Double,
        seed: Int,
        x: Double,
        y: Double,
        z: Double,
        epsilon: Double = 0.01
): Vector3 {
    val xn = noise(seed, x - epsilon, y, z)
    val xp = noise(seed, x + epsilon, y, z)
    val yn = noise(seed, x, y - epsilon, z)
    val yp = noise(seed, x, y + epsilon, z)
    val zn = noise(seed, x, y, z - epsilon)
    val zp = noise(seed, x, y, z + epsilon)
    return Vector3((xp - xn) / (2.0 * epsilon), (yp - yn) / (2.0 * epsilon), (zp - zn) / (2.0 * epsilon))
}

inline fun gradient(
        crossinline noise: (seed: Int, x: Double, y: Double, z: Double, w: Double) -> Double,
        seed: Int,
        x: Double,
        y: Double,
        z: Double,
        w: Double,
        epsilon: Double = 0.01
): Vector4 {
    val xn = noise(seed, x - epsilon, y, z, w)
    val xp = noise(seed, x + epsilon, y, z, w)
    val yn = noise(seed, x, y - epsilon, z, w)
    val yp = noise(seed, x, y + epsilon, z, w)
    val zn = noise(seed, x, y, z - epsilon, w)
    val zp = noise(seed, x, y, z + epsilon, w)
    val wn = noise(seed, x, y, z, w - epsilon)
    val wp = noise(seed, x, y, z, w + epsilon)
    return Vector4(
            (xp - xn) / (2.0 * epsilon),
            (yp - yn) / (2.0 * epsilon),
            (zp - zn) / (2.0 * epsilon),
            (wp - wn) / (2.0 * epsilon)
    )
}