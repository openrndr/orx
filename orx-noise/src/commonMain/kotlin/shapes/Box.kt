package org.openrndr.extra.noise.shapes

import org.openrndr.extra.noise.uhash11
import org.openrndr.math.Vector3
import org.openrndr.shape.Box
import kotlin.random.Random

fun Box.uniform(random: Random = Random.Default): Vector3 {
    val x = random.nextDouble() * width + corner.x
    val y = random.nextDouble() * height + corner.y
    val z = random.nextDouble() * depth + corner.z
    return Vector3(x, y ,z)
}

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