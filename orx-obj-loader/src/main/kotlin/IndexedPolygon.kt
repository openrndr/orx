package org.openrndr.extra.objloader

import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.round

data class IndexedPolygon(
    val positions: IntArray, val textureCoords: IntArray, val normals: IntArray
) {

    fun base(vertexData: VertexData): Matrix44 {
        val u = (vertexData.positions[positions[1]] - vertexData.positions[positions[0]])
        val v = (vertexData.positions[positions[positions.size - 1]] - vertexData.positions[positions[0]])
        val normal = u.cross(v)
        val bitangent = normal.cross(u)
        return Matrix44.fromColumnVectors(
            u.xyz0.normalized,
            bitangent.xyz0.normalized,
            normal.xyz0.normalized,
            Vector4.UNIT_W
        )
    }

    fun isPlanar(vertexData: VertexData, eps: Double = 1E-2): Boolean {
        fun normal(i: Int): Vector3 {
            val p0 = vertexData.positions[positions[(i - 1).mod(positions.size)]]
            val p1 = vertexData.positions[positions[(i).mod(positions.size)]]
            val p2 = vertexData.positions[positions[(i + 1).mod(positions.size)]]

            val u = (p0 - p1).normalized
            val v = (p2 - p1).normalized
            return u.cross(v).normalized
        }
        return if (positions.size <= 3) {
            true
        } else {
            val n0 = normal(0)
            (1 until positions.size - 2).all { n0.dot(normal(it)) >= 1.0 - eps }
        }
    }

    fun isConvex(vertexData: VertexData): Boolean {
        val planar = base(vertexData).inversed

        fun p(v: Vector3): Vector2 {
            return (planar * v.xyz1).xy
        }

        if (positions.size < 3) {
            return false
        }
        var old = p(vertexData.positions[positions[positions.size - 2]])
        var new = p(vertexData.positions[positions[positions.size - 1]])
        var newDirection = atan2(new.y - old.y, new.x - old.x)
        var angleSum = 0.0
        var oldDirection: Double
        var orientation = Double.POSITIVE_INFINITY
        for ((ndx, newPointIndex) in positions.withIndex()) {
            old = new
            oldDirection = newDirection
            val newPoint = p(vertexData.positions[newPointIndex])
            new = newPoint
            newDirection = atan2(new.y - old.y, new.x - old.x)

            if (old == new) {
                return false
            }
            var angle = newDirection - oldDirection
            if (angle <= -Math.PI)
                angle += PI * 2.0

            if (angle > PI) {
                angle -= PI * 2.0
            }

            if (ndx == 0) {
                if (angle == 0.0) {
                    return false
                }
                orientation = if (angle > 0.0) 1.0 else -1.0

            } else {
                if (orientation * angle <= 0.0) {
                    return false
                }
            }
            angleSum += angle

        }
        return abs(round(angleSum / (2 * PI))) == 1.0
    }

    fun tessellate(vertexData: VertexData): List<IndexedPolygon> {
        val points = vertexData.positions.slice(positions.toList())
        val triangles = org.openrndr.shape.triangulate(listOf(points))

        return triangles.windowed(3, 3).map {
            IndexedPolygon(
                positions.sliceArray(it),
                if (textureCoords.isNotEmpty()) textureCoords.sliceArray(it) else intArrayOf(),
                if (normals.isNotEmpty()) normals.sliceArray(it) else intArrayOf()
            )
        }
    }

    fun triangulate(vertexData: VertexData): List<IndexedPolygon> {
        return when {
            positions.size == 3 -> listOf(this)
            isPlanar(vertexData) && isConvex(vertexData) -> {
                val triangleCount = positions.size - 2
                (0 until triangleCount).map {
                    IndexedPolygon(
                        intArrayOf(positions[0], positions[it + 1], positions[it + 2]),
                        listOfNotNull(
                            textureCoords.getOrNull(0),
                            textureCoords.getOrNull(it),
                            textureCoords.getOrNull(it + 1)
                        ).toIntArray(),
                        listOfNotNull(
                            normals.getOrNull(0),
                            normals.getOrNull(it),
                            normals.getOrNull(it + 1)
                        ).toIntArray(),
                    )
                }
            }

            else -> tessellate(vertexData)
        }
    }

    fun toPolygon(vertexData: VertexData): Polygon {
        return Polygon(
            vertexData.positions.slice(positions.toList()).toTypedArray(),
            vertexData.normals.slice(normals.toList()).toTypedArray(),
            vertexData.textureCoords.slice(textureCoords.toList()).toTypedArray()
        )
    }
}