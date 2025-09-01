package org.openrndr.extra.mesh

import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import kotlin.math.*


/**
 * Represents an indexed polygon in 3D space. The polygon is defined using indices referencing
 * the various attributes (e.g., position, texture coordinates, normals) provided in an external vertex data.
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
     * Checks if the polygon defined by the given vertex data is planar.
     *
     * @param vertexData The vertex data that contains the positions of the polygon's vertices.
     * @param eps A small tolerance value used to determine planarity. Defaults to 1E-2.
     * @return True if the polygon is planar, false otherwise.
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
     * Determines if the polygon defined by the given vertex data is convex.
     *
     * @param vertexData The vertex data containing the positions of the polygon's vertices.
     * @return True if the polygon is convex, false otherwise.
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
     * Computes the normal vector of the polygon based on the given vertex data.
     *
     * The method calculates the cross product of two edges of the polygon
     * and normalizes the resulting vector to obtain the normal.
     *
     * @param vertexData The vertex data that contains the positions of the polygon's vertices.
     * @return A normalized 3D vector representing the normal of the polygon.
     */
    fun normal(vertexData: IVertexData) : Vector3 {
        val u = vertexData.positions[positions[1]] - vertexData.positions[positions[0]]
        val v = vertexData.positions[positions[2]] - vertexData.positions[positions[0]]
        return u.cross(v).normalized
    }


    /**
     * Converts the provided vertex data into a polygon representation.
     *
     * @param vertexData The vertex data containing positions, normals, texture coordinates, and other attributes of the vertices.
     * @return A polygon created from the given vertex data.
     */
    fun toPolygon(vertexData: IVertexData): IPolygon
}


/**
 * Represents a polygon defined by indices corresponding to vertex data such as positions,
 * texture coordinates, colors, normals, tangents, and bitangents. It can be used to describe
 * a geometric shape for rendering or processing in 3D graphics or geometry applications.
 *
 * @property positions List of indices referencing the vertex positions.
 * @property textureCoords List of indices referencing the texture coordinates.
 * @property colors List of indices referencing vertex colors.
 * @property normals List of indices referencing vertex normals.
 * @property tangents List of indices referencing vertex tangents.
 * @property bitangents List of indices referencing vertex bitangents.
 */
data class IndexedPolygon(
    override val positions: List<Int>,
    override val textureCoords: List<Int>,
    override val colors: List<Int>,
    override val normals: List<Int>,
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
                if (colors.isNotEmpty()) colors.slice(it) else listOf(),
                if (normals.isNotEmpty()) normals.slice(it) else listOf(),
                if (tangents.isNotEmpty()) tangents.slice(it) else listOf(),
                if (bitangents.isNotEmpty()) bitangents.slice(it) else listOf()
            )
        }
    }


    /**
     * Triangulates the polygon represented by the provided vertex data.
     *
     * @param vertexData The vertex data that defines the positions, texture coordinates,
     *                   colors, normals, tangents, and bitangents of the polygon vertices.
     * @return A list of indexed triangles representing the triangulated polygon. Each triangle
     *         is defined using the vertex information from the provided vertex data.
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
                            colors.getOrNull(0),
                            colors.getOrNull(it + 1),
                            colors.getOrNull(it + 2)
                        ),
                        listOfNotNull(
                            normals.getOrNull(0),
                            normals.getOrNull(it),
                            normals.getOrNull(it + 1)
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
            vertexData.textureCoords.slice(textureCoords),
            vertexData.colors.slice(colors),
            vertexData.normals.slice(normals),
            vertexData.tangents.slice(tangents),
            vertexData.bitangents.slice(bitangents)
        )
    }


    /**
     * Shifts the indices for position, texture coordinates, colors, normals, tangents, and bitangents
     * by the specified amounts and returns a new IndexedPolygon with the updated indices.
     *
     * @param positions The amount to shift the position indices. Defaults to 0.
     * @param textureCoords The amount to shift the texture coordinate indices. Defaults to 0.
     * @param colors The amount to shift the color indices. Defaults to 0.
     * @param normals The amount to shift the normal indices. Defaults to 0.
     * @param tangents The amount to shift the tangent indices. Defaults to 0.
     * @param bitangents The amount to shift the bitangent indices. Defaults to 0.
     * @return A new IndexedPolygon with indices shifted by the provided values.
     */
    fun shiftIndices(
        positions: Int = 0,
        textureCoords: Int = 0,
        colors: Int = 0,
        normals: Int = 0,
        tangents: Int = 0,
        bitangents: Int = 0
    ): IndexedPolygon {
        return IndexedPolygon(
            positions = this.positions.map { it + positions },
            textureCoords = this.textureCoords.map { it + textureCoords },
            colors = this.colors.map { it + colors },
            normals = this.normals.map { it + normals },
            tangents = this.tangents.map { it + tangents },
            bitangents = this.bitangents.map { it + bitangents }
        )
    }
}


/**
 * Represents a mutable 3D indexed polygon. This class allows modifications to its indices and
 * provides functionality to transform vertex references into a corresponding polygon representation.
 *
 * The polygon is defined by indices referencing an external vertex data source, such as the
 * position, texture coordinates, normals, colors, tangents, and bitangents of the vertices. These
 * indices can be updated, providing flexibility for dynamic operations on the polygon.
 *
 * @property positions Mutable list of position indices defining the polygon's vertices.
 * @property textureCoords Mutable list of texture coordinate indices defining the mapping of textures.
 * @property normals Mutable list of normal indices, which specify the normals of the vertices.
 * @property colors Mutable list of color indices specifying the vertex colors.
 * @property tangents Mutable list of tangent indices, optional.
 * @property bitangents Mutable list of bitangent indices, optional.
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
            vertexData.textureCoords.slice(textureCoords).toMutableList(),
            vertexData.colors.slice(colors).toMutableList(),
            vertexData.normals.slice(normals).toMutableList()
        )
    }
}