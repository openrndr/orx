package org.openrndr.extra.composition

import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TestComposition {
    val composition = let { _ ->
        val root = GroupNode().also { it.id = "outer" }
        root.children += GroupNode().also {
            it.id = "inner"
        }
        root.children += ShapeNode(Shape.EMPTY).also {
            it.id = "shape"
        }
        Composition(root)
    }

    @Test
    fun findGroup() {
        assertEquals("outer", composition.findGroup("outer")?.id)
        assertEquals("inner", composition.findGroup("inner")?.id)
        assertNull(composition.findGroup("shape"))
    }

    @Test
    fun findShape() {
        assertEquals("shape", composition.findShape("shape")?.id)
        assertNull(composition.findShape("inner"))
        assertNull(composition.findShape("outer"))
    }

    @Test
    fun findImage() {
        assertNull(composition.findImage("inner"))
        assertNull(composition.findImage("outer"))
        assertNull(composition.findImage("shape"))
    }
}

class TestCompositionIntersections {
    @Test
    fun `use a shape as a mask for line segments`() {
        // Make sure intersections do not fail randomly, which was fixed in
        // https://github.com/openrndr/orx/commit/e8f50b3dd153ed82de121e9017cf42f6ea95ac8e
        val bounds = Rectangle(Vector2.ZERO, 640.0, 480.0)

        // Create a 2D torus
        val outline = Shape(
            listOf(
                Circle(bounds.center, 70.0).contour.reversed,
                Circle(bounds.center, 100.0).contour,
            )
        )

        val radius = outline.bounds.dimensions.length / 2
        val off = outline.bounds.center

        val compositions = List(100) {
            // Create compositions featuring horizontal lines
            // visible inside the torus shape. Change the number of
            // lines to make sure the calculations are not cached.
            val num = radius.toInt() + it
            drawComposition {
                lineSegments(List(num) { segNum ->
                    val yNorm = (segNum / (num - 1.0))
                    val x = ((segNum % 2) * 2.0 - 1.0) * radius
                    val y = (yNorm * 2.0 - 1.0) * radius
                    val start = Vector2(-x, y) + off
                    val end = Vector2(x, y) + off
                    LineSegment(start, end)
                })
                clipMode = ClipMode.INTERSECT
                shape(outline)
            }
        }

        val shapes = compositions.last().findShapes()
        val dimensions = compositions.last().bounds.dimensions

        assertTrue(shapes.isNotEmpty(), "shapes should not be empty")
        assertTrue(shapes.first().shape.contours.isNotEmpty(), "contour should not be empty")
        assertTrue(dimensions.x > 0.0, "dimensions.x should be greater than 0.0")
        assertTrue(dimensions.y > 0.0, "dimensions.y should be greater than 0.0")
    }
}
