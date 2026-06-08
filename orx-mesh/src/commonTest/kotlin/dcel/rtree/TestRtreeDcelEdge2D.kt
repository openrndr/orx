package dcel.rtree

import org.openrndr.extra.mesh.dcel.convert.toDcel
import org.openrndr.extra.mesh.dcel.navigate.allEdges
import org.openrndr.extra.mesh.generate.gridMesh
import org.openrndr.extra.mesh.rtree.RtreeDcelEdge2D
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import kotlin.test.Test
import kotlin.test.assertEquals

class TestRtreeDcelEdge2D {
    @Test
    fun testRtreeFindKNearest() {
        val grid = gridMesh(Rectangle(0.0, 0.0, 720.0, 720.0), 10, 10)
        val dcel = grid.toDcel()

        val rtree = RtreeDcelEdge2D(dcel)
        for (edge in dcel.allEdges()) {
            rtree.insert(edge)
        }
        val nearest1 = rtree.findKNearest(Vector2(360.0, 360.0), 1)
        assertEquals(1, nearest1.size)

        val nearest2 = rtree.findKNearest(Vector2(360.0, 360.0), 2)
        assertEquals(2, nearest2.size)

        val nearest3 = rtree.findKNearest(Vector2(360.0, 360.0), 3)
        assertEquals(3, nearest3.distinct().size)

    }
}