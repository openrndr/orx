package org.openrndr.extra.noise.shapes

import org.openrndr.extra.noise.uhash11
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import kotlin.random.Random

/**
 * Generates a uniformly distributed random point within the `Rectangle`.
 *
 * @param random An optional random number generator to use. Defaults to `Random.Default`.
 * @return A `Vector2` representing a random point within the `Rectangle`.
 */
fun Rectangle.uniform(random: Random = Random.Default): Vector2 {
    val x = random.nextDouble() * width + corner.x
    val y = random.nextDouble() * height + corner.y
    return Vector2(x, y)
}

/**
 * Generates a random point within the bounds of the `Rectangle` using a hash-based approach.
 *
 * @param seed An integer seed for the hash function, used to produce deterministic random results for the same seed.
 * @param x An integer input to the hash function, adding further variation to the generated point.
 * @return A `Vector2` representing a random point within the `Rectangle`, based on the provided `seed` and `x`.
 */
fun Rectangle.hash(seed: Int, x: Int): Vector2 {
    val ux = uhash11(seed.toUInt() + uhash11(x.toUInt()))
    val uy = uhash11(ux + x.toUInt())

    val fx = ux.toDouble() / UInt.MAX_VALUE.toDouble()
    val fy = uy.toDouble() / UInt.MAX_VALUE.toDouble()

    val x = fx * width + corner.x
    val y = fy * height + corner.y
    return Vector2(x, y)
}


/**
 * Generates a uniformly distributed sub-rectangle based on random parameters within specified ranges.
 *
 * @param minWidth The minimum width of the sub-rectangle. Defaults to 0.0.
 * @param maxWidth The maximum width of the sub-rectangle. Defaults to 1.0.
 * @param minHeight The minimum height of the sub-rectangle. Defaults to 0.0.
 * @param maxHeight The maximum height of the sub-rectangle. Defaults to 1.0.
 * @param random The `Random` instance used for generating random values.
 * @return A new `Rectangle` that represents the sub-rectangle.
 */
fun Rectangle.uniformSub(
    minWidth: Double = 0.0,
    maxWidth: Double = 1.0,
    minHeight: Double = 0.0,
    maxHeight: Double = 1.0,
    random: Random = Random.Default
): Rectangle {
    val width = random.nextDouble(minWidth, maxWidth)
    val height = random.nextDouble(minHeight, maxHeight)
    val u0 = random.nextDouble(1.0 - width)
    val v0 = random.nextDouble(1.0 - height)
    val u1 = u0 + width
    val v1 = v0 + height
    return sub(u0..u1, v0..v1)
}