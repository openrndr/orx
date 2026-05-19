package org.openrndr.extra.math.graph.coloring

import org.openrndr.extra.math.graph.Graph

fun colorGraph(graph: Graph, colorCount: Int): IntArray {
    val maxNode = graph.edges.flatMap { listOf(it.source, it.target) }.maxOrNull() ?: -1
    val nodeCount = maxNode + 1
    val colors = IntArray(nodeCount) { -1 }

    if (nodeCount == 0) return colors

    val adjacency = Array(nodeCount) { mutableListOf<Int>() }
    for (edge in graph.edges) {
        adjacency[edge.source].add(edge.target)
        adjacency[edge.target].add(edge.source)
    }

    for (node in 0 until nodeCount) {
        val usedColors = mutableSetOf<Int>()
        for (neighbor in adjacency[node]) {
            val color = colors[neighbor]
            if (color != -1) {
                usedColors.add(color)
            }
        }

        var assignedColor = -1
        for (c in 0 until colorCount) {
            if (c !in usedColors) {
                assignedColor = c
                break
            }
        }

        if (assignedColor == -1) {
            var c = 0
            while (c in usedColors) {
                c++
            }
            assignedColor = c
        }
        colors[node] = assignedColor
    }
    return colors
}