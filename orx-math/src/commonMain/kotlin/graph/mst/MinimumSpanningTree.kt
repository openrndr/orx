package org.openrndr.extra.math.graph.mst

import org.openrndr.extra.math.graph.Edge
import org.openrndr.extra.math.graph.Graph

fun Graph.minimumSpanningTreeUndirected(root: Int): Graph {
    // use Prim's algorithm, treat as undirected graph
    // construct the msp such that `root` is the root of the tree

    val mst = Graph()
    val visited = mutableSetOf<Int>()
    val edgesToVisit = mutableListOf<Edge>()

    fun addEdges(node: Int) {
        visited.add(node)
        for (edge in edges) {
            if (edge.source == node && edge.target !in visited) {
                edgesToVisit.add(edge)
            } else if (edge.target == node && edge.source !in visited) {
                edgesToVisit.add(Edge(edge.target, edge.source, edge.weight))
            }
        }
    }

    addEdges(root)

    while (edgesToVisit.isNotEmpty()) {
        // Find the edge with the minimum weight
        var minEdgeIndex = -1
        var minWeight = Double.MAX_VALUE

        for (i in edgesToVisit.indices) {
            val edge = edgesToVisit[i]
            if (edge.target !in visited && edge.weight < minWeight) {
                minWeight = edge.weight
                minEdgeIndex = i
            }
        }

        if (minEdgeIndex == -1) {
            // Check if there are any remaining edges that target an unvisited node
            // This could happen if the graph is disconnected or if all edges in edgesToVisit target visited nodes.
            // We should remove edges that target already visited nodes to eventually empty the list.
            val it = edgesToVisit.iterator()
            while (it.hasNext()) {
                if (it.next().target in visited) {
                    it.remove()
                }
            }
            if (edgesToVisit.isEmpty()) break
            continue
        }

        val edge = edgesToVisit.removeAt(minEdgeIndex)
        if (edge.target !in visited) {
            mst.edges.add(edge)
            addEdges(edge.target)
        }
    }

    return mst
}