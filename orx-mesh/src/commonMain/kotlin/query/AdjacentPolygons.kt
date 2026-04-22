package org.openrndr.extra.mesh.query

import org.openrndr.extra.mesh.IIndexedPolygon
import org.openrndr.extra.mesh.IMeshData
import org.openrndr.extra.mesh.indicesOf

fun IMeshData.polygonsAdjacentToVertex(vertexId: Int): List<IIndexedPolygon> {
    return polygons.filter { polygon ->
        polygon.positions.any { it == vertexId }
    }
}

fun IMeshData.polygonsAdjacentToPolygon(polygonId: Int): List<IIndexedPolygon> {
    val polygon = polygons[polygonId]
    val vertices = polygon.positions.toSet()

    return polygons.filter { otherPolygon ->
        otherPolygon !== polygon && otherPolygon.positions.any { it in vertices }
    }
}

private fun isPolygonAdjacentToEdge(it: IIndexedPolygon, edgeId: Pair<Int, Int>): Boolean {
    val mayHaveEdge = it.positions.any { it == edgeId.first } && it.positions.any { it == edgeId.second }
    return mayHaveEdge && (
            it.positions.indices.any { idx ->
                val idx0 = idx
                val idx1 = (idx + 1) % it.positions.size

                (it.positions[idx0] == edgeId.first && it.positions[idx1] == edgeId.second) ||
                        (it.positions[idx1] == edgeId.first || it.positions[idx0] == edgeId.second)
            }
            )
}

/**
 * Retrieves a list of polygons from the mesh that are adjacent to the specified edge.
 *
 * An edge is defined by a pair of vertex indices, and this method filters the polygons
 * in the mesh to determine which ones share this edge.
 *
 * @param edgeId A pair of integers representing the vertex indices that define the edge.
 * @return A list of [IIndexedPolygon] instances that are adjacent to the specified edge.
 */
fun IMeshData.polygonsAdjacentToEdge(edgeId: Pair<Int, Int>): List<IIndexedPolygon> = polygons.filter {
    isPolygonAdjacentToEdge(it, edgeId)
}

/**
 * Finds the indices of polygons in the mesh that are adjacent to a given edge.
 *
 * This method determines which polygons share the specified edge, defined as a pair
 * of vertex indices, and returns their indices within the mesh's polygon list.
 *
 * @param edgeId A pair of vertex indices representing the edge to search for.
 *               The first element of the pair represents one vertex of the edge,
 *               while the second element represents the other vertex.
 * @return A list of integer indices corresponding to the polygons adjacent to the specified edge.
 *         If no polygons are adjacent to the edge, an empty list is returned.
 */
fun IMeshData.polygonsAdjacentToEdgeIndices(edgeId: Pair<Int, Int>): List<Int> =
    polygons.indicesOf { isPolygonAdjacentToEdge(it, edgeId) }