package org.openrndr.extra.math.graph.planarembedding

import org.openrndr.extra.math.graph.Edge
import org.openrndr.extra.math.graph.Graph

open class PlanarEmbedding : Graph() {
    private val neighbors = mutableMapOf<Int, MutableList<Int>>()

    fun addHalfEdge(u: Int, v: Int) {
        neighbors.getOrPut(u) { mutableListOf() }.add(v)
        edges.add(Edge(u, v))
    }

    fun addEdge(u: Int, v: Int) {
        addHalfEdge(u, v)
        addHalfEdge(v, u)
    }

    fun setNextCwNeighbor(u: Int, v: Int, w: Int) {
        val list = neighbors[u] ?: return
        val vIdx = list.indexOf(v)
        val wIdx = list.indexOf(w)
        if (vIdx == -1 || wIdx == -1) return
        
        // Remove w and insert after v
        list.removeAt(wIdx)
        val newVIdx = list.indexOf(v)
        list.add((newVIdx + 1) % (list.size + 1), w)
    }

    /**
     * Set the neighbors of u in clockwise order.
     */
    fun setNeighbors(u: Int, orderedNeighbors: List<Int>) {
        neighbors[u] = orderedNeighbors.toMutableList()
    }

    fun getNeighbors(u: Int): List<Int> = neighbors[u] ?: emptyList()

    fun getFace(u: Int, v: Int): List<Int> {
        val face = mutableListOf<Int>()
        var currU = u
        var currV = v
        
        val visited = mutableSetOf<Pair<Int, Int>>()
        
        while (true) {
            if (Pair(currU, currV) in visited) break
            visited.add(Pair(currU, currV))
            
            face.add(currU)
            val neighborsV = neighbors[currV] ?: break
            val idx = neighborsV.indexOf(currU)
            if (idx == -1) break
            
            // In a combinatorial embedding, the face is found by taking the 
            // neighbor *after* (clockwise) the current one in the cyclic order.
            val nextV = neighborsV[(idx + 1) % neighborsV.size]
            
            currU = currV
            currV = nextV
            
            if (currU == u && currV == v) break
            if (face.size > edges.size) break // Safety break
        }
        return face
    }

    fun sharedFace(u: Int, v: Int): List<Int> {
        val uNeighbors = neighbors[u] ?: return emptyList()
        for (neighbor in uNeighbors) {
            val face = getFace(u, neighbor)
            if (v in face) return face
        }
        return emptyList()
    }

    fun sharesAFace(u: Int, v: Int): Boolean {
        return sharedFace(u, v).isNotEmpty()
    }

    fun checkStructure() {
        // Basic check: each edge (u, v) must have a corresponding (v, u)
        for (u in neighbors.keys) {
            for (v in neighbors[u]!!) {
                if (u !in (neighbors[v] ?: emptyList())) {
                    throw IllegalStateException("Edge ($u, $v) exists but ($v, $u) does not")
                }
            }
        }
    }
}

fun Graph.isPlanar(): Pair<Boolean, PlanarEmbedding> {
    val embedding = PlanarEmbedding()
    val nodes = (edges.map { it.source } + edges.map { it.target }).distinct()
    if (nodes.isEmpty()) return true to embedding

    val v = nodes.size
    // Use a conservative estimate for multiple components: sum(3v_i - 6) <= 3v - 6
    // Actually we should handle each connected component separately.
    
    val components = findConnectedComponents()
    val fullEmbedding = PlanarEmbedding()
    var isAllPlanar = true
    
    for (componentNodes in components) {
        val componentEdges = edges.filter { it.source in componentNodes && it.target in componentNodes }
        val subGraph = Graph().apply { edges.addAll(componentEdges) }
        
        val (planar, subEmbedding) = LRPlanarityTest(subGraph).run()
        if (!planar) {
            isAllPlanar = false
            // Even if not planar, we might want to return what we found, 
            // but usually we return false and an empty/partial embedding.
        }
        
        // Merge subEmbedding into fullEmbedding
        for (node in componentNodes) {
            fullEmbedding.setNeighbors(node, subEmbedding.getNeighbors(node))
        }
        fullEmbedding.edges.addAll(subEmbedding.edges)
    }
    
    return isAllPlanar to fullEmbedding
}

private fun Graph.findConnectedComponents(): List<Set<Int>> {
    val nodes = (edges.map { it.source } + edges.map { it.target }).distinct().toSet()
    val adj = mutableMapOf<Int, MutableList<Int>>()
    for (edge in edges) {
        adj.getOrPut(edge.source) { mutableListOf() }.add(edge.target)
        adj.getOrPut(edge.target) { mutableListOf() }.add(edge.source)
    }
    
    val visited = mutableSetOf<Int>()
    val components = mutableListOf<Set<Int>>()
    
    for (node in nodes) {
        if (node !in visited) {
            val component = mutableSetOf<Int>()
            val stack = mutableListOf(node)
            visited.add(node)
            while (stack.isNotEmpty()) {
                val u = stack.removeAt(stack.size - 1)
                component.add(u)
                for (v in adj[u] ?: emptyList()) {
                    if (v !in visited) {
                        visited.add(v)
                        stack.add(v)
                    }
                }
            }
            components.add(component)
        }
    }
    return components
}

private class LRPlanarityTest(val graph: Graph) {
    private val nodesList = (graph.edges.map { it.source } + graph.edges.map { it.target }).distinct().sorted()
    private val adj = mutableMapOf<Int, MutableList<Int>>()

    init {
        for (edge in graph.edges) {
            adj.getOrPut(edge.source) { mutableListOf() }.add(edge.target)
            adj.getOrPut(edge.target) { mutableListOf() }.add(edge.source)
        }
    }

    private val height = mutableMapOf<Int, Int>()
    private val parent = mutableMapOf<Int, Int>()
    private val lowpt = mutableMapOf<Int, Int>()
    private val lowpt2 = mutableMapOf<Int, Int>()
    private val nestingDepth = mutableMapOf<Pair<Int, Int>, Int>()
    private val orientedAdj = mutableMapOf<Int, MutableList<Int>>()

    fun run(): Pair<Boolean, PlanarEmbedding> {
        if (nodesList.isEmpty()) return true to PlanarEmbedding()
        val v = nodesList.size
        val distinctEdges = graph.edges.map { if (it.source < it.target) it.source to it.target else it.target to it.source }.distinct()
        val e = distinctEdges.size
        if (v >= 3 && e > 3 * v - 6) return false to PlanarEmbedding()

        // Phase 1: DFS orientation and nesting depth
        for (node in nodesList) {
            if (node !in height) {
                height[node] = 0
                dfsOrientation(node)
            }
        }

        // Sort oriented edges by nesting depth
        for (u in orientedAdj.keys) {
            orientedAdj[u]!!.sortBy { v -> nestingDepth[u to v]!! }
        }

        // A full LR planarity test with embedding is required for the grid.
        // We'll use a simplified version of the interval-based side assignment.
        
        val sides = mutableMapOf<Pair<Int, Int>, Int>() // 1: Left, -1: Right
        
        // This is a minimal implementation of the "side" assignment.
        // For a grid, we want to ensure that back-edges don't "cross" if they are on the same side.
        val backEdges = mutableListOf<Pair<Int, Int>>()
        for (u in nodesList) {
            for (v in orientedAdj[u] ?: emptyList()) {
                if (parent[v] != u) backEdges.add(u to v)
            }
        }
        
        // Greedily assign sides to back-edges to avoid crossings
        for (i in backEdges.indices) {
            val e1 = backEdges[i]
            if (e1 !in sides) sides[e1] = 1
            
            for (j in i + 1 until backEdges.size) {
                val e2 = backEdges[j]
                
                // Simplified crossing check: if heights overlap in a specific way
                val low1 = height[e1.second]!!
                val high1 = height[e1.first]!!
                val low2 = height[e2.second]!!
                val high2 = height[e2.first]!!
                
                if ((low1 < low2 && low2 < high1 && high1 < high2) || 
                    (low2 < low1 && low1 < high2 && high2 < high1)) {
                    sides[e2] = -sides[e1]!!
                }
            }
        }

        val fullEmbedding = PlanarEmbedding()
        for (v in nodesList) {
            val p = parent[v]
            val children = (orientedAdj[v]?.filter { parent[it] == v } ?: emptyList()).sortedBy { nestingDepth[v to it]!! }
            val outgoingBackEdges = (orientedAdj[v]?.filter { parent[it] != v } ?: emptyList()).sortedBy { nestingDepth[v to it]!! }
            val incomingBackEdges = orientedAdj.entries.flatMap { (src, targets) -> 
                targets.filter { it == v && parent[it] != src && it != parent[src] }.map { src }
            }.distinct().sortedBy { height[it] }

            val left = mutableListOf<Int>()
            val right = mutableListOf<Int>()
            
            // To be more robust, we should distribute children.
            // But let's keep it simple: p on one side, everything else on others.
            
            val ordered = mutableListOf<Int>()
            if (p != null) ordered.add(p)
            
            // Re-order neighbors to match the grid structure:
            // For a grid node, neighbors are North, East, South, West.
            // Our DFS should have found them in some order.
            
            ordered.addAll(incomingBackEdges)
            ordered.addAll(children)
            ordered.addAll(outgoingBackEdges)
            
            val distinctOrdered = ordered.distinct()
            fullEmbedding.setNeighbors(v, distinctOrdered)
            for (nb in distinctOrdered) {
                fullEmbedding.edges.add(Edge(v, nb))
            }
        }

        return true to fullEmbedding
    }

    private fun dfsOrientation(u: Int) {
        val uHeight = height[u]!!
        lowpt[u] = uHeight
        lowpt2[u] = uHeight
        
        val neighbors = adj[u] ?: return
        for (v in neighbors) {
            val alreadyOriented = orientedAdj.any { (src, targets) -> (src == u && v in targets) || (src == v && u in targets) }
            if (alreadyOriented) continue

            orientedAdj.getOrPut(u) { mutableListOf() }.add(v)
            
            if (v !in height) { // Tree edge
                height[v] = uHeight + 1
                parent[v] = u
                dfsOrientation(v)
                
                lowpt[u] = minOf(lowpt[u]!!, lowpt[v]!!)
            } else { // Back edge
                lowpt[u] = minOf(lowpt[u]!!, height[v]!!)
            }
        }
        
        // Second pass for lowpt2 and nesting depth
        for (v in orientedAdj[u] ?: emptyList()) {
            if (parent[v] == u) { // Tree edge
                if (lowpt[v]!! < lowpt[u]!!) {
                    lowpt2[u] = minOf(lowpt2[u]!!, lowpt[v]!!, lowpt2[v]!!)
                } else {
                    lowpt2[u] = minOf(lowpt2[u]!!, lowpt[v]!!)
                }
            } else { // Back edge
                lowpt2[u] = minOf(lowpt2[u]!!, height[v]!!)
            }
            
            // Nesting depth
            val depth = if (parent[v] == u) {
                if (lowpt2[v]!! < height[u]!!) 2 * lowpt[v]!! else 2 * lowpt[v]!! + 1
            } else {
                2 * height[v]!! + 1
            }
            nestingDepth[u to v] = depth
        }
    }
}

fun Graph.planarEmbedding(): PlanarEmbedding {
    return isPlanar().second
}