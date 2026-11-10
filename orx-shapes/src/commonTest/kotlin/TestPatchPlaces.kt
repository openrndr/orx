import org.openrndr.extra.shapes.bezierpatches.BezierPatch3D
import org.openrndr.extra.shapes.bezierpatches.evenGrid
import org.openrndr.extra.shapes.bezierpatches.scatter
import org.openrndr.math.Vector3
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestPatchPlaces {
    /** a patch that is stretched along u: its control points bunch at u = 0 and spread towards u = 1 */
    private val stretched = BezierPatch3D(List(4) { j -> List(4) { i ->
        val x = listOf(0.0, 0.5, 2.0, 12.0)[i]
        Vector3(x, j * 2.0, kotlin.math.sin(x * 0.5 + j) * 1.5)
    } })

    @Test
    fun scatteredPlacesAreApartAndFillThePatch() {
        val places = stretched.scatter(0.6, Random(7))
        val points = places.map { stretched.position(it.x, it.y) }
        assertTrue(places.all { it.x in 0.0..1.0 && it.y in 0.0..1.0 })
        for (i in points.indices) for (j in i + 1 until points.size) {
            assertTrue(points[i].distanceTo(points[j]) >= 0.6 - 1E-12)
        }
        // no hole where another place would fit easily: every point of the surface has a place within twice the distance
        val random = Random(1)
        repeat(400) {
            val q = stretched.position(random.nextDouble(), random.nextDouble())
            assertTrue(points.minOf { it.distanceTo(q) } < 1.2)
        }
        // more places where the patch is stretched, in the half of u near 1, than where it is squeezed
        assertTrue(places.count { it.x > 0.5 } > 2 * places.count { it.x < 0.5 })
        // the same seed, the same places
        assertEquals(places, stretched.scatter(0.6, Random(7)))
    }

    @Test
    fun anEvenGridIsSpacedAlongTheSurface() {
        val grid = stretched.evenGrid(columns = 9, rows = 5)
        assertEquals(5, grid.size)
        assertTrue(grid.all { it.size == 9 && it.first().x == 0.0 && it.last().x == 1.0 })
        assertEquals(0.0, grid.first().first().y)
        assertEquals(1.0, grid.last().first().y)
        for (row in grid) {
            // the arc lengths between neighbours along the row are equal; the chords differ by little more
            val curve = stretched.horizontal(row.first().y)
            val lengths = row.zipWithNext().map { (a, b) -> curve.sub(a.x, b.x).length }
            val mean = lengths.average()
            assertTrue(lengths.all { abs(it - mean) / mean < 0.01 }, "$lengths")
        }
    }
}
