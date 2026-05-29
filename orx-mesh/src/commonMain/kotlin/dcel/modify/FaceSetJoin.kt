package org.openrndr.extra.mesh.dcel.modify

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.query.bordersForEdge
import org.openrndr.extra.mesh.dcel.query.componentsForFaces
import org.openrndr.extra.mesh.dcel.query.edgeBetweenFaces

fun Dcel.faceSetJoin(faceIds: Set<Int>): Set<Int> {
    val components = componentsForFaces(faceIds.toList())

    val resultingFaces: MutableSet<Int> = mutableSetOf()
    for (component in components) {
        if (component.size < 2) continue

        var joinedFaceId = component.first()
        for (i in 1 until component.size) {
            val edgeId = edgeBetweenFaces(joinedFaceId, component[i])
            val borders = bordersForEdge(edgeId)
            joinedFaceId = bordersRemove(borders)
        }
        resultingFaces.add(joinedFaceId)
    }
    return resultingFaces
}