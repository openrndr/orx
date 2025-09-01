package org.openrndr.extra.mesh

import org.openrndr.color.ColorRGBa
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Box
import kotlin.math.max
import kotlin.math.min


/**
 * Represents a polygon in 3D space, defined by a collection of attributes such as positions, texture coordinates,
 * colors, normals, tangents, and bitangents.
 *
 * @property positions The list of 3D positions of the polygon vertices represented as [Vector3].
 * @property textureCoords The list of 2D texture coordinates for the polygon vertices represented as [Vector2].
 * @property colors The list of color values for the polygon vertices represented as [ColorRGBa].
 * @property normals The list of normal vectors for the polygon vertices represented as [Vector3].
 * @property tangents The list of tangent vectors for the polygon vertices represented as [Vector3].
 * @property bitangents The list of bitangent vectors for the polygon vertices represented as [Vector3].
 */
interface IPolygon {
    val positions: List<Vector3>
    val textureCoords: List<Vector2>
    val colors: List<ColorRGBa>
    val normals: List<Vector3>
    val tangents: List<Vector3>
    val bitangents: List<Vector3>

    /**
     * Transforms the polygon using a given 4x4 transformation matrix.
     *
     * @param t The 4x4 transformation matrix to apply to the polygon.
     * @return A new polygon instance resulting from applying the transformation.
     */
    fun transform(t: Matrix44): IPolygon
}

/**
 * Represents a polygon in 3D space with immutable attributes such as positions, texture coordinates,
 * colors, normals, tangents, and bitangents. Provides methods to transform the polygon and convert
 * it to a mutable version.
 *
 * @constructor Creates a Polygon with specified attributes.
 * @param positions The list of 3D positions of the polygon vertices.
 * @param textureCoords The list of 2D texture coordinates for the polygon vertices.
 * @param colors The list of color values for the polygon vertices.
 * @param normals The list of normal vectors for the polygon vertices.
 * @param tangents The list of tangent vectors for the polygon vertices.
 * @param bitangents The list of bitangent vectors for the polygon vertices.
 */
class Polygon(
    override val positions: List<Vector3> = emptyList(),
    override val textureCoords: List<Vector2> = emptyList(),
    override val colors: List<ColorRGBa> = emptyList(),
    override val normals: List<Vector3> = emptyList(),
    override val tangents: List<Vector3> = emptyList(),
    override val bitangents: List<Vector3> = emptyList(),
) : IPolygon {
    override fun transform(t: Matrix44): Polygon {
        return Polygon(positions.map { (t * it.xyz1).div }, textureCoords, colors, normals, tangents, bitangents)
    }

    /**
     * Converts the current immutable polygon instance into a mutable polygon.
     *
     * @return A [MutablePolygon] instance containing mutable lists of positions, texture coordinates,
     * colors, normals, tangents, and bitangents copied from the current polygon.
     */
    fun toMutablePolygon(): MutablePolygon {
        return MutablePolygon(
            positions.toMutableList(),
            textureCoords.toMutableList(),
            colors.toMutableList(),
            normals.toMutableList(),
            tangents.toMutableList(),
            bitangents.toMutableList()
        )
    }
}

/**
 * A mutable implementation of the [IPolygon] interface that represents a polygon in 3D space.
 * This class allows modification of the polygon's attributes such as vertex positions,
 * texture coordinates, colors, normals, tangents, and bitangents.
 *
 * @constructor Creates a mutable polygon with optional initial values for positions, texture coordinates,
 * colors, normals, tangents, and bitangents. If no initial values are provided, empty mutable lists are used.
 *
 * @property positions The mutable list of 3D positions of the polygon vertices, represented as [Vector3].
 * @property textureCoords The mutable list of 2D texture coordinates for the polygon vertices, represented as [Vector2].
 * @property colors The mutable list of color values for the polygon vertices, represented as [ColorRGBa].
 * @property normals The mutable list of normal vectors for the polygon vertices, represented as [Vector3].
 * @property tangents The mutable list of tangent vectors for the polygon vertices, represented as [Vector3].
 * @property bitangents The mutable list of bitangent vectors for the polygon vertices, represented as [Vector3].
 */
class MutablePolygon(
    override val positions: MutableList<Vector3> = mutableListOf(),
    override val textureCoords: MutableList<Vector2> = mutableListOf(),
    override val colors: MutableList<ColorRGBa> = mutableListOf(),
    override val normals: MutableList<Vector3> = mutableListOf(),
    override val tangents: MutableList<Vector3> = mutableListOf(),
    override val bitangents: MutableList<Vector3> = mutableListOf()

) : IPolygon {
    override fun transform(t: Matrix44): MutablePolygon {
        return MutablePolygon(
            positions.map { (t * it.xyz1).div }.toMutableList(),
            ArrayList(textureCoords),
            ArrayList(colors),
            ArrayList(normals),
            ArrayList(tangents),
            ArrayList(bitangents)
        )
    }
}


/**
 * Calculate the 3D bounding box of a list of [IPolygon].
 */
fun bounds(polygons: List<IPolygon>): Box {
    var minX = Double.POSITIVE_INFINITY
    var minY = Double.POSITIVE_INFINITY
    var minZ = Double.POSITIVE_INFINITY

    var maxX = Double.NEGATIVE_INFINITY
    var maxY = Double.NEGATIVE_INFINITY
    var maxZ = Double.NEGATIVE_INFINITY

    polygons.forEach {
        it.positions.forEach { pos ->
            minX = min(minX, pos.x)
            minY = min(minY, pos.y)
            minZ = min(minZ, pos.z)

            maxX = max(maxX, pos.x)
            maxY = max(maxY, pos.y)
            maxZ = max(maxZ, pos.z)
        }
    }
    return Box(Vector3(minX, minY, minZ), maxX - minX, maxY - minY, maxZ - minZ)
}


/**
 * Convert list of polygons to [MeshData]
 */
fun List<IPolygon>.toMeshData(): MeshData {
    val vertexData = MutableVertexData()

    for (p in this) {
        vertexData.positions.addAll(p.positions)
        vertexData.normals.addAll(p.normals)
        vertexData.colors.addAll(p.colors)
        vertexData.textureCoords.addAll(p.textureCoords)
        vertexData.tangents.addAll(p.tangents)
        vertexData.bitangents.addAll(p.bitangents)
    }

    val indexedPolygons = mutableListOf<IndexedPolygon>()

    var vertexOffset = 0
    for (p in this) {

        val indices = (vertexOffset until vertexOffset + p.positions.size).toList()

        indexedPolygons.add(
            IndexedPolygon(
                positions = indices,
                textureCoords = if (p.textureCoords.isNotEmpty()) indices else emptyList(),
                colors = if (p.colors.isNotEmpty()) indices else emptyList(),
                normals = if (p.normals.isNotEmpty()) indices else emptyList(),
                tangents = if (p.tangents.isNotEmpty()) indices else emptyList(),
                bitangents = if (p.bitangents.isNotEmpty()) indices else emptyList()
            )
        )

        vertexOffset += p.positions.size
    }
    return MeshData(vertexData.toVertexData(), indexedPolygons)
}