package org.openrndr.extra.math.meanvaluecoordinates

import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import kotlin.jvm.JvmName
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.tan

fun findMVCWeights(points: List<Vector2>, point: Vector2): DoubleArray {
    val n = points.size
    val d = DoubleArray(n)
    val alpha = DoubleArray(n)

    for (i in 0 until n) {
        val s = points[i] - point
        d[i] = s.length
        if (d[i] < 1E-10) {
            val weights = DoubleArray(n)
            weights[i] = 1.0
            return weights
        }
    }

    for (i in 0 until n) {
        val s1 = points[i] - point
        val s2 = points[(i + 1) % n] - point
        
        val r1 = s1.length
        val r2 = s2.length

        // Signed angle using atan2: atan2(det, dot)
        // det = s1.x * s2.y - s1.y * s2.x
        // dot = s1.x * s2.x + s1.y * s2.y
        val det = s1.x * s2.y - s1.y * s2.x
        val dot = s1.dot(s2)
        alpha[i] = atan2(det, dot)

        if (abs(det) < 1E-10 && dot < 0.0) {
            // Point is on edge i
            val weights = DoubleArray(n)
            weights[i] = r2 / (r1 + r2)
            weights[(i + 1) % n] = r1 / (r1 + r2)
            return weights
        }
    }

    val weights = DoubleArray(n)
    var sumWeights = 0.0
    for (i in 0 until n) {
        val w = (tan(alpha[(i - 1 + n) % n] / 2.0) + tan(alpha[i] / 2.0)) / d[i]
        weights[i] = w
        sumWeights += w
    }

    for (i in 0 until n) {
        weights[i] /= sumWeights
    }

    return weights
}

@JvmName("findMVCWeights3D")
fun findMVCWeights(points: List<Vector3>, point: Vector3, normal: Vector3): DoubleArray {
    val bitangent1 = if (abs(normal.z) < 0.9) {
        normal.cross(Vector3.UNIT_Z).normalized
    } else {
        normal.cross(Vector3.UNIT_Y).normalized
    }
    val bitangent2 = normal.cross(bitangent1).normalized
    val origin = points[0]
    val points2D = points.map {
        val v = it - origin
        Vector2(v.dot(bitangent1), v.dot(bitangent2))
    }

    val point2D = (point - origin).let { Vector2(it.dot(bitangent1), it.dot(bitangent2)) }
    return findMVCWeights(points2D, point2D)
}