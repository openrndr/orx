//package org.openrndr.extra.math.graph.planarsubgraph
//
//import org.openrndr.extra.math.graph.Edge
//import org.openrndr.extra.math.graph.Graph
//import org.openrndr.extra.math.graph.planarembedding.isPlanar
//import kotlin.test.Test
//import kotlin.test.assertSame
//import kotlin.test.assertTrue
//
//class TestPlanarSubgraphImproved {
//
//    @Test
//    fun testGrid() {
//        val g = Graph()
//
//        val seen = mutableSetOf<Pair<Int, Int>>()
//        fun addEdge(a: Int, b: Int) {
//            val x0 = a to b
//            val x1 = b to a
//            if (x0 !in seen && x1 !in seen) {
//                g.edges.add(Edge(a, b, 1.0))
//                seen.add(x0)
//                seen.add(x1)
//            }
//        }
//        for (y in 0 until 5) {
//            for (x in 0 until 5) {
//                addEdge(y * 5 + x, y * 5 + x + 1)
//                addEdge(y * 5 + x + 1, (y+1) * 5 + x + 1)
//                addEdge( (y+1) * 5 + x + 1, (y+1) * 5 + x)
//                addEdge((y+1) * 5 + x, y * 5)
//            }
//        }
//        val (planar, embedding) = g.isPlanar()
//        val face = embedding.getFace(0, 1)
//        println(face)
//        val ps = g.findPlanarSubgraphUndirectedImproved(0)
//    }
//
//
//
//    @Test
//    fun testK5() {
//        val g = Graph()
//        for (i in 0 until 5) {
//            for (j in i + 1 until 5) {
//                g.edges.add(Edge(i, j, 1.0))
//            }
//        }
//        val ps = g.findPlanarSubgraphUndirectedImproved(0)
//        assertTrue(ps.edges.size > 4, "K5 planar subgraph should have more than 4 edges, got ${ps.edges.size}")
//        assertTrue(ps.edges.size <= 9, "K5 planar subgraph should have at most 9 edges, got ${ps.edges.size}")
//        assertTrue(ps.isPlanar().first, "Resulting subgraph should be planar")
//    }
//
//    @Test
//    fun testK33() {
//        val g = Graph()
//        for (i in 0 until 3) {
//            for (j in 3 until 6) {
//                g.edges.add(Edge(i, j, 1.0))
//            }
//        }
//        val ps = g.findPlanarSubgraphUndirectedImproved(0)
//        assertTrue(ps.edges.size > 5, "K3,3 planar subgraph should have more than 5 edges, got ${ps.edges.size}")
//        assertTrue(ps.edges.size < 9, "K3,3 planar subgraph should have fewer than 9 edges, got ${ps.edges.size}")
//        assertTrue(ps.isPlanar().first, "Resulting subgraph should be planar")
//    }
//}
