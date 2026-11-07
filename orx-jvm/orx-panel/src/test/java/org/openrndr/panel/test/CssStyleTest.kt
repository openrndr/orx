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

    @Test
    fun `style sheets of CSS select elements by type, class, state and structure`() {
        lateinit var first: Button
        lateinit var second: Button
        lateinit var inner: Div
        val body = Body(null).apply {
            div("toolbar") {
                first = button { }
                second = button { classes.add(ElementClass("primary")) }
            }
            div("panel") { inner = div("inner") { } }
        }
        val sheets = styleSheets("""
            /* a comment */
            .toolbar > button { background-color: gray; width: 50px }
            .toolbar > button:hover, .primary { background-color: blue }
            button.primary { width: 80px }
            .toolbar button:first-child { height: 10px }
            .panel :not(.toolbar) { color: red }
            div::before { color: green }
            .toolbar > button:nth-child(2n) { border-width: 2px !important }
            button.primary { border-width: 1px }
        """)
        // the rule with a pseudo-element is left out, the selector list gives a sheet per selector
        assertEquals(8, sheets.size)
        val layouter = Layouter().apply { styleSheets.addAll(sheets.flatMap { it.flatten() }) }
        layouter.computeStyles(body)
        assertEquals(0.5, rgb(first.computedStyle.background).r, 0.01)
        assertEquals(50.0, px(first.computedStyle.width))
        assertEquals(10.0, px(first.computedStyle.height))
        // .toolbar > button is more specific than .primary, button.primary than .toolbar > button
        assertEquals(0.5, rgb(second.computedStyle.background).r, 0.01)
        assertEquals(80.0, px(second.computedStyle.width))
        // the important declaration wins over the more specific one that is not
        assertEquals(2.0, px(second.computedStyle.borderWidth))
        assertEquals(1.0, rgb(inner.computedStyle.color).r)
        // states are pseudo-classes
        first.pseudoClasses.add(ElementPseudoClass("hover"))
        layouter.updateStyles(body)
        assertEquals(1.0, rgb(first.computedStyle.background).b)
    }

    @Test
    fun `selectors of the builder and of CSS are the same`() {
        val built = styleSheet(has type "button") {
            descendant(has class_ "label" and (has state "hover")) { }
        }.flatten().last()
        assertEquals("button .label:hover", built.cssSelector.toString())
        assertEquals(built.specificity, styleSheet("button .label:hover") { }.specificity)
        assertEquals(":is(slider, button)", (has type listOf("slider", "button")).toString())
    }

    @Test
    fun `custom properties are inherited and used with var()`() {
        lateinit var child: Div
        lateinit var other: Div
        val body = Body(null).apply {
            div("theme") {
                child = div("item") { }
                other = div("other") { }
            }
        }
        val layouter = Layouter().apply {
            styleSheets.addAll(styleSheets("""
                .theme { --accent: #0000ff; --gap: 6px; --space: calc(var(--gap) * 2) }
                .item { background-color: var(--accent); margin: var(--space) var(--gap) }
                .other { --accent: red; color: var(--accent); width: var(--missing, 30px); height: var(--missing) }
            """).flatMap { it.flatten() })
        }
        layouter.computeStyles(body)
        assertEquals(1.0, rgb(child.computedStyle.background).b)
        assertEquals(12.0, px(child.computedStyle.marginTop))
        assertEquals(6.0, px(child.computedStyle.marginLeft))
        // a custom property of the element itself, a fallback, and a value without a fallback that is unset
        assertEquals(1.0, rgb(other.computedStyle.color).r)
        assertEquals(30.0, px(other.computedStyle.width))
        assertSame(LinearDimension.Auto, other.computedStyle.height)
    }

    @Test
    fun `the default styles take their colors and sizes from the theme`() {
        lateinit var button: Button
        val body = Body(null).apply { button = button { label = "b" } }
        val sheets = defaultStyles() + styleSheets(":root { --control-background: red; --control-font-size: 28px }")
        Layouter().apply { styleSheets.addAll(sheets.flatMap { it.flatten() }) }.computeStyles(body)
        assertEquals(1.0, rgb(button.computedStyle.background).r)
        // sizes derive from the font size
        assertEquals(28.0, px(button.computedStyle.fontSize))
        assertEquals(64.0, px(button.computedStyle.height))
        assertEquals(10.0, px(button.computedStyle.marginTop))
    }

    @Test
    fun `shorthands with var() take part in the cascade like their longhands`() {
        lateinit var item: Div
        val body = Body(null).apply { div("list") { item = div("item") { } } }
        val sheets = styleSheets("""
            :root { --space: 4px }
            .list .item { margin-left: 20px }
            .item { margin: var(--space) }
        """)
        Layouter().apply { styleSheets.addAll(sheets.flatMap { it.flatten() }) }.computeStyles(body)
        // the more specific longhand wins over the shorthand with var(), the other sides take its value
        assertEquals(20.0, px(item.computedStyle.marginLeft))
        assertEquals(4.0, px(item.computedStyle.marginTop))
    }

    @Test
    fun `css files are read again when they change, in their place`() {
        val file = java.io.File.createTempFile("panel", ".css").apply { deleteOnExit() }
        file.writeText(".a { width: 10px }")
        val document = org.openrndr.panel.document.Document()
        val first = styleSheet(".a") { height = 1.px }
        document.styleSheets.add(first)
        val css = org.openrndr.panel.CssFile(file, true, document)
        val last = styleSheet(".a") { height = 2.px }
        document.styleSheets.add(last)
        assertEquals(1, css.styleSheets.size)
        assertEquals(10.0, px(css.styleSheets[0].width))
        // not changed, not read again
        assertTrue(!css.reload())
        file.writeText(".a { width: 20px } .b { width: 30px }")
        file.setLastModified(file.lastModified() + 2000)
        assertTrue(css.reload())
        assertEquals(2, css.reads)
        // the new style sheets are where the old ones were, between the style sheets around them
        val sheets = document.styleSheets
        assertSame(first, sheets.first())
        assertSame(last, sheets.last())
        assertEquals(listOf(20.0, 30.0), css.styleSheets.map { px(it.width) })
        assertEquals(css.styleSheets, sheets.subList(sheets.size - 3, sheets.size - 1))
        css.remove()
        assertEquals(listOf(first, last), document.styleSheets.toList())
    }
}
