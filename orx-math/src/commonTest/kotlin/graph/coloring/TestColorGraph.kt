package org.openrndr.extra.math.graph.coloring

import org.openrndr.extra.math.graph.Edge
import org.openrndr.extra.math.graph.Graph
import kotlin.test.Test
import kotlin.test.assertTrue

class TestColorGraph {
    @Test
    fun testColorSimple() {
        val g = Graph()
        g.edges.add(Edge(0, 1))
        g.edges.add(Edge(1, 2))
        g.edges.add(Edge(2, 0))

        val colors = colorGraph(g, 3)
        assertTrue(colors.size == 3)
        assertTrue(colors[0] != colors[1])
        assertTrue(colors[1] != colors[2])
        assertTrue(colors[2] != colors[0])
        assertTrue(colors.all { it in 0 until 3 })
    }

    @Test
    fun testColorGrid() {
        val g = Graph()
        // 2x2 grid
        // 0 - 1
        // |   |
        // 2 - 3
        g.edges.add(Edge(0, 1))
        g.edges.add(Edge(1, 3))
        g.edges.add(Edge(3, 2))
        g.edges.add(Edge(2, 0))

        val colors = colorGraph(g, 2)
        assertTrue(colors.size == 4)
        for (edge in g.edges) {
            assertTrue(colors[edge.source] != colors[edge.target])
        }
        assertTrue(colors.all { it in 0 until 2 })
    }
}
