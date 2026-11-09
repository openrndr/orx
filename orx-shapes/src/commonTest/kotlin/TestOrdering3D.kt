import org.openrndr.extra.shapes.ordering.hilbertOrder
import org.openrndr.extra.shapes.ordering.mortonOrder
import org.openrndr.math.Vector3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestOrdering3D {
    private val corners = (0 until 8).map { Vector3((it and 1).toDouble(), ((it shr 1) and 1).toDouble(), ((it shr 2) and 1).toDouble()) }.shuffled(kotlin.random.Random(1))

    /** true when every point is a neighbour of the one before it on the cube: they differ in one coordinate */
    private fun stepsToNeighbours(order: List<Vector3>) = order.zipWithNext().all { (a, b) -> (a - b).length in 0.999..1.001 }

    @Test
    fun hilbertOrderVisitsTheCornersOfACubeAsNeighbours() {
        for (bits in listOf(5, 10)) {
            val order = corners.hilbertOrder(bits = bits)
            assertEquals(corners.toSet(), order.toSet())
            assertTrue(stepsToNeighbours(order), "$bits bits: $order")
        }
        assertEquals(corners.hilbertOrder(bits = 5), corners.hilbertOrder(bits = 10))
        assertEquals(corners.hilbertOrder(bits = 10), corners.hilbertOrder())
    }

    @Test
    fun hilbertOrderVisitsAGridAsNeighboursAtFiveBits() {
        // the points inside the grid need all of the bits, not only those of the corners
        val grid = (0 until 64).map { Vector3((it % 4).toDouble(), ((it / 4) % 4).toDouble(), (it / 16).toDouble()) }.shuffled(kotlin.random.Random(2))
        for (bits in listOf(5, 10)) {
            val order = grid.hilbertOrder(bits = bits)
            assertTrue(stepsToNeighbours(order), "$bits bits")
        }
    }

    @Test
    fun mortonOrderIsTheSameAtFiveAndTenBits() {
        assertEquals(corners.mortonOrder(bits = 10), corners.mortonOrder(bits = 5))
        // the z order of the corners: x changes fastest, then y, then z
        assertEquals((0 until 8).map { Vector3((it and 1).toDouble(), ((it shr 1) and 1).toDouble(), ((it shr 2) and 1).toDouble()) }.sortedBy { it.z * 4 + it.y * 2 + it.x },
            corners.mortonOrder(bits = 5).sortedBy { it.z * 4 + it.y * 2 + it.x })
    }
}
