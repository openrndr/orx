package org.openrndr.extra.meshgenerators

import org.openrndr.math.Matrix44
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import org.openrndr.math.transforms.buildTransform

/**
 * Calculate frames (pose matrices) using parallel transport
 * @param up0 initial up vector, should not be collinear with `this[1] - this[0]`
 */
fun List<Vector3>.frames(up0: Vector3): List<Matrix44> {
    val result = mutableListOf<Matrix44>()

    if (this.isEmpty()) {
        return emptyList()
    }

    if (this.size == 1) {
        return listOf(Matrix44.IDENTITY)
    }

    var up = up0.normalized
    run {
        val current = this[0]
        val next = this[1]
        val forward = (next - current).normalized
        val right = (forward cross up).normalized
        up = ((right cross forward)).normalized
        result.add(Matrix44.fromColumnVectors(right.xyz0, up.xyz0, forward.xyz0, current.xyz1))
    }

    require(up.length > 0.0) { "initial `up.length` is zero in .frames()" }

    for (i in 1 until size - 1) {
        val prev = this[i - 1]
        val current = this[i]
        val next = this[i + 1]
        val f1 = (next - current).normalized
        val f0 = (current - prev).normalized

        val forward = (f0 + f1).normalized
        require(forward.length > 0.0) { "`forward.length` is zero in .frames()" }
        val right = (forward cross up).normalized
        up = ((right cross forward)).normalized

        require(up.length > 0.0) { "`up.length` is zero in .frames()" }
        require(right.length > 0.0) { "`right.length` is zero in .frames()" }

        //val m = Matrix44.fromColumnVectors(right.xyz0, up.xyz0, forward.xyz0, current.xyz1)

        val m = buildTransform {
            translate(current)
            multiply(Matrix44.fromColumnVectors(right.xyz0, up.xyz0, forward.xyz0, Vector4.UNIT_W))
        }

        result.add(m)
    }
    return result
}