package org.openrndr.panel.test

import org.openrndr.color.ColorRGBa
import org.openrndr.extra.style.CssKeyword
import org.openrndr.extra.style.TimingFunction
import org.openrndr.panel.elements.*
import org.openrndr.panel.layout.Layouter
import org.openrndr.panel.style.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CssStyleTest {
    private fun px(value: Any?) = (value as LinearDimension.PX).value
    private fun rgb(value: Any?) = (value as Color.RGBa).color.toSRGB()

    @Test
    fun `declarations are parsed into the values of properties`() {
        val style = StyleSheet().apply {
            css("""
                width: 1in; height: 50%; min-width: minmax(10px, 1fr);
                display: flex; flex-direction: column; justify-content: space-between;
                background-color: #ff0000; color: rgb(0 0 255 / 50%);
                opacity: 50%; z-index: 3; cursor: pointer; overflow: auto;
                font-family: Plex, default; font-weight: bold; font-style: italic;
                grid-template-columns: 100px repeat(2, 1fr); grid-column: 2 / 4; grid-row: span 2;
                transition-duration: 120ms; transition-timing-function: linear;
            """)
        }
        assertEquals(96.0, px(style.width))
        assertEquals(50.0, (style.height as LinearDimension.Percent).value)
        assertTrue(style.minWidth is LinearDimension.MinMax)
        assertSame(Display.FLEX, style.display)
        assertSame(FlexDirection.Column, style.flexDirection)
        assertSame(JustifyContent.SpaceBetween, style.justifyContent)
        assertEquals(1.0, rgb(style.background).r)
        assertEquals(0.5, rgb(style.color).alpha, 1E-9)
        assertEquals(0.5, style.opacity)
        assertEquals(3, (style.zIndex as ZIndex.Value).value)
        assertSame(Cursor.Pointer, style.cursor)
        assertSame(Overflow.Scroll, style.overflow)
        assertEquals("Plex, default", style.fontFamily)
        assertEquals(700, style.fontWeight)
        assertEquals(FontStyle.ITALIC, style.fontStyle)
        assertEquals(3, (style.gridTemplateColumns as GridTemplate.LengthList).value.size)
        // grid lines are counted from 1, tracks from 0
        assertEquals(1..2, (style.gridColumn as GridPopulation.Span).value)
        assertEquals(2, (style.gridRow as GridPopulation.Length).value)
        assertEquals(0.12, style.transitionDuration, 1E-9)
        assertSame(TimingFunction.LINEAR, style.transitionTimingFunction)
    }

    @Test
    fun `shorthands set their longhands`() {
        val style = StyleSheet().apply {
            css("margin: 1px 2px; padding: 1px 2px 3px 4px; border: 2px solid #fff; gap: 4px 8px; flex: 2")
            this["transition"] = "background-color 0.2s ease-in 50ms, opacity 0.2s"
            this["text-align"] = "center"
        }
        assertEquals(listOf(1.0, 2.0, 1.0, 2.0), listOf(style.marginTop, style.marginRight, style.marginBottom, style.marginLeft).map(::px))
        assertEquals(listOf(1.0, 2.0, 3.0, 4.0), listOf(style.paddingTop, style.paddingRight, style.paddingBottom, style.paddingLeft).map(::px))
        assertEquals(2.0, px(style.borderWidth))
        assertEquals(1.0, rgb(style.borderColor).g)
        assertEquals(4.0, px(style.rowGap))
        assertEquals(8.0, px(style.columnGap))
        assertEquals(2.0, (style.flexGrow as FlexGrow.Ratio).value)
        assertEquals(listOf("background-color", "opacity"), style.transitionProperty)
        assertEquals(0.2, style.transitionDuration, 1E-9)
        assertEquals(0.05, style.transitionDelay, 1E-9)
        assertEquals(0.5, (style.textHorizontalAlign as TextAlign.Value).value)
    }

    @Test
    fun `values that are not valid and unknown properties are left out`() {
        val style = StyleSheet().apply { css("width: wide; unknown: 1; height: 10px; color: notacolor") }
        assertNull(style["width"])
        assertNull(style["unknown"])
        assertNull(style["color"])
        assertEquals(10.0, px(style.height))
    }

    @Test
    fun `important declarations win, keywords take the values of parents or initial values`() {
        lateinit var child: Div
        val body = Body(null).apply {
            style = StyleSheet().apply { css("color: red; width: 300px; height: 200px") }
            div("box") {
                child = div("inner") {
                    style = StyleSheet().apply { css("width: inherit; color: initial; background-color: blue") }
                }
            }
        }
        val layouter = Layouter().apply {
            styleSheets.addAll(listOf(
                styleSheet(has class_ "inner") { css("background-color: green !important; width: 10px") },
            ).flatMap { it.flatten() })
        }
        layouter.computeStyles(body)
        // the important declaration of the sheet wins over the inline declaration that is not
        assertEquals(0.5, rgb(child.computedStyle.background).g, 0.01)
        // inherit takes the computed value of the parent, the box, which is auto
        assertSame(LinearDimension.Auto, child.computedStyle.width)
        // initial is the initial value, white, not the inherited red
        assertEquals(ColorRGBa.WHITE.toSRGB(), rgb(child.computedStyle.color))
        // a keyword is resolved, the property is not left as a keyword
        assertTrue(child.computedStyle["width"] !is CssKeyword)
    }
}
