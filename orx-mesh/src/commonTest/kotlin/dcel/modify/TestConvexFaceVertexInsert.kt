package org.openrndr.extra.mesh.dcel.modify

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.Face
import org.openrndr.extra.mesh.dcel.HalfEdge
import org.openrndr.extra.mesh.dcel.Vertex
import org.openrndr.extra.mesh.dcel.validate.isEulerMesh
import org.openrndr.math.Vector3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestConvexFaceVertexInsert {
    @Test
    fun testInsertInSquare() {
        val dcel = Dcel()
        dcel.vertices.add(Vertex(Vector3(0.0, 0.0, 0.0), 0))
        dcel.vertices.add(Vertex(Vector3(1.0, 0.0, 0.0), 1))
        dcel.vertices.add(Vertex(Vector3(1.0, 1.0, 0.0), 2))
        dcel.vertices.add(Vertex(Vector3(0.0, 1.0, 0.0), 3))

        dcel.halfEdges.add(HalfEdge(0, 0, 1, 3, -1, IntArray(5) { -1 }))
        dcel.halfEdges.add(HalfEdge(0, 1, 2, 0, -1, IntArray(5) { -1 }))
        dcel.halfEdges.add(HalfEdge(0, 2, 3, 1, -1, IntArray(5) { -1 }))
        dcel.halfEdges.add(HalfEdge(0, 3, 0, 2, -1, IntArray(5) { -1 }))

        dcel.faces.add(Face(0))

        // Insert at center
        dcel.convexFaceVertexInsert(0, Vector3(0.5, 0.5, 0.0))

        assertTrue(dcel.isEulerMesh())

        assertEquals(5, dcel.vertices.size)
        assertEquals(4, dcel.faces.size) // 1 original + 3 new
        assertEquals(12, dcel.halfEdges.size) // 4 original + 4*2 new

        // Verify all faces are triangles
        for (i in 0 until 4) {
            val edges = getFaceEdges(dcel, i)
            assertEquals(3, edges.size, "Face $i should have 3 edges")
        }
    }

    @Test
    fun testInterpolation() {
        val dcel = Dcel()
        dcel.vertices.add(Vertex(Vector3(0.0, 0.0, 0.0), 0))
        dcel.vertices.add(Vertex(Vector3(1.0, 0.0, 0.0), 1))
        dcel.vertices.add(Vertex(Vector3(1.0, 1.0, 0.0), 2))
        dcel.vertices.add(Vertex(Vector3(0.0, 1.0, 0.0), 3))

        dcel.colors.add(org.openrndr.color.ColorRGBa.RED)
        dcel.colors.add(org.openrndr.color.ColorRGBa.GREEN)
        dcel.colors.add(org.openrndr.color.ColorRGBa.BLUE)
        dcel.colors.add(org.openrndr.color.ColorRGBa.YELLOW)

        dcel.halfEdges.add(HalfEdge(0, 0, 1, 3, -1, intArrayOf(0, -1, -1, -1, -1)))
        dcel.halfEdges.add(HalfEdge(0, 1, 2, 0, -1, intArrayOf(1, -1, -1, -1, -1)))
        dcel.halfEdges.add(HalfEdge(0, 2, 3, 1, -1, intArrayOf(2, -1, -1, -1, -1)))
        dcel.halfEdges.add(HalfEdge(0, 3, 0, 2, -1, intArrayOf(3, -1, -1, -1, -1)))

        dcel.faces.add(Face(0))

        // Insert at center
        dcel.convexFaceVertexInsert(0, Vector3(0.5, 0.5, 0.0))

        val newVertexIdx = 4
        val eFrom = dcel.vertices[newVertexIdx].edge
        val colorIdx = dcel.halfEdges[eFrom].attributes[org.openrndr.extra.mesh.dcel.DCELAttributes.COLOR.index]
        val interpolatedColor = dcel.colors[colorIdx]

        // MVC weights for center of square are 0.25 each.
        // Red (1,0,0), Green (0,1,0), Blue (0,0,1), Yellow (1,1,0)
        // Average: (0.5, 0.5, 0.25)
        assertEquals(0.5, interpolatedColor.r, 0.01)
        assertEquals(0.5, interpolatedColor.g, 0.01)
        assertEquals(0.25, interpolatedColor.b, 0.01)
    }

    private fun getFaceEdges(dcel: Dcel, faceId: Int): List<Int> {
        val edges = mutableListOf<Int>()
        val startEdge = dcel.faces[faceId].edge
        var curr = startEdge
        do {
            edges.add(curr)
            curr = dcel.halfEdges[curr].nextEdge
        } while (curr != startEdge && curr != -1)
        return edges
    }
}
