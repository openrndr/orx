package org.openrndr.extra.mesh

import kotlin.jvm.JvmRecord

/**
 * Mesh data interface
 */
interface IMeshData {
    val vertexData: IVertexData
    val polygons: List<IIndexedPolygon>
    fun triangulate(): IMeshData
    fun toPolygons(): List<IPolygon>
}

/**
 * Immutable mesh data implementation
 */
@JvmRecord
data class MeshData(
    override val vertexData: VertexData,
    override val polygons: List<IndexedPolygon>,
) : IMeshData {
    override fun triangulate(): MeshData {
        return if (isTriangular()) {
            this
        } else
            copy(polygons = polygons.flatMap { polygon -> polygon.triangulate(vertexData) })
    }

    override fun toPolygons(): List<Polygon> {
        return polygons.map { ip ->
            ip.toPolygon(vertexData)
        }
    }
}


/**
 * Mutable mesh data implementation
 */
data class MutableMeshData(
    override val vertexData: MutableVertexData,
    override val polygons: MutableList<IndexedPolygon>
) : IMeshData {
    override fun triangulate(): MutableMeshData {
        return if (isTriangular()) {
            this
        } else {
            copy(polygons = polygons.flatMap { it.triangulate(vertexData) }.toMutableList())
        }
    }

    override fun toPolygons(): List<Polygon> {
        return polygons.map { it.toPolygon(vertexData) }
    }
}