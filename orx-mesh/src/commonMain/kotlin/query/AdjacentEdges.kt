package org.openrndr.extra.mesh.query

import org.openrndr.extra.mesh.IMeshData

fun IMeshData.edgesAdjacentToVertex(vertexId: Int): List<Pair<Int, Int>> {
    val result = mutableListOf<Pair<Int, Int>>()
    for (polygon in polygons) {
        for (i in 0 until polygon.positions.size) {
            if (polygon.positions[i] == vertexId) {
                result.add(
                    Pair(
                        polygon.positions[i],
                        polygon.positions[(i + 1).mod(polygon.positions.size)]
                    )
                )
                result.add(
                    Pair(
                        polygon.positions[(i - 1).mod(polygon.positions.size)],
                        polygon.positions[i],
                    )
                )
            }
        }
    }
    return result
}

fun IMeshData.edgesAdjacentToPolygon(polygonId: Int): List<Pair<Int, Int>> {
    val result = mutableListOf<Pair<Int, Int>>()
    for (i in 0 until polygons[polygonId].positions.size) {
        result.add(
            Pair(
                polygons[polygonId].positions[i],
                polygons[polygonId].positions[(i + 1).mod(polygons[polygonId].positions.size)]
            )
        )
    }
    return result
}