package org.openrndr.extra.composition

import org.openrndr.MouseButton
import org.openrndr.MouseEvent
import org.openrndr.MouseEventType
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.buildTransform
import org.openrndr.shape.Rectangle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestInteraction {
    private fun square(id: String, x: Double) = ShapeNode(Rectangle(x, 0.0, 10.0, 10.0).shape).also {
        it.id = id
        it.fill = ColorRGBa.RED
    }

    private val composition = run {
        val root = GroupNode().also { it.id = "root" }
        val link = GroupNode().also { it.id = "link"; it.link = "https://openrndr.org"; it.parent = root }
        link.children.add(square("a", 0.0).also { it.parent = link })
        root.children.add(link)
        root.children.add(square("b", 20.0).also { it.parent = root })
        Composition(root)
    }

    private fun event(x: Double) =
        MouseEvent(Vector2(x, 5.0), Vector2.ZERO, Vector2.ZERO, MouseEventType.BUTTON_DOWN, MouseButton.LEFT, emptySet())

    @Test
    fun sendsPointerEvents() {
        val interaction = CompositionInteraction(composition)
        val log = mutableListOf<String>()
        interaction.entered.listen { log.add("enter ${it.node.id}") }
        interaction.exited.listen { log.add("exit ${it.node.id}") }
        interaction.clicked.listen { log.add("click ${it.node.id} ${it.link}") }

        interaction.move(Vector2(5.0, 5.0))
        assertEquals(listOf("enter root", "enter link", "enter a"), log)
        log.clear()
        interaction.move(Vector2(25.0, 5.0))
        assertEquals(listOf("exit a", "exit link", "enter b"), log)
        log.clear()

        interaction.press(event(5.0))
        interaction.release(event(5.0))
        assertEquals(listOf("click a https://openrndr.org"), log)
        log.clear()
        // a press and a release on different nodes is not a click
        interaction.press(event(5.0))
        interaction.release(event(25.0))
        assertTrue(log.isEmpty())
    }

    @Test
    fun followsTheTransformOfTheComposition() {
        val interaction = CompositionInteraction(composition)
        interaction.transform = buildTransform { scale(2.0, 2.0, 1.0) }
        interaction.move(Vector2(45.0, 10.0))
        assertEquals("b", interaction.hoveredNode?.id)
    }
}
