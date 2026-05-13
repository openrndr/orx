package org.openrndr.extra.mesh.dcel.modify

import org.openrndr.extra.mesh.IndexedPolygon
import org.openrndr.extra.mesh.MeshData
import org.openrndr.extra.mesh.VertexData
import org.openrndr.extra.mesh.dcel.convert.shapeToDcelNoTriangulation
import org.openrndr.extra.mesh.dcel.convert.toDcel
import org.openrndr.extra.mesh.dcel.navigate.allFaces
import org.openrndr.extra.mesh.dcel.query.edgesForFace
import org.openrndr.extra.mesh.dcel.query.faceCount
import org.openrndr.extra.mesh.dcel.validate.isEulerMesh
import org.openrndr.extra.mesh.generate.gridMesh
import org.openrndr.extra.shapes.primitives.Plane
import org.openrndr.math.Vector3
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestFaceSetSplit {

    @Test
    fun testFaceSetSplitHole() {
        val bounds = Rectangle(0.0, 0.0, 720.0, 720.0)

        val outer = bounds.offsetEdges(-100.0).contour
        val inner = bounds.offsetEdges(-200.0).contour.contour.reversed
        val shapeWithHole = Shape(listOf(outer, inner))


        val dcel = shapeToDcelNoTriangulation(shapeWithHole, 1.0)

        dcel.faceSetSplit(dcel.allFaces().toSet(), Plane(Vector3(1.0, 1.0, 0.0).normalized, 720*0.5*sqrt(2.0)))

        assertEquals(2, dcel.faceCount())

        for (i in 0 until 2) {
            assertEquals(6,dcel.edgesForFace(i).size)
            assertEquals(0, dcel.faces[i].holeEdges.size)
        }

    }

    @Test
    fun testFaceSplitGrid() {
        val grid = gridMesh(Rectangle(0.0, 0.0, 720.0, 720.0), 5, 5)
        val dcel = grid.toDcel()

        val ogCount = dcel.faceCount()
        dcel.faceSetSplit(dcel.allFaces().toSet(), Plane(Vector3(1.0, 1.0, 0.0).normalized, 720*0.5*sqrt(2.0)))
        assertEquals(ogCount + 5, dcel.faceCount())

    }

    @Test
    fun testFaceSetSplitSingleTriangle() {
        // Single triangle in XY plane
        val meshData = MeshData(
            VertexData(
                positions = listOf(
                    Vector3(0.0, 0.0, 0.0), // 0
                    Vector3(2.0, 0.0, 0.0), // 1
                    Vector3(1.0, 2.0, 0.0)  // 2
                )
            ),
            polygons = listOf(
                IndexedPolygon(
                    positions = listOf(0, 1, 2),
                    textureCoords = emptyList(),
                    colors = emptyList(),
                    normals = emptyList(),
                    tangents = emptyList(),
                    bitangents = emptyList()
                )
            )
        )

        val dcel = meshData.toDcel()
        
        // Plane x = 1.0. Normal (1, 0, 0), distance 1.0. side(p) = p.x - 1.0
        val plane = Plane(Vector3(1.0, 0.0, 0.0), 1.0)
        
        val newFaceIds = dcel.faceSetSplit(setOf(0), plane)
        
        assertTrue(dcel.isEulerMesh())
        // Should have split into 2 faces
        assertEquals(2, dcel.faces.size)
        assertEquals(2, newFaceIds.size)
        assertTrue(newFaceIds.contains(0))
        assertTrue(newFaceIds.contains(1))
    }

    @Test
    fun testFaceSetSplitSharedEdge() {
        // Two triangles sharing edge (1, 2). Plane x=1.5 splits both.
        val meshData = MeshData(
            VertexData(
                positions = listOf(
                    Vector3(0.0, 0.0, 0.0), // 0
                    Vector3(2.0, 0.0, 0.0), // 1
                    Vector3(2.0, 2.0, 0.0), // 2
                    Vector3(4.0, 0.0, 0.0)  // 3
                )
            ),
            polygons = listOf(
            IndexedPolygon(positions = listOf(0, 1, 2), textureCoords = emptyList(), colors = emptyList(), normals = emptyList(), tangents = emptyList(), bitangents = emptyList()),
            IndexedPolygon(positions = listOf(1, 3, 2), textureCoords = emptyList(), colors = emptyList(), normals = emptyList(), tangents = emptyList(), bitangents = emptyList())
        )
        )
        val dcel = meshData.toDcel()
        val plane = Plane(Vector3(1.0, 0.0, 0.0), 1.5)
        
        val newFaceIds = dcel.faceSetSplit(setOf(0, 1), plane)
        
        assertTrue(dcel.isEulerMesh())
        // Only the left triangle crosses x=1.5, so we expect 3 faces total
        assertEquals(3, dcel.faces.size)
        assertEquals(3, newFaceIds.size)
    }
}
