package org.openrndr.extra.mesh.noise

import org.openrndr.extra.noise.fhash1D
import org.openrndr.math.Vector3
import kotlin.math.sqrt

typealias Barycentric = Vector3


/**
 * Generates a 3D vector with components representing uniform barycentric coordinates
 * over a standard triangle. The barycentric coordinates are computed based on two
 * input parameters, `u` and `v`, which are random values typically ranging between 0 and 1.
 *
 * @param u A random value used to compute the barycentric coordinate.
 * @param v A random value used to compute the barycentric coordinate.
 * @return A [Vector3] instance representing uniform barycentric coordinates, where each component
 *         corresponds to the weight of a vertex in the triangle.
 */
fun uniformBarycentric(u: Double, v: Double): Barycentric {
    val su0 = sqrt(u)
    val b0 = 1.0 - su0
    val b1 = v * su0
    return Barycentric(b0, b1, 1.0 - b0 - b1)
}

/**
 * Adjusts a barycentric coordinate based on weights for each component and normalizes the result.
 *
 * @param barycentric The original barycentric coordinate to be adjusted.
 * @param weight0 The weight for the first component of the barycentric coordinate.
 * @param weight1 The weight for the second component of the barycentric coordinate.
 * @param weight2 The weight for the third component of the barycentric coordinate.
 * @return A new barycentric coordinate with components adjusted by their corresponding weights and normalized.
 */
fun weightBarycentric(barycentric: Barycentric, weight0: Double, weight1: Double, weight2: Double): Barycentric {
    val b0 = barycentric.x * weight0
    val b1 = barycentric.y * weight1
    val b2 = barycentric.z * weight2
    val totalWeight = b0 + b1 + b2
    return Barycentric(b0 / totalWeight, b1 / totalWeight, b2 / totalWeight)
}

/**
 * Computes a barycentric coordinate vector derived from hashing operations.
 *
 * This function generates a barycentric coordinate for a point within a triangle
 * by utilizing hash-based techniques to ensure uniform randomization. The result
 * is expressed as a 3D vector where the components represent the barycentric
 * weights.
 *
 * @param seed An integer seed used for the hash function to ensure reproducibility.
 * @param x An integer value that contributes to the hash-based computation.
 * @return A `Vector3` instance containing the computed barycentric coordinates.
 */
fun hashBarycentric(seed: Int, x: Int): Barycentric {
    val u = fhash1D(seed, x)
    val v = fhash1D(seed, u.toRawBits().toInt() - x)

    val su0 = sqrt(u)
    val b0 = 1.0 - su0
    val b1 = v * su0
    return Vector3(b0, b1, 1.0 - b0 - b1)
}
