package org.openrndr.extra.mesh

import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3

/**
 * Vertex data interface
 */
interface IVertexData {
    /**
     * Vertex positions
     */
    val positions: List<Vector3>

    /**
     * Vertex texture coordinates
     */
    val textureCoords: List<Vector2>

    /**
     * Vertex colors
     */
    val colors: List<ColorRGBa>

    /**
     * Vertex normals
     */
    val normals: List<Vector3>

    /**
     * Vertex tangents
     */
    val tangents: List<Vector3>

    /**
     * Vertex bitangents
     */
    val bitangents: List<Vector3>

    /**
     * Convert to [VertexData]
     */
    fun toVertexData() : VertexData

    /**
     * Convert to [MutableVertexData]
     */
    fun toMutableVertexData() : MutableVertexData
}

/**
 * Immutable vertex data implementation
 */
class VertexData(
    override val positions: List<Vector3> = emptyList(),
    override val textureCoords: List<Vector2> = emptyList(),
    override val colors: List<ColorRGBa> = emptyList(),
    override val normals: List<Vector3> = emptyList(),
    override val tangents: List<Vector3> = emptyList(),
    override val bitangents: List<Vector3> = emptyList()
) : IVertexData {

    override fun toVertexData(): VertexData = this

    override fun toMutableVertexData(): MutableVertexData = MutableVertexData(
        positions.toMutableList(),
        textureCoords.toMutableList(),
        colors.toMutableList(),
        normals.toMutableList(),
        tangents.toMutableList(),
        bitangents.toMutableList()
    )
}


/**
 * Mutable vertex data implementation
 */
class MutableVertexData(
    override val positions: MutableList<Vector3> = mutableListOf(),
    override val textureCoords: MutableList<Vector2> = mutableListOf(),
    override val colors: MutableList<ColorRGBa> = mutableListOf(),
    override val normals: MutableList<Vector3> = mutableListOf(),
    override val tangents: MutableList<Vector3> = mutableListOf(),
    override val bitangents: MutableList<Vector3> = mutableListOf()
) : IVertexData {

    override fun toVertexData(): VertexData = VertexData(
        positions.toList(),
        textureCoords.toList(),
        colors.toList(),
        normals.toList(),
        tangents.toList(),
        bitangents.toList()
    )

    override fun toMutableVertexData(): MutableVertexData = this
}

/**
 * Add [point] to vertex data
 */
fun MutableVertexData.add(point: Point) {
    positions.add(point.position)
    point.color?.let { colors.add(it) }
    point.textureCoord?.let { textureCoords.add(it) }
    point.normal?.let { normals.add(it) }
    point.tangent?.let { tangents.add(it) }
    point.bitangent?.let { bitangents.add(it) }
}

/**
 * Retrieve [Point] from vertex data
 */
operator fun IVertexData.get(
    index: Int,
    textureCoordsIndex: Int = index,
    colorsIndex: Int = index,
    normalsIndex: Int = index,
    tangentsIndex: Int = index,
    bitangentsIndex: Int = index
): Point {
    return Point(
        positions[index],
        textureCoords.getOrNull(textureCoordsIndex),
        colors.getOrNull(colorsIndex),
        normals.getOrNull(normalsIndex),
        tangents.getOrNull(tangentsIndex),
        bitangents.getOrNull(bitangentsIndex)
    )
}