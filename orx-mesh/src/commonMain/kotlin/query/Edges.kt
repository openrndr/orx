package org.openrndr.extra.mesh.query

import org.openrndr.extra.mesh.MeshData

fun MeshData.edges(): Sequence<Pair<Int, Int>> = sequence {
    for (polygon in polygons) {
        for (i in 0 until polygon.positions.size) {
            yield(Pair(polygon.positions[i], polygon.positions[(i + 1) % polygon.positions.size]))
        }
    }
}