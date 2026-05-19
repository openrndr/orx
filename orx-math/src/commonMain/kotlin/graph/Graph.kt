package org.openrndr.extra.math.graph


data class Edge(val source: Int, val target: Int, val weight: Double = 0.0)

open class Graph {
    val edges = mutableListOf<Edge>()
}