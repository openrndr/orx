package org.openrndr.extra.noise

import org.openrndr.math.IntVector2
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import org.openrndr.shape.Rectangle
import kotlin.random.Random

fun random(min: Double = -1.0, max: Double = 1.0,
           random: Random = Random.Default) =
    (random.nextDouble() * (max - min)) + min

fun Int.Companion.uniform(min: Int = -1, max: Int = 2,
                          random: Random = Random.Default) =
        (random.nextDouble() * (max - min)).toInt() + min

fun Double.Companion.uniform(min: Double = -1.0, max: Double = 1.0,
                             random: Random = Random.Default) =
        (random.nextDouble() * (max - min)) + min

fun Vector2.Companion.uniform(min: Vector2 = -ONE, max: Vector2 = ONE,
                              random: Random = Random.Default) =
        Vector2(Double.uniform(min.x, max.x, random),
                Double.uniform(min.y, max.y, random))

fun Vector2.Companion.uniform(min: Double = -1.0, max: Double = 1.0,
                              random: Random = Random.Default) =
        Vector2.uniform(Vector2(min, min), Vector2(max, max), random)

fun Vector2.Companion.uniform(rect: Rectangle,
                              random: Random = Random.Default) =
        Vector2.uniform(rect.corner,
                rect.corner + rect.dimensions, random)

fun IntVector2.Companion.uniform(min: IntVector2 = IntVector2(-1, -1),
                                 max: IntVector2 = IntVector2(2, 2),
                                 random: Random = Random.Default) =
        IntVector2(Int.uniform(min.x, max.x, random),
                Int.uniform(min.y, max.y, random))

fun IntVector2.Companion.uniform(min: Int = -1, max: Int = 2,
                                 random: Random = Random.Default) =
        IntVector2.uniform(IntVector2(min, min),
                IntVector2(max, max), random)

fun Vector2.Companion.uniformRing(innerRadius: Double = 0.0,
                                  outerRadius: Double = 1.0,
                                  random: Random = Random.Default): Vector2 {
    while (true) {
        uniform(-outerRadius, outerRadius, random).let {
            val squaredLength = it.squaredLength
            if (squaredLength >= innerRadius * innerRadius && squaredLength < outerRadius * outerRadius) {
                return it
            }
        }
    }
}

fun Vector2.Companion.uniforms(count: Int,
                               min: Vector2 = -ONE,
                               max: Vector2 = ONE,
                               random: Random = Random.Default): List<Vector2> =
        List(count) {
            Vector2.uniform(min, max, random)
        }

fun Vector2.Companion.uniforms(count: Int,
                               rect: Rectangle,
                               random: Random = Random.Default): List<Vector2> =
        List(count) { Vector2.uniform(rect, random) }

fun Vector2.Companion.uniformSequence(rect: Rectangle,
                               random: Random = Random.Default): Sequence<Vector2> =
    sequence {
        while(true) {
            yield(uniform(rect, random))
        }
    }

fun Vector2.Companion.uniformsRing(count: Int,
                                   innerRadius: Double = 0.0, outerRadius: Double = 1.0,
                                   random: Random = Random.Default): List<Vector2> =
        List(count) {
            Vector2.uniformRing(innerRadius, outerRadius, random)
        }

fun Vector3.Companion.uniform(min: Double = -1.0, max: Double = 1.0, random: Random = Random.Default): Vector3 =
        Vector3.uniform(Vector3(min, min, min), Vector3(max, max, max), random)

fun Vector3.Companion.uniform(min: Vector3 = -ONE,
                              max: Vector3 = ONE,
                              random: Random = Random.Default): Vector3 {
    return Vector3(Double.uniform(min.x, max.x, random),
            Double.uniform(min.y, max.y, random),
            Double.uniform(min.z, max.z, random))
}

fun Vector3.Companion.uniformRing(innerRadius: Double = 0.0,
                                  outerRadius: Double = 1.0,
                                  random: Random = Random.Default): Vector3 {
    while (true) {
        uniform(-outerRadius, outerRadius, random).let {
            val squaredLength = it.squaredLength
            if (squaredLength >= innerRadius * innerRadius && squaredLength < outerRadius * outerRadius) {
                return it
            }
        }
    }
}

fun Vector3.Companion.uniforms(count: Int,
                               min: Double = -1.0,
                               max: Double = 1.0,
                               random: Random = Random.Default): List<Vector3> =
        List(count) {
            Vector3.uniform(min, max, random)
        }


fun Vector3.Companion.uniforms(count: Int,
                               min: Vector3 = -ONE,
                               max: Vector3 = ONE,
                               random: Random = Random.Default): List<Vector3> =
        List(count) {
            Vector3.uniform(min, max, random)
        }

fun Vector3.Companion.uniformsRing(count: Int,
                                   innerRadius: Double = 0.0, outerRadius: Double = 1.0,
                                   random: Random = Random.Default): List<Vector3> =
        List(count) {
            Vector3.uniformRing(innerRadius, outerRadius, random)
        }


fun Vector4.Companion.uniform(min: Double = -1.0, max: Double = 1.0, random: Random = Random.Default): Vector4 =
        Vector4.uniform(Vector4(min, min, min, min), Vector4(max, max, max, max), random)

fun Vector4.Companion.uniform(min: Vector4 = -ONE, max: Vector4 = ONE, random: Random = Random.Default): Vector4 {
    return Vector4(Double.uniform(min.x, max.x, random),
            Double.uniform(min.y, max.y, random),
            Double.uniform(min.z, max.z, random),
            Double.uniform(min.w, max.w, random))
}

fun Vector4.Companion.uniformRing(innerRadius: Double = 0.0,
                                  outerRadius: Double = 1.0,
                                  random: Random = Random.Default): Vector4 {
    while (true) {
        uniform(-outerRadius, outerRadius, random).let {
            val squaredLength = it.squaredLength
            if (squaredLength >= innerRadius * innerRadius && squaredLength < outerRadius * outerRadius) {
                return it
            }
        }
    }
}

