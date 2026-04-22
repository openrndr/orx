package org.openrndr.extra.mesh.dcel.query

import org.openrndr.extra.mesh.MeshData
import org.openrndr.extra.mesh.VertexData
import org.openrndr.extra.mesh.IndexedPolygon
import org.openrndr.extra.mesh.dcel.convert.toDcel
import org.openrndr.math.Vector3
import kotlin.test.Test
import kotlin.test.assertEquals

class TestEdgesForFace {
    @Test
    fun testEdgesForFace() {
        val meshData = MeshData(
            VertexData(
                positions = listOf(
                    Vector3(0.0, 0.0, 0.0),
                    Vector3(1.0, 0.0, 0.0),
                    Vector3(1.0, 1.0, 0.0),
                    Vector3(0.0, 1.0, 0.0)
                )
            ),
            polygons = listOf(
                IndexedPolygon(
                    positions = listOf(0, 1, 2, 3),
                    textureCoords = emptyList(),
                    colors = emptyList(),
                    normals = emptyList(),
                    tangents = emptyList(),
                    bitangents = emptyList()
                )
            )
        )

        val dcel = meshData.toDcel()
        val edges = dcel.edgesForFace(0)

        assertEquals(4, edges.size)
        assertEquals(0, edges[0].vertex)
        assertEquals(1, edges[1].vertex)
        assertEquals(2, edges[2].vertex)
        assertEquals(3, edges[3].vertex)
    }
}
