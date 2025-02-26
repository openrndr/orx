package org.openrndr.extra.noise.shapes

import org.openrndr.extra.noise.uhash11
import org.openrndr.math.Vector3
import org.openrndr.shape.Box
import kotlin.random.Random

/**
 * Generates a uniformly distributed random point inside the `Box`.
 *
 * @param random An optional random number generator to use. Defaults to `Random.Default`.
 * @return A `Vector3` representing a random point within the `Box`.
 */
fun Box.uniform(random: Random = Random.Default): Vector3 {
    val x = random.nextDouble() * width + corner.x
    val y = random.nextDouble() * height + corner.y
    val z = random.nextDouble() * depth + corner.z
    return Vector3(x, y, z)
}

/**
 * Generates a random point inside the boundary of the Box using a hash-based approach.
 *
 * @param seed An integer seed used for hashing to produce deterministic random results for a given seed.
 * @param x An integer that acts as an additional input to the hash, allowing variation in the generated points.
 * @return A Vector3 representing the random point within the Box, based on the provided seed and x.
 */
fun Box.hash(seed: Int, x: Int): Vector3 {
    val ux = uhash11(seed.toUInt() + uhash11(x.toUInt()))
    val uy = uhash11(ux + x.toUInt())
    val uz = uhash11(uy + x.toUInt())

    val fx = ux.toDouble() / UInt.MAX_VALUE.toDouble()
    val fy = uy.toDouble() / UInt.MAX_VALUE.toDouble()
    val fz = uz.toDouble() / UInt.MAX_VALUE.toDouble()

    val x = fx * width + corner.x
    val y = fy * height + corner.y
    val z = fz * depth + corner.z
    return Vector3(x, y, z)
}


/**
 * Generates a uniformly distributed sub-box based on random parameters within specified ranges.
 *
 * @param minWidth The minimum width of the sub-box. Defaults to 0.0.
 * @param maxWidth The maximum width of the sub-box. Defaults to 1.0.
 * @param minHeight The minimum height of the sub-box. Defaults to 0.0.
 * @param maxHeight The maximum height of the sub-box. Defaults to 1.0.
 * @param minDepth The minimum depth of the sub-box. Defaults to 0.0.
 * @param maxDepth The maximum depth of the sub-box. Defaults to 1.0.
 * @param random The `Random` instance used for generating random values. Defaults to `Random.Default`.
 * @return A new `Box` that represents the sub-box.
 */
fun Box.uniformSub(
    minWidth: Double = 0.0,
    maxWidth: Double = 1.0,
    minHeight: Double = 0.0,
    maxHeight: Double = 1.0,
    minDepth: Double = 0.0,
    maxDepth: Double = 1.0,
    random: Random = Random.Default
): Box {
    val width = random.nextDouble(minWidth, maxWidth)
    val height = random.nextDouble(minHeight, maxHeight)
    val depth = random.nextDouble(minDepth, maxDepth)
    val u0 = random.nextDouble(1.0 - width)
    val v0 = random.nextDouble(1.0 - height)
    val w0 = random.nextDouble(1.0 - depth)
    val u1 = u0 + width
    val v1 = v0 + height
    val w1 = w0 + depth
    return sub(u0..u1, v0..v1, w0..w1)
}