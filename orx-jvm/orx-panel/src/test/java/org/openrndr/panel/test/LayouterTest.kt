package org.openrndr.panel.test

import org.openrndr.panel.elements.Body
import org.openrndr.panel.elements.Div
import org.openrndr.panel.elements.Element
import org.openrndr.panel.elements.TextNode
import org.openrndr.panel.elements.style
import org.openrndr.panel.layout.Layouter
import org.openrndr.panel.style.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import org.openrndr.color.ColorRGBa

class LayouterTest {
    private val epsilon = 1E-9

    private fun body(width: Double = 400.0, height: Double = 300.0, init: Element.() -> Unit): Body =
        Body(null).apply {
            style {
                this.width = width.px
                this.height = height.px
            }
            init()
        }

    private fun Element.div(style: StyleSheet.() -> Unit = {}, init: Element.() -> Unit = {}): Div {
        val div = Div()
        div.style(style)
        append(div)
        div.init()
        return div
    }

    private fun layout(root: Element): Layouter {
        val layouter = Layouter()
        layouter.computeStyles(root)
        layouter.layout(root)
        return layouter
    }

    private fun assertBounds(element: Element, x: Double, y: Double, width: Double, height: Double) {
        assertEquals(x, element.layout.screenX, epsilon, "x of $element")
        assertEquals(y, element.layout.screenY, epsilon, "y of $element")
        assertEquals(width, element.layout.screenWidth, epsilon, "width of $element")
        assertEquals(height, element.layout.screenHeight, epsilon, "height of $element")
    }

    @Test
    fun `block stacks its children inside its padding`() {
        lateinit var parent: Div
        lateinit var a: Div
        lateinit var b: Div
        layout(body {
            parent = div({ padding(10.px) }) {
                a = div({ width = 100.px; height = 20.px; margins(5.px) })
                b = div({ width = 50.px; height = 30.px; margins(5.px) })
            }
        })
        assertBounds(a, 15.0, 15.0, 100.0, 20.0)
        assertBounds(b, 15.0, 45.0, 50.0, 30.0)
        // auto sizes count the padding once
        assertBounds(parent, 0.0, 0.0, 10.0 + 110.0 + 10.0, 10.0 + 30.0 + 40.0 + 10.0)
    }

    @Test
    fun `percentages are taken of the content of the container`() {
        lateinit var child: Div
        layout(body {
            div({ width = 200.px; height = 100.px; padding(10.px) }) {
                child = div({ width = 50.percent; height = 100.percent; marginLeft = 10.px })
            }
        })
        // 50% of 180 minus the margin, 100% of 80
        assertBounds(child, 20.0, 10.0, 80.0, 80.0)
    }

    @Test
    fun `percentage margins are taken of the container width`() {
        lateinit var child: Div
        layout(body {
            div({ width = 200.px; height = 100.px }) {
                child = div({ width = 50.px; height = 20.px; marginLeft = 10.percent; marginTop = 5.percent })
            }
        })
        assertBounds(child, 20.0, 10.0, 50.0, 20.0)
    }

    @Test
    fun `calculate widths and heights`() {
        lateinit var child: Div
        layout(body {
            child = div({
                width = LinearDimension.Calculate { 80.0 }
                height = LinearDimension.Calculate { it.elementWidth!! / 2.0 }
            })
        })
        assertBounds(child, 0.0, 0.0, 80.0, 40.0)
    }

    @Test
    fun `flex row grows its children into the space left by padding and gaps`() {
        lateinit var row: Div
        lateinit var a: Div
        lateinit var b: Div
        lateinit var c: Div
        layout(body {
            row = div({
                display = Display.FLEX; flexDirection = FlexDirection.Row
                width = 300.px; height = 50.px; padding(10.px); columnGap = 5.px
            }) {
                a = div({ width = 50.px; height = 10.px })
                b = div({ width = 50.px; height = 10.px; flexGrow = FlexGrow.Ratio(1.0) })
                c = div({ width = 50.px; height = 10.px; flexGrow = FlexGrow.Ratio(3.0) })
            }
        })
        // 280 content - 150 widths - 10 gaps leaves 120: 30 to b, 90 to c
        assertBounds(a, 10.0, 10.0, 50.0, 10.0)
        assertBounds(b, 65.0, 10.0, 80.0, 10.0)
        assertBounds(c, 150.0, 10.0, 140.0, 10.0)
        assertEquals(300.0, row.layout.screenWidth, epsilon)
    }

    @Test
    fun `auto width of a flex row has no trailing gap`() {
        lateinit var row: Div
        layout(body {
            row = div({
                display = Display.FLEX; flexDirection = FlexDirection.Row
                width = LinearDimension.Auto; height = 20.px; columnGap = 5.px; paddingLeft = 3.px
            }) {
                div({ width = 50.px; height = 10.px })
                div({ width = 50.px; height = 10.px })
            }
        })
        assertEquals(3.0 + 50.0 + 5.0 + 50.0, row.layout.screenWidth, epsilon)
    }

    @Test
    fun `flex row shrinks its children weighted by their widths`() {
        lateinit var a: Div
        lateinit var b: Div
        layout(body {
            div({ display = Display.FLEX; flexDirection = FlexDirection.Row; width = 100.px; height = 20.px }) {
                a = div({ width = 100.px; height = 10.px })
                b = div({ width = 50.px; height = 10.px })
            }
        })
        // 50 too wide, a gives way twice as much as b
        assertEquals(100.0 - 50.0 * 2.0 / 3.0, a.layout.screenWidth, epsilon)
        assertEquals(50.0 - 50.0 / 3.0, b.layout.screenWidth, epsilon)
    }

    @Test
    fun `flex column shrinks only the children that may shrink`() {
        lateinit var toolbar: Div
        lateinit var sidebar: Div
        layout(body {
            div({ display = Display.FLEX; flexDirection = FlexDirection.Column; width = 100.px; height = 300.px }) {
                toolbar = div({ width = 100.px; height = 40.px; flexShrink = FlexGrow.Ratio(0.0) })
                sidebar = div({ width = 100.px; height = 100.percent })
            }
        })
        assertBounds(toolbar, 0.0, 0.0, 100.0, 40.0)
        assertBounds(sidebar, 0.0, 40.0, 100.0, 260.0)
    }

    @Test
    fun `flex column grows its children`() {
        lateinit var spacer: Div
        lateinit var bar: Div
        val root = body {
            div({ display = Display.FLEX; flexDirection = FlexDirection.Column; width = 100.px; height = 300.px }) {
                spacer = div({ width = 100.px; height = 0.px; flexGrow = FlexGrow.Ratio(1.0) })
                bar = div({ width = 100.px; height = 50.px })
            }
        }
        layout(root)
        assertBounds(spacer, 0.0, 0.0, 100.0, 250.0)
        assertBounds(bar, 0.0, 250.0, 100.0, 50.0)

    }

    @Test
    fun `flex column applies scrolling`() {
        lateinit var first: Div
        layout(body {
            div({
                display = Display.FLEX; flexDirection = FlexDirection.Column
                width = 100.px; height = 300.px; overflow = Overflow.Scroll
            }) {
                first = div({ width = 100.px; height = 200.px; flexShrink = FlexGrow.Ratio(0.0) })
                div({ width = 100.px; height = 200.px; flexShrink = FlexGrow.Ratio(0.0) })
            }.scrollTop = 30.0
        })
        assertEquals(-30.0, first.layout.screenY, epsilon)
    }

    @Test
    fun `grid places items in cells and fills them`() {
        lateinit var wide: Div
        lateinit var a: Div
        lateinit var b: Div
        layout(body {
            div({
                display = Display.GRID
                width = 210.px; height = 210.px; padding(5.px); columnGap = 10.px; rowGap = 10.px
                gridTemplateColumns = GridTemplate.LengthList(listOf(LinearDimension.Fraction(1.0), LinearDimension.Fraction(1.0)))
                gridTemplateRows = GridTemplate.LengthList(listOf(50.px, 50.px))
            }) {
                wide = div({ gridColumn = GridPopulation.Length(2) })
                // hidden items do not take a cell
                div({ display = Display.NONE })
                a = div()
                b = div({ marginLeft = 10.percent })
            }
        })
        assertBounds(wide, 5.0, 5.0, 200.0, 50.0)
        assertBounds(a, 5.0, 65.0, 95.0, 50.0)
        // the cell is the reference for percentages
        assertBounds(b, 110.0 + 9.5, 65.0, 95.0 - 9.5, 50.0)
    }

    @Test
    fun `absolute elements are placed relative to their parent`() {
        lateinit var parent: Div
        lateinit var absolute: Div
        lateinit var after: Div
        layout(body {
            div({ width = 400.px; height = 10.px })
            parent = div({ width = 200.px; height = 100.px }) {
                absolute = div({
                    position = Position.ABSOLUTE; left = 50.percent; top = 50.percent
                    width = 20.px; height = 20.px
                })
                after = div({ width = 10.px; height = 10.px })
            }
        })
        assertBounds(absolute, 100.0, 60.0, 20.0, 20.0)
        // absolute elements are not part of the flow
        assertBounds(after, 0.0, 10.0, 10.0, 10.0)
    }

    @Test
    fun `measuring does not change the layout`() {
        lateinit var row: Div
        lateinit var child: Div
        val root = body {
            row = div({ display = Display.FLEX; flexDirection = FlexDirection.Row; width = 300.px; height = 50.px }) {
                child = div({ width = 50.px; height = 10.px; flexGrow = FlexGrow.Ratio(1.0) })
            }
        }
        val layouter = layout(root)
        assertBounds(child, 0.0, 0.0, 300.0, 10.0)
        assertEquals(50.0, layouter.width(child), epsilon)
        assertEquals(10.0, layouter.height(child), epsilon)
        assertBounds(child, 0.0, 0.0, 300.0, 10.0)
    }

    @Test
    fun `text nodes take the look of their parent but not its box`() {
        lateinit var parent: Div
        lateinit var autoParent: Div
        val text = TextNode("text")
        val autoText = TextNode("text")
        Layouter().computeStyles(body {
            parent = div({
                width = 200.px; height = 50.px; margins(10.px); padding(5.px); borderWidth = 2.px
                position = Position.ABSOLUTE; left = 20.px; top = 30.px
                color = Color.RGBa(ColorRGBa.PINK)
            })
            parent.append(text)
            autoParent = div({ width = LinearDimension.Auto; height = LinearDimension.Auto })
            autoParent.append(autoText)
        })
        val style = text.computedStyle
        assertEquals(Color.RGBa(ColorRGBa.PINK).color, (style.color as Color.RGBa).color)
        for (length in listOf(style.marginTop, style.marginLeft, style.paddingTop, style.paddingLeft, style.borderWidth)) {
            assertEquals(0.0, assertIs<LinearDimension.PX>(length).value)
        }
        assertEquals(Position.STATIC, style.position)
        // the text fills a parent with a definite size, and sizes to itself in a parent that sizes to its content
        assertEquals(100.0, assertIs<LinearDimension.Percent>(style.width).value)
        assertEquals(100.0, assertIs<LinearDimension.Percent>(style.height).value)
        assertEquals(LinearDimension.Auto, autoText.computedStyle.width)
        assertEquals(LinearDimension.Auto, autoText.computedStyle.height)
    }

    @Test
    fun `scrolling is limited to the content`() {
        lateinit var scroller: Div
        lateinit var first: Div
        val root = body {
            scroller = div({ width = 100.px; height = 100.px; paddingBottom = 10.px; overflow = Overflow.Scroll }) {
                first = div({ width = 100.px; height = 150.px })
                div({ width = 100.px; height = 100.px })
            }
        }
        layout(root)
        assertEquals(260.0, scroller.layout.scrollHeight, epsilon)
        assertEquals(160.0, scroller.maxScrollTop, epsilon)

        scroller.scrollTop = 1000.0
        layout(root)
        assertEquals(160.0, scroller.scrollTop, epsilon)
        assertEquals(-160.0, first.layout.screenY, epsilon)

        // the scroll position follows content that shrinks
        first.style { height = 50.px }
        layout(root)
        assertEquals(60.0, scroller.scrollTop, epsilon)
    }

    private fun Element.row(style: StyleSheet.() -> Unit, init: Element.() -> Unit) = div({
        display = Display.FLEX; flexDirection = FlexDirection.Row
        style()
    }, init)

    @Test
    fun `align items places items across a row`() {
        val items = mutableListOf<Div>()
        for (align in listOf(AlignItems.Start, AlignItems.Center, AlignItems.End, AlignItems.Stretch)) {
            layout(body {
                row({ width = 100.px; height = 50.px; alignItems = align }) {
                    items.add(div({ width = 10.px; height = if (align == AlignItems.Stretch) LinearDimension.Auto else 20.px; marginTop = 2.px }))
                }
            })
        }
        assertEquals(2.0, items[0].layout.screenY, epsilon)
        assertEquals(2.0 + (50.0 - 22.0) / 2.0, items[1].layout.screenY, epsilon)
        assertEquals(50.0 - 20.0, items[2].layout.screenY, epsilon)
        // stretched items fill the line, minus their margins
        assertBounds(items[3], 0.0, 2.0, 10.0, 48.0)
    }

    @Test
    fun `justify content distributes the space left in a row`() {
        fun positions(justify: JustifyContent): List<Double> {
            val items = mutableListOf<Div>()
            layout(body {
                row({ width = 100.px; height = 10.px; justifyContent = justify }) {
                    repeat(2) { items.add(div({ width = 20.px; height = 10.px })) }
                }
            })
            return items.map { it.layout.screenX }
        }
        assertEquals(listOf(0.0, 20.0), positions(JustifyContent.Start))
        assertEquals(listOf(60.0, 80.0), positions(JustifyContent.End))
        assertEquals(listOf(30.0, 50.0), positions(JustifyContent.Center))
        assertEquals(listOf(0.0, 80.0), positions(JustifyContent.SpaceBetween))
        assertEquals(listOf(15.0, 65.0), positions(JustifyContent.SpaceAround))
        assertEquals(listOf(20.0, 60.0), positions(JustifyContent.SpaceEvenly))
    }

    @Test
    fun `reversed directions place items from the end`() {
        lateinit var a: Div
        lateinit var b: Div
        lateinit var c: Div
        lateinit var d: Div
        layout(body {
            row({ width = 100.px; height = 10.px; flexDirection = FlexDirection.RowReverse; columnGap = 5.px }) {
                a = div({ width = 20.px; height = 10.px; marginRight = 3.px })
                b = div({ width = 30.px; height = 10.px })
            }
            div({ display = Display.FLEX; flexDirection = FlexDirection.ColumnReverse; width = 100.px; height = 100.px }) {
                c = div({ width = 10.px; height = 20.px })
                d = div({ width = 10.px; height = 30.px })
            }
        })
        assertEquals(100.0 - 3.0 - 20.0, a.layout.screenX, epsilon)
        assertEquals(100.0 - 23.0 - 5.0 - 30.0, b.layout.screenX, epsilon)
        assertEquals(10.0 + 100.0 - 20.0, c.layout.screenY, epsilon)
        assertEquals(10.0 + 100.0 - 20.0 - 30.0, d.layout.screenY, epsilon)
    }

    @Test
    fun `wrapping rows break items into lines`() {
        lateinit var row: Div
        val items = mutableListOf<Div>()
        layout(body {
            row = row({ width = 100.px; height = LinearDimension.Auto; flexWrap = FlexWrap.Wrap; columnGap = 10.px; rowGap = 5.px }) {
                for (height in listOf(20.0, 30.0, 10.0)) items.add(div({ width = 40.px; this.height = height.px }))
            }
        })
        assertBounds(items[0], 0.0, 0.0, 40.0, 20.0)
        assertBounds(items[1], 50.0, 0.0, 40.0, 30.0)
        // the third item does not fit, it starts a line below the tallest item of the first line
        assertBounds(items[2], 0.0, 35.0, 40.0, 10.0)
        assertEquals(30.0 + 5.0 + 10.0, row.layout.screenHeight, epsilon)
    }

    @Test
    fun `minimum and maximum sizes limit sizes and flexing`() {
        lateinit var limited: Div
        lateinit var capped: Div
        lateinit var other: Div
        lateinit var floor: Div
        layout(body {
            limited = div({ width = 300.px; maxWidth = 50.percent; height = 10.px; minHeight = 30.px })
            row({ width = 300.px; height = 10.px }) {
                capped = div({ width = 0.px; height = 10.px; flexGrow = FlexGrow.Ratio(1.0); maxWidth = 50.px })
                other = div({ width = 0.px; height = 10.px; flexGrow = FlexGrow.Ratio(1.0) })
            }
            row({ width = 100.px; height = 10.px }) {
                floor = div({ width = 100.px; height = 10.px; minWidth = 80.px })
                div({ width = 100.px; height = 10.px })
            }
        })
        assertBounds(limited, 0.0, 0.0, 200.0, 30.0)
        // the capped item is frozen at its maximum, the other item takes the rest
        assertEquals(50.0, capped.layout.screenWidth, epsilon)
        assertEquals(250.0, other.layout.screenWidth, epsilon)
        // the item that can not shrink below 80 leaves the rest of the shrinking to the other item
        assertEquals(80.0, floor.layout.screenWidth, epsilon)
    }

    @Test
    fun `borders and percentage padding are part of the box`() {
        lateinit var parent: Div
        lateinit var child: Div
        layout(body {
            div({ width = 200.px; height = 100.px }) {
                parent = div({ width = LinearDimension.Auto; height = LinearDimension.Auto; borderWidth = 2.px; paddingLeft = 10.percent; paddingTop = 5.px }) {
                    child = div({ width = 50.px; height = 20.px })
                }
            }
        })
        assertEquals(20.0, parent.layout.paddingLeft, epsilon)
        assertEquals(2.0, parent.layout.borderWidth, epsilon)
        assertBounds(child, 22.0, 7.0, 50.0, 20.0)
        assertBounds(parent, 0.0, 0.0, 2.0 + 20.0 + 50.0 + 2.0, 2.0 + 5.0 + 20.0 + 2.0)
        assertEquals(child.layout.screenX - parent.layout.screenX, parent.layout.contentBoundsAtOrigin.x, epsilon)
    }

    @Test
    fun `grid places items around items with a fixed place`() {
        lateinit var fixed: Div
        val auto = mutableListOf<Div>()
        layout(body {
            div({
                display = Display.GRID; width = 300.px; height = 100.px
                gridTemplateColumns = GridTemplate.LengthList(List(3) { 100.px })
                gridTemplateRows = GridTemplate.LengthList(listOf(50.px))
            }) {
                fixed = div({ gridColumn = GridPopulation.Position(1); gridRow = GridPopulation.Position(0) })
                repeat(3) { auto.add(div()) }
                auto.add(div({ gridColumn = GridPopulation.Length(5) }))
            }
        })
        assertEquals(100.0, fixed.layout.screenX, epsilon)
        // the auto placed items skip the occupied cell
        assertEquals(listOf(0.0 to 0.0, 200.0 to 0.0, 0.0 to 50.0), auto.take(3).map { it.layout.screenX to it.layout.screenY })
        // a span wider than the grid is limited to the grid, on a row of its own
        assertBounds(auto[3], 0.0, 100.0, 300.0, 50.0)
    }

    @Test
    fun `grid sizes auto tracks to their items and takes gaps of the content`() {
        lateinit var grid: Div
        lateinit var wide: Div
        lateinit var tall: Div
        lateinit var last: Div
        layout(body {
            grid = div({
                display = Display.GRID; width = 220.px; height = LinearDimension.Auto; padding(10.px)
                columnGap = 10.percent
                gridTemplateColumns = GridTemplate.LengthList(listOf(LinearDimension.Auto, LinearDimension.Fraction(1.0)))
                gridTemplateRows = GridTemplate.LengthList(listOf(LinearDimension.Auto))
            }) {
                wide = div({ width = 60.px; height = 10.px })
                tall = div({ height = 40.px })
                last = div({ height = 25.px })
            }
        })
        // 10% of the 200 content width
        assertEquals(60.0 + 20.0 + 10.0, tall.layout.screenX, epsilon)
        assertEquals(200.0 - 60.0 - 20.0, tall.layout.screenWidth, epsilon)
        // auto rows take the height of their tallest item, also beyond the template
        assertEquals(10.0 + 40.0, last.layout.screenY, epsilon)
        assertEquals(10.0 + 40.0 + 25.0 + 10.0, grid.layout.screenHeight, epsilon)
    }
}
