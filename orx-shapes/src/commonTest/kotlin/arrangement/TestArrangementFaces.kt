package arrangement

import org.openrndr.extra.shapes.arrangement.Arrangement
import org.openrndr.extra.shapes.arrangement.BoundedFace
import org.openrndr.extra.shapes.hobbycurve.hobbyCurve
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeProvider
import org.openrndr.shape.contains
import org.openrndr.shape.triangulate
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestArrangementFaces {
    private fun Random.point(size: Double) = Vector2(nextDouble() * size, nextDouble() * size)

    /** the shapes that contain a point well inside [face]: the centroid of its largest triangle */
    private fun containing(face: BoundedFace, shapes: List<ShapeProvider>): Set<ShapeProvider>? {
        val triangles = triangulate(face.shape, 0.01).chunked(3)
        val area = { t: List<Vector2> -> abs((t[1] - t[0]).x * (t[2] - t[0]).y - (t[1] - t[0]).y * (t[2] - t[0]).x) / 2 }
        val largest = triangles.maxByOrNull(area) ?: return null
        // too thin to tell from a point
        if (area(largest) < 1.0) return null
        val c = (largest[0] + largest[1] + largest[2]) / 3.0
        return shapes.filter { it.shape.closedContours.isNotEmpty() && c in it.shape }.toSet()
    }

    @Test
    fun strongly_curved_faces_are_bounded() {
        // a long curve through random points that crosses itself: its faces have strongly curved edges, on which
        // the control polygons of the edges can wind the other way than the edges
        val random = Random(3)
        val curve = hobbyCurve(List(150) { random.point(1000.0) }, closed = true)
        val arrangement = Arrangement(curve, curve)
        // one connected component: one unbounded face, and V - E + F = 2
        assertEquals(1, arrangement.unboundedFaces.size)
        val vertices = arrangement.halfEdges.map { it.source }.toSet().size
        assertEquals(2, vertices - arrangement.edges.size + arrangement.faces.size)
    }

    @Test
    fun faces_are_inside_the_shapes_that_contain_them() {
        val random = Random(9)
        val annuli = List(12) {
            val c = random.point(600.0)
            Shape(listOf(Circle(c, 80.0).contour.clockwise, Circle(c, 40.0).contour.counterClockwise))
        }
        val circles = List(30) { Circle(random.point(600.0), 30.0 + random.nextDouble() * 60.0).contour }
        val overlapping = List(4) {
            val c = random.point(300.0)
            Shape(listOf(Circle(c, 80.0).contour, Circle(c + Vector2(50.0, 0.0), 80.0).contour))
        }
        // curves that cross themselves, passed twice so that their own crossings are in the arrangement
        val selfCrossing = List(3) { hobbyCurve(List(10) { random.point(400.0) }, closed = true) }.flatMap { listOf(it, it) }
        for (shapes in listOf<List<ShapeProvider>>(annuli, circles, overlapping, selfCrossing, annuli + circles)) {
            val arrangement = Arrangement(shapes)
            var checked = 0
            for (face in arrangement.boundedFaces) {
                face as BoundedFace
                val expected = containing(face, shapes.distinct()) ?: continue
                assertEquals(expected, face.origins.toSet())
                checked++
            }
            assertTrue(checked > 0)
            // the faces of each shape, by shape
            for (shape in shapes) {
                val faces = arrangement.hFacesMap[shape].orEmpty()
                assertTrue(faces.all { shape in it.origins })
            }
        }
    }

    /** checks that every bounded face is in the shapes that contain it, and returns the faces by their sets of origins */
    private fun checkedFaces(arrangement: Arrangement, shapes: List<ShapeProvider>): List<Set<ShapeProvider>> =
        arrangement.boundedFaces.map { face ->
            face as BoundedFace
            containing(face, shapes)?.let { assertEquals(it, face.origins.toSet(), "the face at ${face.contour.bounds}") }
            face.origins.toSet()
        }

    /** true when no two edges of [arrangement] lie on each other */
    private fun noDoubleEdges(arrangement: Arrangement): Boolean = arrangement.edges.indices.none { i ->
        (0 until i).any { j ->
            val a = arrangement.edges[i]
            val b = arrangement.edges[j]
            ((a.source === b.source && a.target === b.target) || (a.source === b.target && a.target === b.source)) &&
                    listOf(0.25, 0.5, 0.75).all { t -> b.contour.nearest(a.contour.position(t)).position.distanceTo(a.contour.position(t)) < 1E-3 }
        }
    }

    @Test
    fun shapes_that_share_parts_of_edges() {
        // overlapping on x 0..50, the tops and bottoms on the same lines
        val a = Rectangle(-50.0, -50.0, 100.0, 100.0).shape
        val b = Rectangle(0.0, -50.0, 100.0, 100.0).shape
        val overlap = Arrangement(listOf(a, b))
        // the corners of the contours are no vertices, where they meet are
        assertEquals(4, overlap.vertices.size)
        assertEquals(3, overlap.boundedFaces.size)
        assertTrue(noDoubleEdges(overlap))
        val faces = checkedFaces(overlap, listOf(a, b))
        assertEquals(setOf(setOf(a), setOf(a, b), setOf(b)), faces.toSet())
        // a alone, the overlap and b alone, each 50 wide
        val bounds = overlap.boundedFaces.associate { it.origins.toSet() to (it as BoundedFace).contour.bounds }
        assertEquals(-50.0, bounds.getValue(setOf(a)).x, 1E-6)
        assertEquals(0.0, bounds.getValue(setOf(a, b)).x, 1E-6)
        assertEquals(50.0, bounds.getValue(setOf(b)).x, 1E-6)
        for (r in bounds.values) assertEquals(50.0, r.width, 1E-6)

        // two squares that share a whole side, which runs one way along one and the other way along the other
        val left = Rectangle(0.0, 0.0, 100.0, 100.0).shape
        val right = Rectangle(100.0, 0.0, 100.0, 100.0).shape
        val adjacent = Arrangement(listOf(left, right))
        assertEquals(2, adjacent.vertices.size)
        assertEquals(3, adjacent.edges.size)
        assertTrue(noDoubleEdges(adjacent))
        assertEquals(setOf(setOf(left), setOf(right)), checkedFaces(adjacent, listOf(left, right)).toSet())

        // a grid of tiles: a face per tile, in that tile only
        val tiles = List(16) { k -> Rectangle((k % 4) * 50.0, (k / 4) * 50.0, 50.0, 50.0).shape }
        val grid = Arrangement(tiles)
        // the points of the lattice but the corners of the grid
        assertEquals(21, grid.vertices.size)
        assertTrue(noDoubleEdges(grid))
        assertEquals(tiles.map { setOf(it) }.toSet(), checkedFaces(grid, tiles).toSet())

        // a square inside another, sharing a corner and parts of two sides
        val outer = Rectangle(0.0, 0.0, 100.0, 100.0).shape
        val inner = Rectangle(0.0, 0.0, 40.0, 60.0).shape
        val nested = Arrangement(listOf(outer, inner))
        assertEquals(2, nested.boundedFaces.size)
        assertTrue(noDoubleEdges(nested))
        assertEquals(setOf(setOf(outer), setOf(outer, inner)), checkedFaces(nested, listOf(outer, inner)).toSet())

        // the same circle twice: one face in both
        val c1 = Circle(0.0, 0.0, 50.0).shape
        val c2 = Circle(0.0, 0.0, 50.0).shape
        val same = Arrangement(listOf(c1, c2))
        assertEquals(1, same.boundedFaces.size)
        assertEquals(setOf(c1, c2), same.boundedFaces.single().origins.toSet())
    }
}
