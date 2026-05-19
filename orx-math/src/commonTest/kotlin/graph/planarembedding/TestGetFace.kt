package org.openrndr.extra.math.graph.planarembedding

import org.openrndr.extra.math.graph.Edge
import org.openrndr.extra.math.graph.Graph
import org.openrndr.extra.math.graph.mst.minimumSpanningTreeUndirected
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class TestGetFace {

    @Test
    fun testEmptyFace() {
        val g = Graph()
        for (i in 0 until 5) {
            for (j in i + 1 until 5) {
                g.edges.add(Edge(i, j))
            }
        }
        val mst = g.minimumSpanningTreeUndirected(0)
        val (planar, embedding) = mst.isPlanar()

        val sharedFace = embedding.sharedFace(1, 4)
        println(sharedFace)
    }


    @Test
    fun testSquareFace() {
        val embedding = PlanarEmbedding()
        // 0 -- 1
        // |    |
        // 3 -- 2
        // Neighbors in CW order:
        // 0: 1, 3
        // 1: 2, 0
        // 2: 3, 1
        // 3: 0, 2
        
        embedding.addHalfEdge(0, 1)
        embedding.addHalfEdge(0, 3)
        embedding.addHalfEdge(1, 0)
        embedding.addHalfEdge(1, 2)
        embedding.addHalfEdge(2, 1)
        embedding.addHalfEdge(2, 3)
        embedding.addHalfEdge(3, 2)
        embedding.addHalfEdge(3, 0)
        
        // Ensure CW order by setting next neighbors
        // 0: 1 -> 3
        embedding.setNextCwNeighbor(0, 1, 3)
        // 1: 2 -> 0
        embedding.setNextCwNeighbor(1, 2, 0)
        // 2: 3 -> 1
        embedding.setNextCwNeighbor(2, 3, 1)
        // 3: 0 -> 2
        embedding.setNextCwNeighbor(3, 0, 2)

        // Face containing (0, 1) CCW: 0, 1, 2, 3
        // From (0, 1): 
        // next is neighbor of 1 before 0 in CW: 2
        // next edge (1, 2)
        // next is neighbor of 2 before 1 in CW: 3
        // next edge (2, 3)
        // next is neighbor of 3 before 2 in CW: 0
        // next edge (3, 0)
        // next is neighbor of 0 before 3 in CW: 1
        // next edge (0, 1) -> LOOP
        
        val face01 = embedding.getFace(0, 1)
        assertEquals(listOf(0, 1, 2, 3), face01)
        
        // Face containing (1, 0) CW: 1, 0, 3, 2
        // From (1, 0):
        // next is neighbor of 0 before 1 in CW: 3
        // next edge (0, 3)
        // next is neighbor of 3 before 0 in CW: 2
        // next edge (3, 2)
        // next is neighbor of 2 before 3 in CW: 1
        // next edge (2, 1)
        // next is neighbor of 1 before 2 in CW: 0
        // next edge (1, 0) -> LOOP
        
        val face10 = embedding.getFace(1, 0)
        assertEquals(listOf(1, 0, 3, 2), face10)

        embedding.checkStructure()
    }
}
