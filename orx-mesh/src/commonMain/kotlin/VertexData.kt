package org.openrndr.extra.mesh

import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3

/**
 * Interface representing vertex data for 3D graphics. This includes attributes such as positions,
 * texture coordinates, colors, normals, tangents, and bitangents. It provides methods to convert
 * the data to either immutable or mutable vertex data representations.
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
     * Converts the vertex data represented by the current instance into an immutable [VertexData] object.
     * This includes attributes such as positions, texture coordinates, colors, normals, tangents, and bitangents.
     *
     * @return A [VertexData] instance containing the immutable representation of the vertex data.
     */
    fun toVertexData() : VertexData

    /**
     * Converts the vertex data represented by the current instance into a mutable [MutableVertexData] object.
     * This includes attributes such as positions, texture coordinates, colors, normals, tangents, and bitangents.
     *
     * @return A [MutableVertexData] instance containing the mutable representation of the vertex data.
     */
    fun toMutableVertexData() : MutableVertexData
}

/**
 * Immutable implementation of vertex data for 3D graphics. This class provides a container for
 * vertex attributes such as positions, texture coordinates, colors, normals, tangents, and bitangents.
 *
 * It implements the [IVertexData] interface, allowing easy conversion between immutable and mutable
 * representations of vertex data.
 *
 * @property positions List of vertex positions as [Vector3].
 * @property textureCoords List of 2D texture coordinates as [Vector2].
 * @property colors List of vertex colors as [ColorRGBa].
 * @property normals List of vertex normals as [Vector3].
 * @property tangents List of vertex tangents as [Vector3].
 * @property bitangents List of vertex bitangents as [Vector3].
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
 * Mutable implementation of vertex data for 3D graphics. This class provides a container for
 * vertex attributes such as positions, texture coordinates, colors, normals, tangents, and bitangents
 * with mutable backing lists. It allows modification of vertex data.
 *
 * This class implements the [IVertexData] interface, enabling conversion between mutable and immutable
 * representations of vertex data.
 *
 * @constructor Creates a [MutableVertexData] object with optional initial data provided as mutable lists
 * for positions, texture coordinates, colors, normals, tangents, and bitangents.
 *
 * @property positions Mutable list of vertex positions as [Vector3].
 * @property textureCoords Mutable list of 2D texture coordinates as [Vector2].
 * @property colors Mutable list of vertex colors as [ColorRGBa].
 * @property normals Mutable list of vertex normals as [Vector3].
 * @property tangents Mutable list of vertex tangents as [Vector3].
 * @property bitangents Mutable list of vertex bitangents as [Vector3].
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
 * Adds a [Point] to the vertex data by updating the corresponding mutable lists of vertex attributes.
 * Each optional attribute of the point is only added if it is not null.
 *
 * @param point The 3D point to be added, which may include optional attributes such as texture coordinates,
 * color, normal, tangent, and bitangent.
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
 * Retrieves a [Point] instance from the vertex data at the specified indices.
 * The indices allow access to positions, texture coordinates, colors, normals,
 * tangents, and bitangents.
 *
 * @param index The index for retrieving the vertex position.
 * @param textureCoordsIndex The index for retrieving texture coordinates. Defaults to the value of `index`.
 * @param colorsIndex The index for retrieving vertex colors. Defaults to the value of `index`.
 * @param normalsIndex The index for retrieving vertex normals. Defaults to the value of `index`.
 * @param tangentsIndex The index for retrieving vertex tangents. Defaults to the value of `index`.
 * @param bitangentsIndex The index for retrieving vertex bitangents. Defaults to the value of `index`.
 * @return A [Point] object containing the vertex attributes at the specified indices.
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