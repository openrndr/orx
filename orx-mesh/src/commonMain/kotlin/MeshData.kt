package org.openrndr.extra.objloader

import kotlin.jvm.JvmRecord

interface IMeshData {
    val vertexData: IVertexData
    val polygons: List<IIndexedPolygon>
    fun triangulate(): IMeshData
    fun flattenPolygons(): List<IPolygon>
}

@JvmRecord
data class MeshData(
    override val vertexData: VertexData,
    override val polygons: List<IndexedPolygon>,
) : IMeshData {
    override fun triangulate(): MeshData {
        return copy(polygons = polygons.flatMap { polygon -> polygon.triangulate(vertexData) })
    }

    override fun flattenPolygons(): List<Polygon> {
        return polygons.map { ip ->
            ip.toPolygon(vertexData)
        }
    }
}


data class MutableMeshData(
    override val vertexData: MutableVertexData,
    override val polygons: MutableList<IndexedPolygon>
) : IMeshData {
    override fun triangulate(): MutableMeshData {
        return copy(polygons = polygons.flatMap { it.triangulate(vertexData) }.toMutableList())
    }

    override fun flattenPolygons(): List<Polygon> {
        return polygons.map { it.toPolygon(vertexData) }

    }
}