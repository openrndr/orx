package org.openrndr.extra.math.graph.mst

import org.openrndr.extra.math.graph.Edge
import org.openrndr.extra.math.graph.Graph
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestMinimumSpanningTree {
    @Test
    fun testMSTSimple() {
        val g = Graph()
        // 0 --(1)-- 1 --(2)-- 2
        //  \________(4)_______/
        g.edges.add(Edge(0, 1, 1.0))
        g.edges.add(Edge(1, 2, 2.0))
        g.edges.add(Edge(0, 2, 4.0))

        val mst = g.minimumSpanningTreeUndirected(0)
        
        assertEquals(2, mst.edges.size)
        assertTrue(mst.edges.any { it.source == 0 && it.target == 1 && it.weight == 1.0 })
        assertTrue(mst.edges.any { it.source == 1 && it.target == 2 && it.weight == 2.0 })
    }

    @Test
    fun testMSTGrid() {
        val g = Graph()
        // 0 -(1)- 1
        // |(10)   |(2)
        // 2 -(1)- 3
        g.edges.add(Edge(0, 1, 1.0))
        g.edges.add(Edge(1, 3, 2.0))
        g.edges.add(Edge(3, 2, 1.0))
        g.edges.add(Edge(2, 0, 10.0))

        val mst = g.minimumSpanningTreeUndirected(0)

        assertEquals(3, mst.edges.size)
        // Should have edges (0,1), (1,3), (3,2)
        assertTrue(mst.edges.any { it.source == 0 && it.target == 1 })
        assertTrue(mst.edges.any { it.source == 1 && it.target == 3 })
        assertTrue(mst.edges.any { it.source == 3 && it.target == 2 })
    }

    @Test
    fun testMSTDisconnected() {
        val g = Graph()
        g.edges.add(Edge(0, 1, 1.0))
        g.edges.add(Edge(2, 3, 1.0))

        val mst = g.minimumSpanningTreeUndirected(0)
        assertEquals(1, mst.edges.size)
        assertTrue(mst.edges.all { it.source == 0 && it.target == 1 })
    }
}
