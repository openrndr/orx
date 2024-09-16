package org.openrndr.extra.objloader

import org.openrndr.color.ColorRGBa
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Box
import kotlin.math.max
import kotlin.math.min


/**
 * 3D Polygon interface
 */
interface IPolygon {
    val positions: List<Vector3>
    val normals: List<Vector3>
    val textureCoords: List<Vector2>
    val colors: List<ColorRGBa>
    val tangents: List<Vector3>
    val bitangents: List<Vector3>

    fun transform(t: Matrix44): IPolygon
}

/**
 * Immutable 3D Polygon implementation
 *
 * @property positions Vertex 3D positions
 * @property normals Vertex 3D normals
 * @property textureCoords Vertex 2D texture coordinates
 * @constructor Create empty 3D Polygon
 */
class Polygon(
    override val positions: List<Vector3> = emptyList(),
    override val normals: List<Vector3> = emptyList(),
    override val textureCoords: List<Vector2> = emptyList(),
    override val colors: List<ColorRGBa> = emptyList(),
    override val tangents: List<Vector3> = emptyList(),
    override val bitangents: List<Vector3> = emptyList(),
) : IPolygon {
    override fun transform(t: Matrix44): Polygon {
        return Polygon(positions.map { (t * it.xyz1).div }, normals, textureCoords, colors, tangents, bitangents)
    }

    /**
     * Create a [MutablePolygon] by copying
     */
    fun toMutablePolygon(): MutablePolygon {
        return MutablePolygon(
            positions.toMutableList(),
            normals.toMutableList(),
            textureCoords.toMutableList(),
            colors.toMutableList(),
            tangents.toMutableList(),
            bitangents.toMutableList()
        )
    }
}

/**
 * Mutable 3D Polygon implementation
 */
class MutablePolygon(
    override val positions: MutableList<Vector3> = mutableListOf(),
    override val normals: MutableList<Vector3> = mutableListOf(),
    override val textureCoords: MutableList<Vector2> = mutableListOf(),
    override val colors: MutableList<ColorRGBa> = mutableListOf(),
    override val tangents: MutableList<Vector3> = mutableListOf(),
    override val bitangents: MutableList<Vector3> = mutableListOf()

) : IPolygon {
    override fun transform(t: Matrix44): MutablePolygon {
        return MutablePolygon(
            positions.map { (t * it.xyz1).div }.toMutableList(),
            ArrayList(normals),
            ArrayList(textureCoords),
            ArrayList(colors),
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
                normals = if (p.normals.isNotEmpty()) indices else emptyList(),
                colors = if (p.colors.isNotEmpty()) indices else emptyList(),
                tangents = if (p.tangents.isNotEmpty()) indices else emptyList(),
                bitangents = if (p.bitangents.isNotEmpty()) indices else emptyList()
            )
        )

        vertexOffset += p.positions.size
    }
    return MeshData(vertexData.toVertexData(), indexedPolygons)
}