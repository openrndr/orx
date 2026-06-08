package org.openrndr.extra.mesh.dcel.modify

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.query.edgePlane

fun Dcel.faceSetSplitByEdge(faceIds: Set<Int>, edgeId: Int,
                            splitEpsilon: Double = 1E-6,
                            weldEpsilon: Double = 1E-3
                            ): Set<Int> {

    val plane = edgePlane(edgeId)
    return faceSetSplit(faceIds, plane, splitEpsilon, weldEpsilon)
}

