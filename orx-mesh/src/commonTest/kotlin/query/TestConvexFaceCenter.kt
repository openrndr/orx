package query

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.Face
import org.openrndr.extra.mesh.dcel.HalfEdge
import org.openrndr.extra.mesh.dcel.Vertex
import org.openrndr.extra.mesh.dcel.query.convexFaceCenter
import org.openrndr.math.Vector3
import kotlin.test.Test
import kotlin.test.assertEquals

class TestConvexFaceCenter {
    @Test
    fun testTriangleCenter() {
        val dcel = Dcel()
        dcel.vertices.add(Vertex(Vector3(0.0, 0.0, 0.0), 0))
        dcel.vertices.add(Vertex(Vector3(1.0, 0.0, 0.0), 1))
        dcel.vertices.add(Vertex(Vector3(0.0, 1.0, 0.0), 2))

        val face = Face(0)
        dcel.faces.add(face)

        val e0 = HalfEdge(0, 0, 1, 2, -1, IntArray(0))
        val e1 = HalfEdge(0, 1, 2, 0, -1, IntArray(0))
        val e2 = HalfEdge(0, 2, 0, 1, -1, IntArray(0))

        dcel.halfEdges.add(e0)
        dcel.halfEdges.add(e1)
        dcel.halfEdges.add(e2)

        val center = dcel.convexFaceCenter(0)
        val expected = Vector3(1.0/3.0, 1.0/3.0, 0.0)
        assertEquals(expected.x, center.x, 1e-6)
        assertEquals(expected.y, center.y, 1e-6)
        assertEquals(expected.z, center.z, 1e-6)
    }

    @Test
    fun testSquareCenter() {
        val dcel = Dcel()
        dcel.vertices.add(Vertex(Vector3(0.0, 0.0, 0.0), 0))
        dcel.vertices.add(Vertex(Vector3(1.0, 0.0, 0.0), 1))
        dcel.vertices.add(Vertex(Vector3(1.0, 1.0, 0.0), 2))
        dcel.vertices.add(Vertex(Vector3(0.0, 1.0, 0.0), 3))

        val face = Face(0)
        dcel.faces.add(face)

        val e0 = HalfEdge(0, 0, 1, 3, -1, IntArray(0))
        val e1 = HalfEdge(0, 1, 2, 0, -1, IntArray(0))
        val e2 = HalfEdge(0, 2, 3, 1, -1, IntArray(0))
        val e3 = HalfEdge(0, 3, 0, 2, -1, IntArray(0))

        dcel.halfEdges.add(e0)
        dcel.halfEdges.add(e1)
        dcel.halfEdges.add(e2)
        dcel.halfEdges.add(e3)

        val center = dcel.convexFaceCenter(0)
        val expected = Vector3(0.5, 0.5, 0.0)
        assertEquals(expected.x, center.x, 1e-6)
        assertEquals(expected.y, center.y, 1e-6)
        assertEquals(expected.z, center.z, 1e-6)
    }
}