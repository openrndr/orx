package org.openrndr.extra.mesh.dcel.query

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.shapes.kernel.findKernel
import org.openrndr.math.Vector3

fun Dcel.isFaceStar(faceId: Int): Boolean = findFaceStarKernel(faceId).isNotEmpty()

fun Dcel.findFaceStarKernel(faceId: Int): List<Vector3> {
    val edges = edgeObjectsForFace(faceId)
    val vPositions = edges.map { vertices[it.vertex].position }
    return findKernel(vPositions)
}