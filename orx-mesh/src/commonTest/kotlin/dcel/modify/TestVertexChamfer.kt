package org.openrndr.extra.mesh.dcel.modify

import org.openrndr.extra.mesh.IndexedPolygon
import org.openrndr.extra.mesh.MeshData
import org.openrndr.extra.mesh.VertexData
import org.openrndr.extra.mesh.dcel.convert.toDcel
import org.openrndr.extra.mesh.dcel.query.edgesForFace
import org.openrndr.extra.mesh.dcel.query.edgesForVertex
import org.openrndr.extra.mesh.dcel.query.faceCount
import org.openrndr.extra.mesh.dcel.query.vertexCount
import org.openrndr.extra.mesh.dcel.query.verticesForFace
import org.openrndr.extra.mesh.dcel.validate.isEulerMesh
import org.openrndr.extra.mesh.generate.gridMesh
import org.openrndr.math.Vector3
import org.openrndr.shape.Rectangle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TestVertexChamfer {
    @Test
    fun testVertexChamferBoundaryCorner() {
        val dcel = gridMesh(Rectangle(0.0, 0.0, 100.0, 100.0), 2, 2).toDcel()

        val newFace = dcel.vertexChamfer(0, 2.0)

        assertEquals(5, dcel.verticesForFace(0).size)

        assertEquals(4, dcel.faceCount() )
        assertTrue(dcel.isEulerMesh())

        assertEquals(-1, newFace)

    }


    @Test
    fun testVertexChamferBoundaryEdge() {
        val dcel = gridMesh(Rectangle(0.0, 0.0, 100.0, 100.0), 3, 3).toDcel()
        // Vertex 1 is on a boundary edge.

        val faceCountBefore = dcel.faces.size
        val vertexCountBefore = dcel.vertices.size


        val newFace = dcel.vertexChamfer(1, 2.0)
        assertTrue(dcel.isEulerMesh())

        assertEquals(faceCountBefore + 1, dcel.faceCount())
        assertEquals(vertexCountBefore + 3, dcel.vertexCount())


        assertEquals(3, dcel.edgesForFace(newFace).size)
    }

    @Test
    fun testVertexChamferGrid() {
        val dcel = gridMesh(Rectangle(0.0, 0.0, 100.0, 100.0), 2, 2).toDcel()
        // Vertex 4 is the center vertex (internal).
        val faceCountBefore = dcel.faces.size
        val vertexCountBefore = dcel.vertices.size
        val edgeCountBefore = dcel.halfEdges.size

        assertEquals(4, dcel.edgesForVertex(4).size)


        val newFace = dcel.vertexChamfer(4, 2.0)

        assertEquals(-1, dcel.vertices[4].edge)

        assertEquals(edgeCountBefore + 8, dcel.halfEdges.size)

        assertEquals(faceCountBefore + 1, dcel.faces.size)
        // Center vertex had 4 outgoing edges. So 4 new vertices should be created.
        // Original vertex 4 is still there but unused? No, we don't delete it.
        assertEquals(vertexCountBefore + 4, dcel.vertices.size)
        
        // The new face should have 4 edges.
        val edges = dcel.edgesForFace(newFace)
        assertEquals(4, edges.size)

        assertTrue(dcel.halfEdges.none { it.vertex == 4 })

        val newVertices = dcel.verticesForFace(newFace)
        assertEquals(4, newVertices.size)
        for (v in newVertices) {
            assertEquals(3, dcel.edgesForVertex(v).size)
        }
    }
}
