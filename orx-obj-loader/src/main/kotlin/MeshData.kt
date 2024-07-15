package org.openrndr.extra.objloader

@JvmRecord
data class MeshData(val vertexData: VertexData, val polygonGroups: Map<String, List<IndexedPolygon>>) {
    fun triangulate(): MeshData {
        return copy(polygonGroups = polygonGroups.mapValues {
            it.value.flatMap { polygon -> polygon.triangulate(vertexData) }
        })
    }

    fun flattenPolygons(): Map<String, List<Polygon>> {
        return polygonGroups.mapValues {
            it.value.map { ip ->
                ip.toPolygon(vertexData)
            }
        }
    }
}