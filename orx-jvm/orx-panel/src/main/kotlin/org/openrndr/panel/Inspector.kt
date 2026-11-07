package org.openrndr.panel

import org.openrndr.ApplicationWindow
import org.openrndr.WindowConfiguration
import org.openrndr.color.ColorRGBa
import org.openrndr.math.IntVector2
import org.openrndr.panel.document.body
import org.openrndr.panel.document.document
import org.openrndr.panel.elements.ElementType
import org.openrndr.panel.elements.div
import org.openrndr.panel.elements.requestRedraw
import org.openrndr.window
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.extra.style.CssKeyword
import org.openrndr.extra.style.TimingFunction
import org.openrndr.extra.style.toCssColor
import org.openrndr.panel.elements.Element
import org.openrndr.panel.elements.TextNode
import org.openrndr.panel.layout.Layouter
import org.openrndr.panel.style.*
import org.openrndr.shape.Rectangle
import kotlin.math.roundToInt

/**
 * The inspector of a control manager, like the inspector of the developer tools of browsers: it shows the element under
 * the pointer with its margin, border, padding and content, and a panel with its path, its box, the style sheets that
 * select it in the order of the cascade, with the declarations that other declarations override, and its computed
 * style. The panel is shown in a window of its own, see [inWindow]. F12 turns it on and off, shift-F12 pins the
 * element, so that the pointer can move away from it.
 */
class Inspector internal constructor(private val controlManager: ControlManager) {
    /** true when the inspector is shown */
    var enabled = false

    /** the element that is inspected while the inspector is pinned, null when it follows the pointer */
    var pinned: Element? = null

    /** the element that is inspected, the pinned element or the element under the pointer */
    val inspected: Element?
        get() = pinned ?: controlManager.body?.let { body -> elementAt(body, controlManager.mousePosition) { it !is TextNode } }

    private val style = StyleSheet().apply { fontSize = 12.px }
    private val headingColor = ColorRGBa.fromHex("#7fd1b9")
    private val textColor = ColorRGBa.WHITE.shade(0.85)
    private val dimColor = ColorRGBa.WHITE.shade(0.45)

    /** a line of the panel, in [color], indented by [indent] */
    internal data class Line(val text: String, val color: ColorRGBa, val indent: Double = 0.0, val strike: Boolean = false)

    /** the lines of the panel for [element] */
    internal fun lines(element: Element, layouter: Layouter?): List<Line> {
        val lines = mutableListOf<Line>()
        lines.add(Line(path(element), headingColor))
        val cs = element.computedStyle
        val area = element.screenArea
        lines.add(Line("${format(area.x)}, ${format(area.y)}  ${format(area.width)} × ${format(area.height)}", textColor))
        fun sides(t: Any?, r: Any?, b: Any?, l: Any?) = listOf(t, r, b, l).joinToString(" ") { cssText(it) }
        lines.add(Line("margin ${sides(cs.marginTop, cs.marginRight, cs.marginBottom, cs.marginLeft)}", dimColor))
        lines.add(Line("padding ${sides(cs.paddingTop, cs.paddingRight, cs.paddingBottom, cs.paddingLeft)}", dimColor))
        lines.add(Line("border ${cssText(cs.borderWidth)}", dimColor))

        // the rules that select the element, the last wins, then the inline style
        lines.add(Line("", textColor))
        lines.add(Line("rules", headingColor))
        val sheets = (layouter?.matchingStyleSheets(element) ?: emptyList()).map { it.cssSelector.toString() + "  (${it.specificity.ids},${it.specificity.classes},${it.specificity.types})" to it } +
                listOfNotNull(element.style?.let { "style" to it })
        // the declaration that wins for a property: the last important one, or the last one
        val winners = HashMap<String, StyleSheet>()
        for ((_, sheet) in sheets) for ((name, property) in sheet.properties) {
            val current = winners[name]?.properties?.get(name)
            if (current == null || property.important || !current.important) winners[name] = sheet
        }
        for ((title, sheet) in sheets.reversed()) {
            if (sheet.properties.isEmpty()) continue
            lines.add(Line(title, textColor))
            // a shorthand with var() is shown once, as it is written; it is overridden when all its longhands are
            val shown = mutableSetOf<org.openrndr.extra.style.CssDeclaration>()
            for ((name, property) in sheet.properties.toSortedMap()) {
                val variable = property.value as? VariableValue
                if (variable != null && variable.longhand != variable.declaration.name.trim().lowercase()) {
                    if (!shown.add(variable.declaration)) continue
                    val longhands = sheet.properties.filterValues { (it.value as? VariableValue)?.declaration === variable.declaration }.keys
                    val overridden = longhands.all { winners[it] !== sheet }
                    lines.add(Line("${variable.declaration.name}: ${variable.declaration.value}${if (property.important) " !important" else ""}",
                        if (overridden) dimColor else textColor, 12.0, overridden))
                    continue
                }
                val overridden = winners[name] !== sheet
                lines.add(Line("$name: ${cssText(name, property.value)}${if (property.important) " !important" else ""}",
                    if (overridden) dimColor else textColor, 12.0, overridden))
            }
        }

        // the computed style: the properties that are not their initial values, and the custom properties
        lines.add(Line("", textColor))
        lines.add(Line("computed", headingColor))
        val properties = panelProperties()
        for ((name, property) in cs.properties.toSortedMap()) {
            if (name.startsWith("--")) continue
            val initial = properties[name]?.initial
            if (initial != null && sameValue(initial, property.value)) continue
            lines.add(Line("$name: ${cssText(name, property.value)}", textColor, 12.0))
        }
        val variables = cs.properties.keys.filter { it.startsWith("--") }.sorted()
        if (variables.isNotEmpty()) {
            lines.add(Line("custom properties", dimColor))
            for (name in variables) lines.add(Line("$name: ${cs.properties[name]?.value}", dimColor, 12.0))
        }
        return lines
    }

    /** the path of [element] from the body, like `body > div.sidebar > slider#radius:hover` */
    internal fun path(element: Element): String =
        generateSequence(element) { it.parent }.toList().reversed().joinToString(" > ") { e ->
            e.type.name.lowercase() + (e.id?.let { "#$it" } ?: "") + e.classes.joinToString("") { ".${it.name}" } +
                    e.pseudoClasses.joinToString("") { ":${it.name}" }
        }

    private fun format(v: Double): String = if (v == v.roundToInt().toDouble()) v.roundToInt().toString() else "%.1f".format(v)

    /** [value] of the property [name] as CSS text, as far as it can be */
    internal fun cssText(name: String, value: Any?): String =
        if (value is Double && name.startsWith("transition-") && !name.endsWith("function")) "${format(value)}s" else cssText(value)

    /** [value] as CSS text, as far as it can be */
    internal fun cssText(value: Any?): String = when (value) {
        null -> "none"
        is CssKeyword -> value.css
        is VariableValue -> value.declaration.value
        is LinearDimension.PX -> "${format(value.value)}px"
        is LinearDimension.Percent -> "${format(value.value)}%"
        is LinearDimension.Fraction -> "${format(value.value)}fr"
        is LinearDimension.MinMax -> "minmax(${cssText(value.min)}, ${cssText(value.max)})"
        is LinearDimension.Auto -> "auto"
        is Color.RGBa -> value.color.toHexOrCss()
        is ColorRGBa -> value.toHexOrCss()
        is ZIndex.Value -> value.value.toString()
        is FlexGrow.Ratio -> format(value.value)
        is TextAlign.Value -> format(value.value)
        is GridTemplate.LengthList -> value.value.joinToString(" ") { cssText(it) }
        is GridPopulation.Position -> "${value.value + 1}"
        is GridPopulation.Length -> "span ${value.value}"
        is GridPopulation.Span -> "${value.value.first + 1} / ${value.value.last + 2}"
        is Double -> format(value)
        is List<*> -> value.joinToString(", ") { cssText(it) }
        is TimingFunction -> when (value) {
            TimingFunction.LINEAR -> "linear"; TimingFunction.EASE -> "ease"; TimingFunction.EASE_IN -> "ease-in"
            TimingFunction.EASE_OUT -> "ease-out"; TimingFunction.EASE_IN_OUT -> "ease-in-out"; else -> "cubic-bezier(…)"
        }
        is Enum<*> -> value.name.lowercase()
        is PropertyValue -> value::class.simpleName?.lowercase() ?: value.toString()
        else -> value.toString()
    }

    private fun ColorRGBa.toHexOrCss(): String {
        val c = toSRGB()
        if (c.alpha <= 0.0) return "transparent"
        if (c.alpha < 1.0) return toCssColor()
        fun hex(v: Double) = (v.coerceIn(0.0, 1.0) * 255.0).roundToInt().toString(16).padStart(2, '0')
        return "#${hex(c.r)}${hex(c.g)}${hex(c.b)}"
    }

    /** draws the highlight of the inspected element and the panel, over the panel, in the coordinates of the window */
    internal fun draw(drawer: Drawer, text: PanelText, layouter: Layouter?, screen: Rectangle) {
        if (!enabled) return
        val element = inspected ?: return
        val cs = element.computedStyle
        fun px(v: LinearDimension) = (v as? LinearDimension.PX)?.value ?: 0.0
        val border = element.screenArea
        val bw = px(cs.borderWidth)
        val margin = Rectangle(border.x - px(cs.marginLeft), border.y - px(cs.marginTop),
            border.width + px(cs.marginLeft) + px(cs.marginRight), border.height + px(cs.marginTop) + px(cs.marginBottom))
        val padding = Rectangle(border.x + bw, border.y + bw, border.width - 2 * bw, border.height - 2 * bw)
        val content = Rectangle(padding.x + px(cs.paddingLeft), padding.y + px(cs.paddingTop),
            padding.width - px(cs.paddingLeft) - px(cs.paddingRight), padding.height - px(cs.paddingTop) - px(cs.paddingBottom))
        drawer.isolated {
            drawer.defaults()
            drawer.ortho(org.openrndr.draw.RenderTarget.active)
            drawer.stroke = null
            // the regions of the box, like the colors of the developer tools of browsers
            drawer.fill = ColorRGBa.fromHex("#f6b26b").opacify(0.35); drawer.rectangle(margin)
            drawer.fill = ColorRGBa.fromHex("#ffe599").opacify(0.45); drawer.rectangle(border)
            drawer.fill = ColorRGBa.fromHex("#93c47d").opacify(0.45); drawer.rectangle(padding)
            drawer.fill = ColorRGBa.fromHex("#6fa8dc").opacify(0.5); drawer.rectangle(content)
            drawer.fill = null
            drawer.stroke = if (pinned != null) ColorRGBa.fromHex("#ff6b6b") else ColorRGBa.WHITE.opacify(0.8)
            drawer.strokeWeight = 1.0
            drawer.rectangle(border)

            if (window != null) return@isolated
            // without a window of its own, the panel is drawn at the side of the window that the element is not on
            val all = lines(element, layouter)
            val lineHeight = lineHeight(text)
            val width = minOf(420.0, screen.width * 0.5)
            val maxLines = ((screen.height - 24.0) / lineHeight).toInt().coerceAtLeast(1)
            val shown = if (all.size <= maxLines) all else all.take(maxLines - 1) + Line("… ${all.size - maxLines + 1} more", dimColor)
            val x = if (border.center.x > screen.center.x) screen.x + 8.0 else screen.x + screen.width - width - 8.0
            val y = screen.y + 8.0
            drawer.fill = background
            drawer.stroke = ColorRGBa.WHITE.opacify(0.2)
            drawer.rectangle(x, y, width, shown.size * lineHeight + 12.0)
            drawLines(drawer, text, this@Inspector, shown, x + 8.0, y + 6.0, width - 16.0)
        }
        // the window shows the lines of the inspected element
        if (window != null) windowLines = lines(element, layouter)
    }

    private val background = ColorRGBa.fromHex("#16181c").opacify(0.94)

    /** the height of a line of the panel */
    internal fun lineHeight(text: PanelText): Double = text.face(style).let { (it.ascent - it.descent) * 1.35 }

    /**
     * Draws [lines] from ([x], [y]), the top of the first line, cut at [width], with the text drawer [text]; the draw
     * commands are kept for [owner]
     */
    internal fun drawLines(drawer: Drawer, text: PanelText, owner: Any, lines: List<Line>, x: Double, y: Double, width: Double) {
        val face = text.face(style)
        val lineHeight = lineHeight(text)
        drawer.stroke = null
        for ((i, line) in lines.withIndex()) {
            if (line.text.isEmpty()) continue
            // lines that do not fit are cut
            var t = line.text
            val available = width - line.indent
            if (text.width(style, t) > available) {
                while (t.isNotEmpty() && text.width(style, "$t…") > available) t = t.dropLast(1)
                t = "$t…"
            }
            val baseline = y + face.ascent + i * lineHeight
            text.drawLine(drawer, owner, "line $i", style, t, x + line.indent, baseline, line.color)
            if (line.strike) {
                drawer.stroke = line.color
                drawer.lineSegment(x + line.indent, baseline - face.ascent * 0.35, x + line.indent + text.width(style, t), baseline - face.ascent * 0.35)
                drawer.stroke = null
            }
        }
    }

    //<editor-fold desc="the window of the inspector">
    /** true when the inspector shows its panel in a window of its own, false to draw it over the window */
    var inWindow = true

    /** the window of the inspector, null when it is not open */
    private var window: ApplicationWindow? = null

    /** the lines that the window shows */
    @Volatile
    internal var windowLines: List<Line> = emptyList()

    /** shows the inspector, in a window of its own when [inWindow] is set */
    fun show() {
        enabled = true
        if (inWindow && window == null) openWindow()
    }

    /** hides the inspector and closes its window */
    fun hide() {
        enabled = false
        pinned = null
        val w = window
        window = null
        windowLines = emptyList()
        w?.close()
    }

    private fun openWindow() {
        val program = controlManager.program
        val position = program.window.position.toInt() + IntVector2(program.width + 12, 0)
        window = try {
            program.window(
                WindowConfiguration(
                    title = "Inspector", width = 440, height = maxOf(program.height, 480), position = position,
                    resizable = true, utilityWindow = true
                )
            ) {
                val view = InspectorView(this@Inspector)
                val cm = ControlManager()
                cm.document = document {
                    styleSheets.addAll(defaultStyles() + styleSheets(windowCss))
                    body {
                        div("inspector") { append(view) }
                    }
                }
                // the lines of the inspected element, they are drawn again when they change
                extend {
                    val lines = windowLines
                    if (lines != view.lines) {
                        view.lines = lines
                        view.requestRedraw()
                    }
                }
                extend(cm)
                this.window.closed.listen {
                    if (this@Inspector.window != null) {
                        this@Inspector.window = null
                        enabled = false
                        pinned = null
                    }
                }
            }
        } catch (e: Throwable) {
            // without child windows, the panel is drawn over the window
            null
        }
    }

    private val windowCss = """
        .inspector {
            width: 100%; height: 100%; overflow: scroll; overflow-x: hidden; padding: 8px;
            background-color: ${background.opacify(1.0).toCssColor()};
        }
        inspector-view { width: 100% }
    """
    //</editor-fold>
}

/** the lines of the inspector in its window, as tall as they are */
internal class InspectorView(private val inspector: Inspector) : Element(ElementType("inspector-view")) {
    var lines: List<Inspector.Line> = emptyList()

    override val heightHint: Double
        get() = lines.size * (panelText?.let { inspector.lineHeight(it) } ?: 16.0)

    override fun draw(drawer: Drawer) {
        val text = panelText ?: return
        inspector.drawLines(drawer, text, this, lines, 0.0, 0.0, layout.screenWidth)
    }
}
