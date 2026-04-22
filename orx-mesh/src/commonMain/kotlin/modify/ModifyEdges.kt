package org.openrndr.extra.mesh.modify

import org.openrndr.extra.mesh.IIndexedPolygon
import org.openrndr.extra.mesh.IndexedPolygon
import org.openrndr.extra.mesh.MutableIndexedPolygon
import org.openrndr.extra.mesh.MutableMeshData
import org.openrndr.extra.mesh.query.polygonsAdjacentToEdgeIndices

fun <T> joinLists(a: List<T>, b: List<T>, a0: Int, a1: Int, b0: Int, b1: Int): List<T> {
    if (a.isEmpty() || b.isEmpty()) return emptyList()
    val result = mutableListOf<T>()

    val aSize = a.size
    val bSize = b.size

    // Add all vertices from a starting from a1 until a0
    var curr = a1
    while (curr != a0) {
        result.add(a[curr])
        curr = (curr + 1) % aSize
    }

    // Add all vertices from b starting from b0 until b1
    curr = b0
    while (curr != b1) {
        result.add(b[curr])
        curr = (curr + 1) % bSize
    }

    return result
}


fun <T> splitList(list: List<T>, i0: Int, i1: Int): List<List<T>> {
    if (list.isEmpty()) return listOf(emptyList(), emptyList())
    val size = list.size

    val list0 = mutableListOf<T>()
    var curr = i0
    while (curr != i1) {
        list0.add(list[curr])
        curr = (curr + 1) % size
    }
    list0.add(list[i1])

    val list1 = mutableListOf<T>()
    curr = i1
    while (curr != i0) {
        list1.add(list[curr])
        curr = (curr + 1) % size
    }
    list1.add(list[i0])

    return listOf(list0, list1)
}

fun splitPolygonAtEdge(polygon: IIndexedPolygon, edgeId: Pair<Int, Int>): List<IIndexedPolygon> {
    val a0 = polygon.positions.indexOfFirst { it == edgeId.first }
    val a1 = polygon.positions.indexOfFirst { it == edgeId.second }
    require(a0 != -1 && a1 != -1) { "Edge not found in polygon" }

    val newPositions = splitList(polygon.positions, a0, a1)
    val newTextureCoords = splitList(polygon.textureCoords, a0, a1)
    val newColors = splitList(polygon.colors, a0, a1)
    val newNormals = splitList(polygon.normals, a0, a1)
    val newTangents = splitList(polygon.tangents, a0, a1)
    val newBitangents = splitList(polygon.bitangents, a0, a1)

    val mutable = polygon is MutableIndexedPolygon
    return (0 until 2).map {
        if (!mutable) IndexedPolygon(
            newPositions[it],
            newTextureCoords[it],
            newColors[it],
            newNormals[it],
            newTangents[it],
            newBitangents[it]
        ) else {
            MutableIndexedPolygon(
                newPositions[it].toMutableList(),
                newTextureCoords[it].toMutableList(),
                newColors[it].toMutableList(),
                newNormals[it].toMutableList(),
                newTangents[it].toMutableList(),
                newBitangents[it].toMutableList(),
            )
        }
    }
}

fun joinPolygonsAtEdge(a: IIndexedPolygon, b: IIndexedPolygon, edgeId: Pair<Int, Int>): IIndexedPolygon {
    val a0 = a.positions.indexOfFirst { it == edgeId.first }
    val a1 = a.positions.indexOfFirst { it == edgeId.second }
    val b0 = b.positions.indexOfFirst { it == edgeId.first }
    val b1 = b.positions.indexOfFirst { it == edgeId.second }

    require(a0 != -1 && a1 != -1) { "Edge not found in polygon a" }
    require(b0 != -1 && b1 != -1) { "Edge not found in polygon b" }

    // Build merged position list: a vertices (excluding edge) + b vertices (excluding edge)
    val newPositions = joinLists(a.positions, b.positions, a0, a1, b0, b1)
    val newNormals = joinLists(a.normals, b.normals, a0, a1, b0, b1)
    val newTangents = joinLists(a.tangents, b.tangents, a0, a1, b0, b1)
    val newBitangents = joinLists(a.bitangents, b.bitangents, a0, a1, b0, b1)
    val newTextureCoords = joinLists(a.textureCoords, b.textureCoords, a0, a1, b0, b1)
    val newColors = joinLists(a.colors, b.colors, a0, a1, b0, b1)


    val mutable = a is MutableIndexedPolygon || b is MutableIndexedPolygon

    val mergedPolygon = if (!mutable) IndexedPolygon(
        newPositions,
        newTextureCoords,
        newColors,
        newNormals,
        newTangents,
        newBitangents
    ) else {
        MutableIndexedPolygon(
            newPositions.toMutableList(),
            newTextureCoords.toMutableList(),
            newNormals.toMutableList(),
            newColors.toMutableList(),
            newTangents.toMutableList(),
            newBitangents.toMutableList()
        )
    }
    return mergedPolygon
}

fun MutableMeshData.removeEdge(edgeId: Pair<Int, Int>) {
    val adjacentPolygons = polygonsAdjacentToEdgeIndices(edgeId)

    when (adjacentPolygons.size) {
        0 -> {}
        1 -> {
            polygons.removeAt(adjacentPolygons[0])
        }

        2 -> {
            val a = polygons[adjacentPolygons[0]]
            val b = polygons[adjacentPolygons[1]]
            val c = joinPolygonsAtEdge(a, b, edgeId)
            polygons[adjacentPolygons[0]] = c as IndexedPolygon
            polygons.removeAt(adjacentPolygons[1])
        }

        else -> error("mesh structure invalid")
    }
}

