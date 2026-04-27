package org.openrndr.extra.mesh.dcel.convert

import org.openrndr.extra.mesh.dcel.query.edgeObjectsForFace
import org.openrndr.shape.Circle
import org.openrndr.shape.Shape
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestShapeToDcel {
    @Test
    fun testCircleToDcel() {
        val circle = Circle(0.0, 0.0, 10.0).shape
        val dcel = shapeToDcel(circle, 1.0)
        
        // A circle sampled with distance tolerance 1.0 will have many triangles
        assertTrue(dcel.vertices.size > 0)
        assertTrue(dcel.faces.size > 0)
        assertTrue(dcel.halfEdges.size > 0)
        
        println("Triangulated - Vertices: ${dcel.vertices.size}, Faces: ${dcel.faces.size}, Edges: ${dcel.halfEdges.size}")
    }

    @Test
    fun testCircleNoTriangulation() {
        val circle = Circle(0.0, 0.0, 10.0).shape
        // Circle.shape is typically CW (outline)
        val dcel = shapeToDcelNoTriangulation(circle, 1.0)

        // No triangulation means it should have only one face (the circle itself)
        assertTrue(dcel.vertices.size > 0)
        assertEquals(1, dcel.faces.size)
        assertEquals(dcel.vertices.size, dcel.halfEdges.size) // Each vertex has one outgoing edge in a single polygon

        println("No Triangulation - Vertices: ${dcel.vertices.size}, Faces: ${dcel.faces.size}, Edges: ${dcel.halfEdges.size}")
    }

    @Test
    fun testShapeWithHoleNoTriangulation() {
        val outer = org.openrndr.shape.Rectangle(-10.0, -10.0, 20.0, 20.0).contour
        val inner = org.openrndr.shape.Rectangle(-5.0, -5.0, 10.0, 10.0).contour.reversed
        val shapeWithHole = Shape(listOf(outer, inner))

        val dcel = shapeToDcelNoTriangulation(shapeWithHole, 1.0)

        // Outlines should map to a face, holes should have no face
        assertEquals(1, dcel.faces.size)
        
        assertEquals(1, dcel.faces[0].holeEdges.size)
        assertEquals(4, dcel.edgeObjectsForFace(0).size)
    }
}
