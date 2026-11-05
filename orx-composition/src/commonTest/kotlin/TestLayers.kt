package org.openrndr.extra.composition

import org.openrndr.color.ColorRGBa
import org.openrndr.shape.Rectangle
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestLayers {
    private fun shape(fill: Boolean, stroke: Boolean) = ShapeNode(Rectangle(0.0, 0.0, 10.0, 10.0).shape).also {
        it.fill = if (fill) ColorRGBa.RED else null
        it.stroke = if (stroke) ColorRGBa.BLACK else null
    }

    @Test
    fun layersGroupsWithOpacity() {
        val group = GroupNode()
        assertFalse(needsLayer(group, 1.0))
        assertTrue(needsLayer(group, 0.5))
        group.style.isolation = Isolation.ISOLATE
        assertTrue(needsLayer(group, 1.0))
    }

    @Test
    fun layersTranslucentShapesWithFillAndStroke() {
        assertFalse(needsLayer(shape(fill = true, stroke = false), 0.5))
        assertFalse(needsLayer(shape(fill = false, stroke = true), 0.5))
        assertTrue(needsLayer(shape(fill = true, stroke = true), 0.5))
        assertFalse(needsLayer(shape(fill = true, stroke = true), 1.0))
    }

    @Test
    fun layersMasksAndBlendModes() {
        val masked = shape(fill = true, stroke = false)
        masked.style.mask = MaskReference.Value(Mask(GroupNode()))
        assertTrue(needsLayer(masked, 1.0))
        val blended = shape(fill = true, stroke = false)
        blended.style.mixBlendMode = MixBlendMode.SCREEN
        assertTrue(needsLayer(blended, 1.0))
        val root = GroupNode().also { it.children.add(GroupNode().also { g -> g.children.add(blended) }) }
        assertTrue(usesBlendModes(root))
        assertFalse(usesBlendModes(GroupNode().also { it.children.add(shape(fill = true, stroke = true)) }))
    }

    @Test
    fun layersFilters() {
        val filtered = shape(fill = true, stroke = false)
        filtered.style.filter = FilterReference.Value(listOf(FilterEffect(listOf(FilterPrimitive.Flood(ColorRGBa.RED)))))
        assertTrue(needsLayer(filtered, 1.0))
    }
}
