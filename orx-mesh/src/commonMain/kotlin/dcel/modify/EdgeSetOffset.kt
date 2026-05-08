package org.openrndr.extra.mesh.dcel.modify

import org.openrndr.extra.mesh.dcel.Dcel

fun Dcel.edgeSetOffset(edgeIds: Set<Int>, offset: Double): Set<Int> {
    // assumes that all edgeIds are of boundary edges, edges for which otherEdge == -1
    // offsetting an edge creates a face with 4 edges of which 1 is parallel to the original edge.
    // it is possible that edgeIds contains adjacent edges or even multiple sets of contingent edges.
    // offsetting N adjacent edges creates N adjacent faces.
    // return the ids of the newly created faces

}