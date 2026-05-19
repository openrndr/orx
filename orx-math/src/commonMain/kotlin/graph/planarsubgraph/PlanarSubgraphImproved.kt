package org.openrndr.extra.math.graph.planarsubgraph

import org.openrndr.extra.math.graph.Edge
import org.openrndr.extra.math.graph.Graph
import org.openrndr.extra.math.graph.mst.minimumSpanningTreeUndirected
import org.openrndr.extra.math.graph.planarembedding.PlanarEmbedding
import org.openrndr.extra.math.graph.planarembedding.isPlanar

fun Graph.findPlanarSubgraphUndirectedImproved(root: Int): Graph {
    val mst = minimumSpanningTreeUndirected(root)
    var (_, embedding) = mst.isPlanar()

    val allEdges = edges.map { if (it.source < it.target) it else Edge(it.target, it.source, it.weight) }
        .distinctBy { it.source to it.target }
        .sortedByDescending { it.weight }

    val subgraphEdges = mst.edges.toMutableSet()

    for (edge in allEdges) {
        // If edge not already in MST
        if (!subgraphEdges.any { (it.source == edge.source && it.target == edge.target) || (it.source == edge.target && it.target == edge.source) }) {
            if (embedding.sharesAFace(edge.source, edge.target)) {
                println("adding edge: $edge")
                subgraphEdges.add(edge)
                embedding.addEdge(edge.source, edge.target)
            } else {
                println("skipping edge: $edge")
            }
        }
    }

    val result = Graph()
    result.edges.addAll(subgraphEdges)
    return result
}