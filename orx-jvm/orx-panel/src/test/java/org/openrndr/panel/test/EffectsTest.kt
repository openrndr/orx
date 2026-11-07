package org.openrndr.panel.test

import org.openrndr.color.ColorRGBa
import org.openrndr.extra.style.TimingFunction
import org.openrndr.math.Vector2
import org.openrndr.panel.StyleTransitions
import org.openrndr.panel.elementAt
import org.openrndr.panel.elements.*
import org.openrndr.panel.layout.Layouter
import org.openrndr.panel.style.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class EffectsTest {
    private fun rgb(value: Any?) = (value as Color.RGBa).color.toSRGB()

    private fun layouter(css: String) = Layouter().apply { styleSheets.addAll(styleSheets(css).flatMap { it.flatten() }) }

    @Test
    fun `attributes, checked and focus-within select elements`() {
        lateinit var save: Button
        lateinit var toggle: Toggle
        lateinit var box: Div
        val body = Body(null).apply {
            box = div("box") {
                save = button { label = "Save" }
                toggle = toggle { label = "on" }
            }
        }
        val layouter = layouter("""
            button[label="Save"] { width: 77px }
            [data-kind^=pri] { height: 11px }
            toggle:checked { color: red }
            .box:focus-within { background-color: blue }
        """)
        layouter.computeStyles(body)
        assertEquals(77.0, (save.computedStyle.width as LinearDimension.PX).value)
        // a change of an attribute restyles
        save.label = "Load"
        save.setAttribute("data-kind", "primary")
        layouter.updateStyles(body)
        assertSame(LinearDimension.Auto, save.computedStyle.width)
        assertEquals(11.0, (save.computedStyle.height as LinearDimension.PX).value)
        toggle.value = true
        layouter.updateStyles(body)
        assertEquals(1.0, rgb(toggle.computedStyle.color).r)
        box.pseudoClasses.add(ElementPseudoClass("focus-within"))
        layouter.updateStyles(body)
        assertEquals(1.0, rgb(box.computedStyle.background).b)
    }

    @Test
    fun `gradients and shadows are parsed`() {
        val style = StyleSheet().apply {
            css("""
                background: linear-gradient(to right, red, blue 80%);
                box-shadow: 0 4px 12px rgb(0 0 0 / 0.5), 2px 2px red
            """)
        }
        val image = style.backgroundImage as BackgroundImage.Linear
        assertEquals(90.0, image.angle)
        assertEquals(2, image.stops.size)
        assertEquals(80.0, (image.stops[1].position as LinearDimension.Percent).value)
        // the shorthand makes the background color transparent
        assertEquals(0.0, rgb(style.background).alpha)
        val shadows = style.boxShadow.shadows
        assertEquals(listOf(Vector2(0.0, 4.0), Vector2(2.0, 2.0)), shadows.map { it.offset })
        assertEquals(12.0, shadows[0].blur)
        assertEquals(0.5, shadows[0].color!!.alpha, 1E-9)
        val radial = StyleSheet().apply { css("background-image: radial-gradient(circle at 25% top, white, black)") }.backgroundImage
        assertEquals(BackgroundImage.Radial(true, Vector2(0.25, 0.0), (radial as BackgroundImage.Radial).stops), radial)
        val corner = StyleSheet().apply { css("background-image: repeating-linear-gradient(to bottom left, red 0, blue 10px)") }.backgroundImage
        assertEquals(Vector2(-1.0, 1.0), (corner as BackgroundImage.Linear).corner)
        assertTrue(corner.repeating)
        assertNull(StyleSheet().apply { css("box-shadow: inset 0 0 4px red") }["box-shadow"])
    }

    @Test
    fun `transforms transform elements and their descendants`() {
        lateinit var outer: Div
        lateinit var inner: Div
        val body = Body(null).apply {
            style { width = 400.px; height = 400.px }
            outer = div("outer") { inner = div("inner") { } }
        }
        val layouter = layouter("""
            .outer { width: 100px; height: 100px; transform: translate(100px, 50px) scale(2); transform-origin: left top }
            .inner { width: 10px; height: 10px }
        """)
        layouter.computeStyles(body)
        layouter.layout(body)
        // the inner box, 10 by 10 at the top left of the outer box, is at (100, 50) and 20 by 20 on the screen
        assertSame(inner, elementAt(body, Vector2(115.0, 65.0)) { it === inner || it === outer })
        assertSame(outer, elementAt(body, Vector2(125.0, 75.0)) { it === inner || it === outer })
        assertNull(elementAt(body, Vector2(5.0, 5.0)) { it === inner || it === outer })
        val bounds = inner.paintArea
        assertTrue(bounds.contains(Vector2(119.0, 69.0)) && bounds.width > 40.0)
        // transforms are parsed
        val t = StyleSheet().apply { css("transform: rotate(0.25turn) skewX(10deg) translateY(50%)") }.transform
        assertEquals(listOf("rotate", "skew", "translate"), t.functions.map { it.name })
        assertEquals(90.0, (t.functions[0].arguments[0] as LinearDimension.PX).value, 1E-9)
    }

    @Test
    fun `transforms transition and keyframes animate`() {
        var now = 0.0
        val transitions = StyleTransitions { now }
        lateinit var box: Div
        val body = Body(null).apply {
            style { width = 400.px; height = 300.px }
            box = div("box") { }
        }
        val layouter = layouter("""
            @keyframes pulse {
                from { opacity: 0.2 }
                50% { opacity: 1; background-color: #ff0000 }
                to { opacity: 0.2 }
            }
            .box { transition: transform 1s linear; width: 50px; height: 50px }
            .box:hover { transform: scale(2) }
            .grow { animation: grow 2s linear 1 forwards }
            @keyframes grow { to { width: 150px } }
            .pulse { animation: pulse 1s linear infinite }
        """)
        fun update(): Boolean {
            // as the control manager does, the styles of elements that animate are computed again in every frame
            for (element in transitions.animating) element.styleDirty = true
            layouter.updateStyles(body)
            return transitions.apply(body)
        }
        layouter.computeStyles(body)
        transitions.apply(body)
        // a transform transitions from none
        box.pseudoClasses.add(ElementPseudoClass("hover"))
        update()
        now = 0.5
        update()
        val scale = box.computedStyle.transform.functions.single()
        assertEquals(1.5, (scale.arguments[0] as LinearDimension.PX).value, 1E-9)
        now = 2.0
        box.pseudoClasses.remove(ElementPseudoClass("hover"))
        update()
        now = 4.0
        update()

        // an animation that repeats, between the keyframes; an animation of a width lays out
        box.classes.add(ElementClass("pulse"))
        update()
        now = 4.25
        update()
        assertEquals(0.6, box.computedStyle.opacity, 1E-9)
        assertTrue(box in transitions.animating)
        now = 5.5
        update()
        assertEquals(1.0, box.computedStyle.opacity, 1E-9)
        assertEquals(1.0, rgb(box.computedStyle.background).r, 1E-6)

        box.classes.remove(ElementClass("pulse"))
        box.classes.add(ElementClass("grow"))
        update()
        now = 6.5
        val layout = update()
        assertTrue(layout)
        assertEquals(100.0, (box.computedStyle.width as LinearDimension.PX).value, 1E-9)
        // it ends and keeps its last values
        now = 9.0
        update()
        assertEquals(150.0, (box.computedStyle.width as LinearDimension.PX).value, 1E-9)
        assertTrue(box !in transitions.animating)
    }
}
