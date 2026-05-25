package org.openrndr.extra.math.graph.planarsubgraph

import org.openrndr.extra.math.graph.Edge
import org.openrndr.extra.math.graph.Graph
import kotlin.test.Test
import kotlin.test.assertTrue

class TestPlanarSubgraph {
    @Test
    fun testK5() {
        val g = Graph()
        for (i in 0 until 5) {
            for (j in i + 1 until 5) {
                g.edges.add(Edge(i, j, 1.0))
            }
        }
        // K5 has 10 edges. Maximum planar graph with 5 vertices has 3*5 - 6 = 9 edges.
        val ps = g.findPlanarSubgraphUndirected(0)
        assertTrue(ps.edges.size <= 9, "K5 planar subgraph should have at most 9 edges, got ${ps.edges.size}")
    }

    @Test
    fun testK33() {
        val g = Graph()
        for (i in 0 until 3) {
            for (j in 3 until 6) {
                g.edges.add(Edge(i, j, 1.0))
            }
        }
        // K3,3 has 9 edges. It's not planar.
        // For a bipartite planar graph, E <= 2V - 4. 2*6 - 4 = 8.
        val ps = g.findPlanarSubgraphUndirected(0)
        assertTrue(ps.edges.size < 9, "K3,3 planar subgraph should have fewer than 9 edges, got ${ps.edges.size}")
    }
}
