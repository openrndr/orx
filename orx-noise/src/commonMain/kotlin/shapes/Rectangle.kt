package org.openrndr.extra.noise.shapes

import org.openrndr.extra.noise.uhash11
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import kotlin.random.Random

fun Rectangle.uniform(random: Random = Random.Default): Vector2 {
    val x = random.nextDouble() * width + corner.x
    val y = random.nextDouble() * height + corner.y
    return Vector2(x, y)
}

fun Rectangle.hash(seed: Int, x: Int): Vector2 {
    val ux = uhash11(seed.toUInt() + uhash11(x.toUInt()))
    val uy = uhash11(ux + x.toUInt())

    val fx = ux.toDouble() / UInt.MAX_VALUE.toDouble()
    val fy = uy.toDouble() / UInt.MAX_VALUE.toDouble()

    val x = fx * width + corner.x
    val y = fy * height + corner.y
    return Vector2(x, y)
}