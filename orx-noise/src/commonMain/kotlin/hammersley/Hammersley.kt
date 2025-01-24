package org.openrndr.extra.noise.hammersley

import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4

/**
 * Computes a 2D Hammersley point based on the given index and total number of samples.
 *
 * @param i The index of the sample, typically in the range [0, n).
 * @param n The total number of samples.
 * @return A 2D point as a `Vector2` within the unit square [0, 1] x [0, 1].
 */
fun hammersley2D(i: Int, n: Int): Vector2 {
    return Vector2(i.toDouble() / n, radicalInverseBase2(i.toUInt()))
}

/**
 * Computes a 3D point in the Hammersley sequence based on the given index and total number of samples.
 *
 * @param i The index of the sample, typically in the range [0, n).
 * @param n The total number of samples.
 * @return A 3D point as a `Vector3` within the unit cube [0, 1] x [0, 1] x [0, 1].
 */
fun hammersley3D(i: Int, n: Int): Vector3 {
    return Vector3(i.toDouble() / n, radicalInverseBase2(i.toUInt()), radicalInverse(3, i))
}

/**
 * Computes a 4D Hammersley point based on the given index and total number of samples.
 *
 * @param i The index of the sample, typically in the range [0, n).
 * @param n The total number of samples.
 * @return A 4D point as a `Vector4` where each component lies within the range [0, 1].
 */
fun hammersley4D(i: Int, n: Int): Vector4 {
    return Vector4(i.toDouble() / n, radicalInverseBase2(i.toUInt()), radicalInverse(3, i), radicalInverse(5, i))
}

/**
 * Computes the radical inverse of a given unsigned integer `i` in base 2.
 *
 * @param i The input unsigned integer for which the radical inverse in base 2 is computed.
 * @return The radical inverse value of the input as a `Double`, mapped to the range [0, 1).
 */
fun radicalInverseBase2(i: UInt): Double {
    var bits = i
    bits = ((bits shl 16) or (bits shr 16))
    bits = ((bits and 0x55555555u) shl 1) or ((bits and 0xAAAAAAAAu) shr 1)
    bits = ((bits and 0x33333333u) shl 2) or ((bits and 0xCCCCCCCCu) shr 2)
    bits = ((bits and 0x0F0F0F0Fu) shl 4) or ((bits and 0xF0F0F0F0u) shr 4)
    bits = ((bits and 0x00FF00FFu) shl 8) or ((bits and 0xFF00FF00u) shr 8)
    return bits.toDouble() * 2.3283064365386963e-10
}

/**
 * Computes the radical inverse of an integer `i` in a given base.
 * This method is often used in quasi-random sequence generation for sampling.
 *
 * @param base The base in which to compute the radical inverse. Must be greater than 1.
 * @param i The integer for which the radical inverse is calculated. Must be non-negative.
 * @return The radical inverse value as a `Double`, within the range [0, 1).
 */
fun radicalInverse(base: Int, i: Int): Double {
    var v = 0.0
    var denom = 1.0
    var n = i
    while (n > 0) {
        denom *= base
        val remainder = n.mod(base)
        n /= base
        v += remainder / denom
    }
    return v
}
