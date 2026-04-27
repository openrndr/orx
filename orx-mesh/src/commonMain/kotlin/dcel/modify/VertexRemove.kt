package org.openrndr.extra.mesh.dcel.modify

import org.openrndr.extra.mesh.dcel.Dcel

fun Dcel.vertexRemove(vertexId: Int) {
    // removes a vertex and all edges incident to it

    // possible cases
    // 1. vertex has no incident edges, vertex was dead already -> do nothing
    // 2. vertex part of a single face -> remove vertex adjust face edge loop
    // 3. vertex

}