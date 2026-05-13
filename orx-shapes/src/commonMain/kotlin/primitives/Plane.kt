package org.openrndr.extra.shapes.primitives

import org.openrndr.math.Vector2
import org.openrndr.math.Vector3

class Plane(val normal: Vector3, val distance: Double) {
    fun side(point: Vector3): Double {
        return normal.dot(point) - distance
    }

    companion object {
        val XY = Plane(Vector3.UNIT_Z, 0.0)
        val XZ = Plane(Vector3.UNIT_Y, 0.0)
        val YZ = Plane(Vector3.UNIT_X, 0.0)


        fun fromPoints(a: Vector2, b: Vector2): Plane {
            val direction = b - a
            val normal = Vector3(-direction.y, direction.x, 0.0).normalized
            val distance = normal.dot(a.xy0)
            return Plane(normal, distance)
        }
    }
}