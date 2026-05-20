package graph.planarembedding

import org.openrndr.extra.math.graph.Edge
import org.openrndr.extra.math.graph.Graph
import org.openrndr.extra.math.graph.planarembedding.isPlanar
import org.openrndr.extra.math.graph.planarsubgraph.findPlanarSubgraphUndirectedImproved
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestPlanarEmbedding {

    @Test
    fun testGrid() {
        val g = Graph()

        val seen = mutableSetOf<Pair<Int, Int>>()
        fun addEdge(a: Int, b: Int) {
            val x0 = a to b
            val x1 = b to a
            if (x0 !in seen && x1 !in seen) {
                g.edges.add(Edge(a, b, 1.0))
                seen.add(x0)
                seen.add(x1)
            }
        }
        for (y in 0 until 4) {
            for (x in 0 until 4) {
                addEdge(y * 5 + x, y * 5 + x + 1)
                addEdge(y * 5 + x + 1, (y + 1) * 5 + x + 1)
                addEdge((y + 1) * 5 + x + 1, (y + 1) * 5 + x)
                addEdge((y + 1) * 5 + x, y * 5 + x)
            }
        }

        assertEquals(25, (g.edges.map { it.source } + g.edges.map { it.target }).distinct().size)
        assertEquals((0 until 25).toList(), (g.edges.map { it.source } + g.edges.map { it.target }).distinct().sorted())

        val (planar, embedding) = g.isPlanar()
        assertTrue(planar)
        assertEquals(40, g.edges.size)
        assertEquals(80, embedding.edges.size)


        // do we get the outer face when we query in reversed order?
        val outerFace = embedding.getFace(1, 0)

        // do we get an inner face when we query in order?
        val f0 = embedding.getFace(0, 1)
        assertEquals(4, f0.size)

        // do we get an inner face when we query in order?
        val f1 = embedding.getFace(1, 2)
        assertEquals(4, f1.size)



    }


}