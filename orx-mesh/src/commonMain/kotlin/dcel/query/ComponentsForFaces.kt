package org.openrndr.extra.mesh.dcel.query

import org.openrndr.extra.mesh.dcel.Dcel


/**
 * Finds all connected components of the given faces.
 * A connected component consists of faces that are connected through their edges.
 * Only faces provided in [faceIds] are considered for the components.
 *
 * @param faceIds a list of face IDs to group into connected components.
 * @return a list of components, where each component is a list of unique face IDs.
 */
fun Dcel.componentsForFaces(faceIds: List<Int>):
        List<List<Int>> {
    val remainingFaces = faceIds.toMutableSet()
    val components = mutableListOf<List<Int>>()

    while (remainingFaces.isNotEmpty()) {
        val component = mutableListOf<Int>()
        val startFace = remainingFaces.first()
        val queue = ArrayDeque<Int>()
        queue.add(startFace)
        remainingFaces.remove(startFace)

        while (queue.isNotEmpty()) {
            val faceId = queue.removeFirst()
            component.add(faceId)

            val neighbors = faceAdjacent(faceId)
            for (neighbor in neighbors) {
                if (remainingFaces.contains(neighbor)) {
                    queue.add(neighbor)
                    remainingFaces.remove(neighbor)
                }
            }
        }
        components.add(component)
    }
    return components
}