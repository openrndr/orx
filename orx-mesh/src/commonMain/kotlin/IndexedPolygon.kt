package org.openrndr.extra.mesh

import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.round

/**
 * Indexed polygon interface
 */
interface IIndexedPolygon {
    /**
     * Position indices
     */
    val positions: List<Int>

    /**
     * Texture coordinate indices, optional
     */
    val textureCoords: List<Int>

    /**
     * Normal indices, optional
     */
    val normals: List<Int>

    /**
     * Color indices, optional
     */
    val colors: List<Int>

    /**
     * Tangents, optional
     */
    val tangents: List<Int>

    /**
     * Bitangents, optional
     */
    val bitangents: List<Int>

    fun base(vertexData: IVertexData): Matrix44 {
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

    /**
     * Determine if polygon is planar
     * @param vertexData the vertex data
     * @param eps error tolerance
     */
    fun isPlanar(vertexData: IVertexData, eps: Double = 1E-2): Boolean {
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

    /**
     * Determine polygon convexity
     */
    fun isConvex(vertexData: IVertexData): Boolean {
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
            if (angle <= -PI)
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

    /**
     * Convert to [IPolygon]
     * @param vertexData the vertex data required to build the [IPolygon]
     */
    fun toPolygon(vertexData: IVertexData): IPolygon
}

/**
 * Immutable indexed polygon implementation
 */
data class IndexedPolygon(
    override val positions: List<Int>,
    override val textureCoords: List<Int>,
    override val normals: List<Int>,
    override val colors: List<Int>,
    override val tangents: List<Int>,
    override val bitangents: List<Int>

    ) : IIndexedPolygon {

    private fun tessellate(vertexData: IVertexData): List<IndexedPolygon> {
        val points = vertexData.positions.slice(positions.toList())
        val triangles = org.openrndr.shape.triangulate(listOf(points))

        return triangles.windowed(3, 3).map {
            IndexedPolygon(
                positions.slice(it),
                if (textureCoords.isNotEmpty()) textureCoords.slice(it) else listOf(),
                if (normals.isNotEmpty()) normals.slice(it) else listOf(),
                if (colors.isNotEmpty()) colors.slice(it) else listOf(),
                if (tangents.isNotEmpty()) tangents.slice(it) else listOf(),
                if (bitangents.isNotEmpty()) bitangents.slice(it) else listOf()
            )
        }
    }

    /**
     * Convert to a list of triangle [IndexedPolygon]
     *
     * Supports non-planar and non-convex polygons
     */
    fun triangulate(vertexData: IVertexData): List<IndexedPolygon> {
        return when {
            positions.size == 3 -> listOf(this)
            isPlanar(vertexData) && isConvex(vertexData) -> {
                val triangleCount = positions.size - 2
                (0 until triangleCount).map {
                    IndexedPolygon(
                        listOf(positions[0], positions[it + 1], positions[it + 2]),
                        listOfNotNull(
                            textureCoords.getOrNull(0),
                            textureCoords.getOrNull(it),
                            textureCoords.getOrNull(it + 1)
                        ),
                        listOfNotNull(
                            normals.getOrNull(0),
                            normals.getOrNull(it),
                            normals.getOrNull(it + 1)
                        ),
                        listOfNotNull(
                            colors.getOrNull(0),
                            colors.getOrNull(it + 1),
                            colors.getOrNull(it + 2)
                        ),
                        listOfNotNull(
                            tangents.getOrNull(0),
                            tangents.getOrNull(it + 1),
                            tangents.getOrNull(it + 2)
                        ),
                        listOfNotNull(
                            bitangents.getOrNull(0),
                            bitangents.getOrNull(it + 1),
                            bitangents.getOrNull(it + 2)
                        ),
                    )
                }
            }

            else -> tessellate(vertexData)
        }
    }

    override fun toPolygon(vertexData: IVertexData): Polygon {
        return Polygon(
            vertexData.positions.slice(positions),
            vertexData.normals.slice(normals),
            vertexData.textureCoords.slice(textureCoords),
            vertexData.colors.slice(colors)
        )
    }
}

/**
 * Mutable indexed polygon implementation
 */
data class MutableIndexedPolygon(
    override val positions: MutableList<Int>,
    override val textureCoords: MutableList<Int>,
    override val normals: MutableList<Int>,
    override val colors: MutableList<Int>,
    override val tangents: MutableList<Int>,
    override val bitangents: MutableList<Int>
) : IIndexedPolygon {

    override fun toPolygon(vertexData: IVertexData): MutablePolygon {
        return MutablePolygon(
            vertexData.positions.slice(positions).toMutableList(),
            vertexData.normals.slice(normals).toMutableList(),
            vertexData.textureCoords.slice(textureCoords).toMutableList(),
            vertexData.colors.slice(colors).toMutableList()
        )
    }
}