package org.openrndr.extra.noise.shapes

import org.openrndr.extra.noise.fhash1D
import org.openrndr.extra.noise.hash
import org.openrndr.math.Polar
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Generate a uniformly distributed random point inside [Circle]
 */
fun Circle.hash(seed: Int, x: Int): Vector2 {
    val r = radius * sqrt(fhash1D(seed, x))
    val phi = 360.0 * fhash1D(seed xor 0x7f7f_7f7f, x)
    return Polar(phi, r).cartesian + center
}

/**
 * Generate a uniformly distributed random point inside [Circle]
 */
fun Circle.uniform(random: Random = Random.Default): Vector2 {
    val r = radius * sqrt(random.nextDouble())
    val phi = 360.0 * random.nextDouble()
    return Polar(phi, r).cartesian + center
}