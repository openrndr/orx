package org.openrndr.extra.composition

import org.openrndr.color.ColorRGBa
import org.openrndr.math.Matrix44
import org.openrndr.math.transforms.buildTransform
import org.openrndr.shape.Rectangle
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TestVisualBounds {
    private fun square(stroke: Boolean = false) = ShapeNode(Rectangle(0.0, 0.0, 10.0, 10.0).shape).also {
        it.fill = ColorRGBa.RED
        it.stroke = if (stroke) ColorRGBa.BLACK else null
    }

    @Test
    fun widensStrokes() {
        assertEquals(Rectangle(0.0, 0.0, 10.0, 10.0), visualBounds(square()))
        val stroked = square(stroke = true).also { it.strokeWeight = 2.0 }
        // half the stroke weight, times the miter limit for miter joins
        assertEquals(Rectangle(-4.0, -4.0, 18.0, 18.0), visualBounds(stroked))
        stroked.style.lineJoin = LineJoin.Round
        assertEquals(Rectangle(-1.0, -1.0, 12.0, 12.0), visualBounds(stroked))
        stroked.style.lineCap = LineCap.Square
        val d = sqrt(2.0)
        assertEquals(-d, visualBounds(stroked)!!.x, 1E-9)
        stroked.style.markerEnd = MarkerReference.Value(Marker(GroupNode()))
        assertNull(visualBounds(stroked))
    }

    @Test
    fun combinesGroupsAndFilters() {
        val group = GroupNode()
        group.children.add(square().also { it.parent = group })
        group.children.add(square().also { it.parent = group; it.transform = buildTransform { translate(20.0, 0.0) } })
        assertEquals(Rectangle(0.0, 0.0, 30.0, 10.0), visualBounds(group))
        // the region of a filter, in the bounds of the group
        group.style.filter = FilterReference.Value(listOf(FilterEffect(listOf(FilterPrimitive.Flood(ColorRGBa.RED)))))
        val b = visualBounds(group)!!
        assertEquals(-3.0, b.x, 1E-9)
        assertEquals(36.0, b.width, 1E-9)
        // filters without a region reach as far as their blurs
        group.style.filter = FilterReference.Value(listOf(FilterEffect(
            listOf(FilterPrimitive.GaussianBlur(stdDeviation = org.openrndr.math.Vector2(2.0, 1.0))), region = null)))
        assertEquals(Rectangle(-6.0, -6.0, 42.0, 22.0), visualBounds(group))
    }

    @Test
    fun mapsBoundsToPixels() {
        // a target of 100 by 50 points, user space maps to clip space like an orthographic projection
        val toClip = Matrix44(2.0 / 100.0, 0.0, 0.0, -1.0, 0.0, -2.0 / 50.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0)
        val box = pixelBox(Rectangle(10.0, 10.0, 20.0, 10.0), toClip, 100.0, 50.0, 1.0)!!
        // two pixels of margin, the clip from the top
        assertEquals(Rectangle(8.0, 8.0, 24.0, 14.0), box.clip)
        assertNull(pixelBox(Rectangle(200.0, 10.0, 20.0, 10.0), toClip, 100.0, 50.0, 1.0))
    }
}
