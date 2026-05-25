package dcel.convert

import org.openrndr.extra.mesh.dcel.convert.angularSweep
import org.openrndr.extra.mesh.dcel.query.edgeCount
import org.openrndr.extra.mesh.dcel.query.edgesForFace
import org.openrndr.extra.mesh.dcel.query.faceCount
import org.openrndr.extra.mesh.dcel.query.faceWinding
import org.openrndr.extra.mesh.dcel.query.vertexCount
import org.openrndr.extra.mesh.dcel.validate.isEulerMesh
import org.openrndr.extra.shapes.primitives.regularPolygon
import org.openrndr.math.Vector2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestAngularSweep {

    @Test
    fun testAngularSweepSimple() {
        val polygon = regularPolygon(5, Vector2.ZERO, 100.0)
        val points = polygon.segments.map { it.start }
        val edges = (0 until polygon.segments.size).map { it to (it + 1).mod(polygon.segments.size) }
        val dcel = angularSweep(points, edges)
        assertEquals(1, dcel.faceCount())
        assertEquals(5, dcel.edgesForFace(0).size)
        assertTrue(dcel.isEulerMesh())
        assertEquals(polygon.winding, dcel.faceWinding(0))
    }

    @Test
    fun testAngularSweepSimple2() {
        val polygon = regularPolygon(6, Vector2.ZERO, 100.0)
        val points = polygon.segments.map { it.start }
        val edges = (0 until polygon.segments.size).map { it to (it + 1).mod(polygon.segments.size) } + listOf(0 to 2)
        val dcel = angularSweep(points, edges)
        assertEquals(2, dcel.faceCount())
        assertTrue(dcel.isEulerMesh())
        assertEquals(polygon.winding, dcel.faceWinding(0))
        assertEquals(polygon.winding, dcel.faceWinding(1))
    }

    @Test
    fun testAngularSweepSimple3() {
        val polygon = regularPolygon(6, Vector2.ZERO, 100.0)
        val points = polygon.segments.map { it.start } + listOf(Vector2.ZERO)
        val edges = (0 until polygon.segments.size).map { it to (it + 1).mod(polygon.segments.size) } +
        (0 until polygon.segments.size).map { it to 6 }
        val dcel = angularSweep(points, edges)
        assertEquals(6, dcel.faceCount())
        assertTrue(dcel.isEulerMesh())
        assertEquals(polygon.winding, dcel.faceWinding(0))
        assertEquals(polygon.winding, dcel.faceWinding(1))
    }

    @Test
    fun testAngularSweepPartial() {
        val polygon = regularPolygon(6, Vector2.ZERO, 100.0)
        val polygon2 = regularPolygon(6, Vector2.ZERO, 200.0)
        val points = polygon.segments.map { it.start } + polygon2.segments.map { it.start }
        val edges = (0 until polygon.segments.size).map { it to (it + 1).mod(polygon.segments.size) } +
                (0 until polygon.segments.size).map { it to it + 6 }
        val dcel = angularSweep(points, edges)
        assertEquals(1, dcel.faceCount())
        assertTrue(dcel.isEulerMesh())
        assertEquals(polygon.winding, dcel.faceWinding(0))
        assertEquals(6, dcel.edgeCount())
        assertEquals(6, dcel.vertexCount())
    }
}