package org.openrndr.extra.mesh.modify

import org.openrndr.extra.mesh.IndexedPolygon
import kotlin.test.Test
import kotlin.test.assertEquals

class TestModifyEdges {

    @Test
    fun testSplitList() {
        val list = listOf(0, 1, 2, 3)
        // Split at edge (0, 2) - diagonal
        val result = splitList(list, 0, 2)
        assertEquals(listOf(0, 1, 2), result[0])
        assertEquals(listOf(2, 3, 0), result[1])
    }

    @Test
    fun testSplitPolygonAtEdge() {
        val poly = IndexedPolygon(
            positions = listOf(0, 1, 2, 3),
            textureCoords = listOf(10, 11, 12, 13),
            colors = emptyList(),
            normals = emptyList(),
            tangents = emptyList(),
            bitangents = emptyList()
        )

        val split = splitPolygonAtEdge(poly, Pair(0, 2))
        assertEquals(2, split.size)

        assertEquals(listOf(0, 1, 2), split[0].positions)
        assertEquals(listOf(10, 11, 12), split[0].textureCoords)

        assertEquals(listOf(2, 3, 0), split[1].positions)
        assertEquals(listOf(12, 13, 10), split[1].textureCoords)
    }

    @Test
    fun testJoinLists() {
        // poly A: (0, 1, 2), shared edge (0, 1)
        // poly B: (1, 0, 3), shared edge (1, 0)
        val a = listOf(0, 1, 2)
        val b = listOf(1, 0, 3)
        
        val joined = joinLists(a, b, 0, 1, 1, 0)
        // From a, a1=1 to a0=0: a[1] is added, then curr=2, a[2] is added, then curr=0 (stop) -> (1, 2)
        // From b, b1=0 to b0=1: b[0] is added, then curr=2 (b[2]=3), b[2] is added, then curr=1 (stop) -> (0, 3)
        // Result: (1, 2, 0, 3)
        assertEquals(listOf(1, 2, 0, 3), joined)
    }

    @Test
    fun testJoinPolygonsAtEdge() {
        val a = IndexedPolygon(
            positions = listOf(0, 1, 2),
            textureCoords = listOf(10, 11, 12),
            colors = emptyList(),
            normals = emptyList(),
            tangents = emptyList(),
            bitangents = emptyList()
        )
        val b = IndexedPolygon(
            positions = listOf(1, 0, 3),
            textureCoords = listOf(11, 10, 13),
            colors = emptyList(),
            normals = emptyList(),
            tangents = emptyList(),
            bitangents = emptyList()
        )

        val joined = joinPolygonsAtEdge(a, b, Pair(0, 1))
        assertEquals(listOf(1, 2, 0, 3), joined.positions)
        assertEquals(listOf(11, 12, 10, 13), joined.textureCoords)
    }
}
