import org.openrndr.extra.mesh.IndexedPolygon
import org.openrndr.extra.mesh.VertexData
import org.openrndr.math.Vector3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestIndexedPolygonTriangulation {
    private fun polygon(n: Int) = IndexedPolygon((0 until n).toList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())

    @Test
    fun aConcavePolygonGivesTrianglesOfItsVertices() {
        // an arrow, concave at its notch
        val data = VertexData(listOf(
            Vector3(0.0, 0.0, 0.0), Vector3(4.0, 0.0, 0.0), Vector3(4.0, 3.0, 0.0), Vector3(2.0, 1.0, 0.0), Vector3(0.0, 3.0, 0.0)
        ))
        val triangles = polygon(5).triangulate(data)
        assertEquals(3, triangles.size)
        assertTrue(triangles.all { t -> t.positions.size == 3 && t.positions.all { it in 0 until 5 } })
    }

    @Test
    fun aPolygonThatCrossesItselfDoesNotThrow() {
        // a bow tie, its sides crossing in the middle
        val data = VertexData(listOf(
            Vector3(0.0, 0.0, 0.0), Vector3(2.0, 2.0, 0.0), Vector3(2.0, 0.0, 0.0), Vector3(0.0, 2.0, 0.0)
        ))
        val triangles = polygon(4).triangulate(data)
        // the triangles at the crossing need a vertex the data does not have: only those of its own vertices remain
        assertTrue(triangles.all { t -> t.positions.size == 3 && t.positions.all { it in 0 until 4 } })
    }
}
