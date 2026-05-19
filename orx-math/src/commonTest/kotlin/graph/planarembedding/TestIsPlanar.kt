package org.openrndr.extra.math.graph.planarembedding

import org.openrndr.extra.math.graph.Edge
import org.openrndr.extra.math.graph.Graph
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestIsPlanar {
    @Test
    fun testK4() {
        val g = Graph()
        for (i in 0 until 4) {
            for (j in i + 1 until 4) {
                g.edges.add(Edge(i, j))
            }
        }
        assertTrue(g.isPlanar().first)
    }

    @Test
    fun testK5() {
        val g = Graph()
        for (i in 0 until 5) {
            for (j in i + 1 until 5) {
                g.edges.add(Edge(i, j))
            }
        }
        assertFalse(g.isPlanar().first)
    }

    @Test
    fun testK33() {
        val g = Graph()
        for (i in 0 until 3) {
            for (j in 3 until 6) {
                g.edges.add(Edge(i, j))
            }
        }
        assertFalse(g.isPlanar().first)
    }
}
