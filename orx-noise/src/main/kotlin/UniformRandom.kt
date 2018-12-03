package org.openrndr.extra.noise

import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4


fun Double.Companion.uniform(min: Double = -1.0, max: Double = 1.0): Double {
    return (Math.random() * (max - min)) + min
}

fun Vector2.Companion.uniform(min: Vector2 = -ONE, max: Vector2 = ONE): Vector2 {
    return Vector2(Double.uniform(min.x, max.x), Double.uniform(min.y, max.y))
}

fun Vector2.Companion.uniform(min: Double = -1.0, max: Double = 1.0) =
        Vector2.uniform(Vector2(min, min), Vector2(max, max))

fun Vector2.Companion.uniformRing(innerRadius: Double = 0.0, outerRadius: Double = 1.0): Vector2 {
    while (true) {
        uniform(-outerRadius, outerRadius).let {
            val squaredLength = it.squaredLength
            if (squaredLength >= innerRadius * innerRadius && squaredLength < outerRadius * outerRadius) {
                return it
            }
        }
    }
}

fun Vector3.Companion.uniform(min: Double = -1.0, max: Double = 1.0): Vector3 =
        Vector3.uniform(Vector3(min, min, min), Vector3(max, max, max))

fun Vector3.Companion.uniform(min: Vector3 = -ONE, max: Vector3 = ONE): Vector3 {
    return Vector3(Double.uniform(min.x, max.x), Double.uniform(min.y, max.y), Double.uniform(min.z, max.z))
}

// squared length 'polyfill' for OPENRNDR 0.3.30
private val Vector3.squaredLength__: Double  get() = x * x + y * y + z * z

fun Vector3.Companion.uniformRing(innerRadius: Double = 0.0, outerRadius: Double = 1.0): Vector3 {
    while (true) {
        uniform(-outerRadius, outerRadius).let {
            val squaredLength = it.squaredLength__
            if (squaredLength >= innerRadius * innerRadius && squaredLength < outerRadius * outerRadius) {
                return it
            }
        }
    }
}

// squared length 'polyfill' for OPENRNDR 0.3.30
private val Vector4.squaredLength__: Double  get() = x * x + y * y + z * z + w * w

fun Vector4.Companion.uniform(min: Double = -1.0, max: Double = 1.0): Vector4 =
        Vector4.uniform(Vector4(min, min, min, min), Vector4(max, max,max, max))

fun Vector4.Companion.uniform(min: Vector4 = -ONE, max: Vector4 = ONE): Vector4 {
    return Vector4(Double.uniform(min.x, max.x), Double.uniform(min.y, max.y), Double.uniform(min.z, max.z), Double.uniform(min.w, max.w))
}

fun Vector4.Companion.uniformRing(innerRadius: Double = 0.0, outerRadius: Double = 1.0): Vector4 {
    while (true) {
        uniform(-outerRadius, outerRadius).let {
            val squaredLength = it.squaredLength__
            if (squaredLength >= innerRadius * innerRadius && squaredLength < outerRadius * outerRadius) {
                return it
            }
        }
    }
}

