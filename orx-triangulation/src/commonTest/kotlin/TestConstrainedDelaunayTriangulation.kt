import org.openrndr.extra.triangulation.ConstrainedDelaunayTriangulation
import org.openrndr.extra.triangulation.constrainedDelaunayTriangulation
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contains
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TestConstrainedDelaunayTriangulation {

    private fun area(triangulation: ConstrainedDelaunayTriangulation): Double =
        triangulation.triangles().sumOf { it.area }

    private fun canonicalEdges(triangulation: ConstrainedDelaunayTriangulation): Set<Pair<Int, Int>> {
        val edges = mutableSetOf<Pair<Int, Int>>()
        for (t in triangulation.triangleIndices()) {
            for (i in 0 until 3) {
                val a = t[i]
                val b = t[(i + 1) % 3]
                edges.add(Pair(minOf(a, b), maxOf(a, b)))
            }
        }
        return edges
    }

    private fun contourEdgesOf(triangulation: ConstrainedDelaunayTriangulation, contour: ShapeContour): Set<Pair<Int, Int>> {
        val n = contour.segments.size
        val startIndex = triangulation.points.indexOf(contour.segments[0].start)
        val edges = mutableSetOf<Pair<Int, Int>>()
        for (i in 0 until n) {
            val a = startIndex + i
            val b = startIndex + (i + 1) % n
            edges.add(Pair(minOf(a, b), maxOf(a, b)))
        }
        return edges
    }

    @Test
    fun testSquare() {
        val shape = Rectangle(0.0, 0.0, 100.0, 100.0).shape
        val cdt = shape.constrainedDelaunayTriangulation()
        assertEquals(4, cdt.points.size)
        assertEquals(2, cdt.triangleIndices().size)
        assertEquals(10000.0, area(cdt), 1E-6)
    }

    @Test
    fun testSquareWithHole() {
        val outer = Rectangle(0.0, 0.0, 100.0, 100.0).contour
        val hole = Rectangle(25.0, 25.0, 50.0, 50.0).contour.reversed
        val shape = Shape(listOf(outer, hole))
        val cdt = shape.constrainedDelaunayTriangulation()

        assertEquals(8, cdt.points.size)
        assertEquals(10000.0 - 2500.0, area(cdt), 1E-6)

        // every contour edge must survive as an edge of the triangulation
        val edges = canonicalEdges(cdt)
        for (e in contourEdgesOf(cdt, outer)) assertTrue(e in edges)
        for (e in contourEdgesOf(cdt, hole)) assertTrue(e in edges)

        // no triangle centroid should fall inside the hole
        val holeRect = Rectangle(25.0, 25.0, 50.0, 50.0)
        for (t in cdt.triangles()) {
            val centroid = (t.x1 + t.x2 + t.x3) / 3.0
            assertTrue(
                !(centroid.x > holeRect.x && centroid.x < holeRect.x + holeRect.width &&
                        centroid.y > holeRect.y && centroid.y < holeRect.y + holeRect.height)
            )
        }
    }

    @Test
    fun testLShape() {
        // a non-convex L-shaped polygon, which forces actual constrained edge insertion
        val points = listOf(
            Vector2(0.0, 0.0),
            Vector2(100.0, 0.0),
            Vector2(100.0, 50.0),
            Vector2(50.0, 50.0),
            Vector2(50.0, 100.0),
            Vector2(0.0, 100.0)
        )
        val contour = ShapeContour.fromPoints(points, closed = true)
        val shape = Shape(listOf(contour))
        val cdt = shape.constrainedDelaunayTriangulation()

        assertEquals(6, cdt.points.size)
        assertEquals(4, cdt.triangleIndices().size)
        // area of the L shape: 100x100 square minus the missing 50x50 quadrant
        assertEquals(10000.0 - 2500.0, area(cdt), 1E-6)

        val edges = canonicalEdges(cdt)
        for (e in contourEdgesOf(cdt, contour)) assertTrue(e in edges)
    }

    @Test
    fun testRejectsNonLinearShape() {
        val shape = org.openrndr.shape.Circle(50.0, 50.0, 25.0).shape
        assertFailsWith<IllegalArgumentException> {
            shape.constrainedDelaunayTriangulation()
        }
    }

    @Test
    fun testInteriorPoints() {
        val shape = Rectangle(0.0, 0.0, 100.0, 100.0).shape
        val interior = listOf(Vector2(50.0, 50.0))
        val cdt = shape.constrainedDelaunayTriangulation(interior)

        assertEquals(5, cdt.points.size)
        // triangle count for a simple polygon triangulation: 2*interior + boundary - 2
        assertEquals(4, cdt.triangleIndices().size)
        assertEquals(10000.0, area(cdt), 1E-6)

        // boundary edges must still all be present
        val edges = canonicalEdges(cdt)
        for (e in contourEdgesOf(cdt, shape.contours[0])) assertTrue(e in edges)
    }

    @Test
    fun testRejectsInteriorPointOutsideShape() {
        val shape = Rectangle(0.0, 0.0, 100.0, 100.0).shape
        assertFailsWith<IllegalArgumentException> {
            shape.constrainedDelaunayTriangulation(listOf(Vector2(500.0, 500.0)))
        }
    }

    /** Shoelace formula, used as an independent cross-check of the triangulated area. */
    private fun polygonArea(points: List<Vector2>): Double {
        var sum = 0.0
        val n = points.size
        for (i in 0 until n) {
            val a = points[i]
            val b = points[(i + 1) % n]
            sum += a.x * b.y - b.x * a.y
        }
        return kotlin.math.abs(sum) / 2.0
    }

    private fun starPoints(center: Vector2, outerRadius: Double, innerRadius: Double, points: Int): List<Vector2> {
        val result = mutableListOf<Vector2>()
        for (i in 0 until points * 2) {
            val r = if (i % 2 == 0) outerRadius else innerRadius
            val angle = kotlin.math.PI * i / points
            result.add(center + Vector2(r * kotlin.math.cos(angle), r * kotlin.math.sin(angle)))
        }
        return result
    }

    @Test
    fun testStarShape() {
        // a sharply concave 5-pointed star: every "inner" vertex requires edge() to
        // swap out arcs crossing into neighboring points of the convex hull.
        val starPts = starPoints(Vector2(0.0, 0.0), 100.0, 40.0, 5)
        val contour = ShapeContour.fromPoints(starPts, closed = true)
        val shape = Shape(listOf(contour))
        val cdt = shape.constrainedDelaunayTriangulation()

        assertEquals(starPts.size, cdt.points.size)
        assertEquals(starPts.size - 2, cdt.triangleIndices().size)
        assertEquals(polygonArea(starPts), area(cdt), 1E-6)

        val edges = canonicalEdges(cdt)
        for (e in contourEdgesOf(cdt, contour)) assertTrue(e in edges)
    }

    @Test
    fun testTwoHoles() {
        val outer = Rectangle(0.0, 0.0, 200.0, 100.0).contour
        val hole1 = Rectangle(20.0, 20.0, 40.0, 40.0).contour.reversed
        val hole2 = Rectangle(120.0, 30.0, 50.0, 50.0).contour.reversed
        val shape = Shape(listOf(outer, hole1, hole2))
        val cdt = shape.constrainedDelaunayTriangulation()

        val expectedArea = 200.0 * 100.0 - 40.0 * 40.0 - 50.0 * 50.0
        assertEquals(expectedArea, area(cdt), 1E-6)

        val edges = canonicalEdges(cdt)
        for (e in contourEdgesOf(cdt, outer)) assertTrue(e in edges)
        for (e in contourEdgesOf(cdt, hole1)) assertTrue(e in edges)
        for (e in contourEdgesOf(cdt, hole2)) assertTrue(e in edges)

        val holeRects = listOf(Rectangle(20.0, 20.0, 40.0, 40.0), Rectangle(120.0, 30.0, 50.0, 50.0))
        for (t in cdt.triangles()) {
            val centroid = (t.x1 + t.x2 + t.x3) / 3.0
            for (r in holeRects) {
                assertTrue(!(centroid.x > r.x && centroid.x < r.x + r.width && centroid.y > r.y && centroid.y < r.y + r.height))
            }
        }
    }

    @Test
    fun testVoronoiDiagramRespectsHole() {
        val outer = Rectangle(0.0, 0.0, 100.0, 100.0).contour
        val hole = Rectangle(25.0, 25.0, 50.0, 50.0).contour.reversed
        val shape = Shape(listOf(outer, hole))
        val cdt = shape.constrainedDelaunayTriangulation(listOf(Vector2(10.0, 90.0), Vector2(90.0, 10.0)))

        val voronoi = cdt.voronoiDiagram()
        val cells = voronoi.cellPolygons()
        assertEquals(cdt.points.size, cells.size)

        // cells must tile the shape exactly: total area matches, and no cell leaks into the hole
        val totalArea = cells.sumOf { kotlin.math.abs(Shape(listOf(it)).area) }
        assertEquals(10000.0 - 2500.0, totalArea, 1E-6)

        // a vertex exactly on the hole's boundary is fine (that's where a clipped cell
        // touches the hole); only strict interior points indicate a leak.
        val eps = 1E-9
        fun strictlyInsideHole(p: Vector2) = p.x > 25.0 + eps && p.x < 75.0 - eps && p.y > 25.0 + eps && p.y < 75.0 - eps
        for (cell in cells) {
            for (segment in cell.segments) {
                assertTrue(!strictlyInsideHole(segment.start))
            }
        }
    }
}
