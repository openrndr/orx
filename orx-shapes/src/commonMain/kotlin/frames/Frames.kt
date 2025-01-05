package org.openrndr.extra.shapes.frames

import org.openrndr.math.Matrix44
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import org.openrndr.math.transforms.buildTransform

/**
 * Calculate frames (pose matrices) using parallel transport
 * @param up0 initial up vector, should not be collinear with `this[1] - this[0]`
 */

fun List<Vector3>.frames(up0: Vector3): List<Matrix44> {
    return frames(this, up0 = up0)
}

fun frames(positions: List<Vector3>, directions: List<Vector3> = emptyList(), up0: Vector3): List<Matrix44> {

    require(up0.squaredLength > 0.0) {
        "up0 ($up0) has 0 or NaN length"
    }

    val result = mutableListOf<Matrix44>()

    if (positions.isEmpty()) {
        return emptyList()
    }

    if (positions.size == 1) {
        return listOf(Matrix44.IDENTITY)
    }

    var up = up0.normalized
    run {
        val current = positions[0]
        val next = positions[1]
        val forward = (directions.getOrNull(0) ?: (next - current)).normalized
        val right = (forward cross up).normalized
        up = ((right cross forward)).normalized
        val frame = Matrix44.fromColumnVectors(right.xyz0, up.xyz0, -forward.xyz0, current.xyz1)
        require(frame.determinant in 0.99..1.01) {
            "Initial frame determinant (${frame.determinant}) != 1.0"
        }
        result.add(frame)
    }

    for (i in 1 until positions.size - 1) {
        val prev = positions[i - 1]
        val current = positions[i]
        val next = positions[i + 1]
        val f1 = (next - current).normalized
        val f0 = (current - prev).normalized

        val forward = (directions.getOrNull(i) ?: (f0 + f1)).normalized
        require(forward.length > 0.0) { "`forward.length` is zero or NaN in .frames()" }
        val right = (forward cross up).normalized
        up = ((right cross forward)).normalized

        require(up.length > 0.0) { "`up.length` is zero or NaN in .frames()" }
        require(right.length > 0.0) { "`right.length` is zero or NaN in .frames()" }

        val orientation = Matrix44.fromColumnVectors(right.xyz0, up.xyz0, -forward.xyz0, Vector4.UNIT_W)
        require(orientation.determinant in 0.99..1.01) {
            "Orientation determinant ${orientation.determinant} != 1.0"
        }

        val m = buildTransform {
            translate(current)
            multiply(orientation)
        }

        result.add(m)
    }
    return result
}