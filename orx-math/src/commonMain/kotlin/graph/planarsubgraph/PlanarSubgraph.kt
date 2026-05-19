package org.openrndr.extra.math.graph.planarsubgraph

import org.openrndr.extra.math.graph.Edge
import org.openrndr.extra.math.graph.Graph
import org.openrndr.extra.math.graph.mst.minimumSpanningTreeUndirected

fun Graph.findPlanarSubgraphUndirected(root: Int): Graph {
    // treat as undirected graph

    // first find a mst, which is guaranteed planar
    val subgraph = minimumSpanningTreeUndirected(root)

    val allEdges = edges.map { if (it.source < it.target) it else Edge(it.target, it.source, it.weight) }
        .distinctBy { it.source to it.target }
        .sortedByDescending { it.weight }

    val subgraphEdges = subgraph.edges.toMutableSet()

    for (edge in allEdges) {
        if (edge !in subgraphEdges && !subgraphEdges.any { it.source == edge.target && it.target == edge.source }) {
            // treat as undirected graph
            // greedily add edges to subgraph
            // if after adding an edge to subgraph we find that the subgraph contains K3,3 (thus it is no longer planar) we remove the edge again
            // For now, a very simple (and incorrect for general case but better than nothing) check:
            // if it doesn't form K5 or K3,3. But checking for minors is hard.
            // Let's use the property that for planar graphs E <= 3V - 6.
            // For bipartite planar graphs, E <= 2V - 4.
            
            val vertices = mutableSetOf<Int>()
            for (e in subgraphEdges) {
                vertices.add(e.source)
                vertices.add(e.target)
            }
            vertices.add(edge.source)
            vertices.add(edge.target)
            
            val isBipartite = run {
                val color = mutableMapOf<Int, Int>()
                var bipartite = true
                for (v in vertices) {
                    if (v !in color) {
                        val stack = mutableListOf(v to 0)
                        while (stack.isNotEmpty()) {
                            val (curr, c) = stack.removeAt(stack.size - 1)
                            if (curr in color) {
                                if (color[curr] != c) {
                                    bipartite = false
                                    break
                                }
                            } else {
                                color[curr] = c
                                for (e in subgraphEdges) {
                                    if (e.source == curr) {
                                        stack.add(e.target to 1 - c)
                                    } else if (e.target == curr) {
                                        stack.add(e.source to 1 - c)
                                    }
                                }
                                if (edge.source == curr) {
                                    stack.add(edge.target to 1 - c)
                                } else if (edge.target == curr) {
                                    stack.add(edge.source to 1 - c)
                                }
                            }
                        }
                    }
                    if (!bipartite) break
                }
                bipartite
            }

            val maxEdges = if (isBipartite) 2 * vertices.size - 4 else 3 * vertices.size - 6
            if (subgraphEdges.size + 1 <= maxEdges || vertices.size < 3) {
                subgraphEdges.add(edge)
            }
        }
    }

    val result = Graph()
    result.edges.addAll(subgraphEdges)
    return result
}