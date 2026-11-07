package org.openrndr.panel.elements

import org.openrndr.shape.Rectangle

import org.openrndr.KEY_ENTER
import org.openrndr.KEY_ESCAPE
import org.openrndr.KEY_SPACEBAR
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.color.Linearity
import org.openrndr.color.rgb
import org.openrndr.draw.Drawer
import org.openrndr.draw.LineCap

import org.openrndr.events.Event
import org.openrndr.panel.binding.Binding0
import org.openrndr.panel.binding.Binding1
import org.openrndr.panel.style.*

import kotlin.math.roundToInt
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KMutableProperty1

class ColorpickerButton : Element(ElementType("colorpicker-button")) {

    /** the label, the attribute `label` */
    var label: String by attribute("label", "OK")
    var color: ColorRGBa = ColorRGBa(0.5, 0.5, 0.5, linearity = Linearity.SRGB)
        set(value) {
            if (value != field) {
                field = value
                requestRedraw()
                events.valueChanged.trigger(ColorChangedEvent(this, value))
            }
        }

    class ColorChangedEvent(val source: ColorpickerButton, val color: ColorRGBa)

    class Events : AutoCloseable {
        val valueChanged = Event<ColorChangedEvent>()
        override fun close() {
            valueChanged.close()
        }
    }

    val events = Events()

    override val handlesKeyboardFocus = true

    /** true when the color picker is open */
    val isOpen: Boolean get() = children.any { it is SlideOut }

    /** the color when the picker opened, it is recent when the picker closes with another color */
    private var colorAtOpen: ColorRGBa? = null

    /** the hex field of the open picker */
    private var hexField: TextInput? = null

    /** closes the color picker, the button takes the keys again */
    fun closePicker() {
        children.filterIsInstance<SlideOut>().forEach { it.dispose() }
        colorAtOpen?.let { if (it != color) addRecentColor(color) }
        colorAtOpen = null
        hexField = null
        (root() as? Body)?.controlManager?.keyboardInput?.requestFocus(this)
        requestRedraw()
    }

    /** opens the color picker below the button, or above it when there is no space below; it takes the keys */
    fun openPicker() {
        if (isOpen) return
        // a picker that closed by itself, when the pointer left it, makes its color recent now
        colorAtOpen?.let { if (it != color) addRecentColor(color) }
        colorAtOpen = color
        replace {
            // it closes half a second after the cursor moved out of it
            slideOut(0.0, screenArea.height, screenArea.width, 200.0, aboveY = 0.0, closeDelayMillis = 500L) {
                val cp = colorpicker {
                    style {
                        padding(length { 0.px })
                        margins(length { 0.px })
                    }
                    color = this@ColorpickerButton.color.copy(alpha = 1.0)
                    // the picker sets hue, saturation and value, the alpha is kept
                    events.colorChanged.listen {
                        this@ColorpickerButton.color = it.newColor.copy(alpha = this@ColorpickerButton.color.alpha)
                    }
                    keyboard.pressed.listen {
                        if (it.key == KEY_ESCAPE || it.key == KEY_ENTER) {
                            it.cancelPropagation()
                            closePicker()
                        }
                    }
                }
                // the color as hex, enter takes a typed color
                val hex = textInput {
                    value = this@ColorpickerButton.color.toHexString()
                    style {
                        width = LinearDimension.Percent(100.0)
                        height = length { 32.px }
                        padding(length { 0.px })
                        paddingLeft = length { 5.px }
                        paddingRight = length { 5.px }
                        margins(length { 0.px })
                    }
                    keyboard.pressed.listen {
                        if (it.key == KEY_ESCAPE) {
                            value = this@ColorpickerButton.color.toHexString()
                        }
                        if (it.key == KEY_ENTER) {
                            try {
                                val typed = rgb(value.trim())
                                this@ColorpickerButton.color = typed.copy(alpha = this@ColorpickerButton.color.alpha)
                                cp.color = typed.copy(alpha = 1.0)
                            } catch (e: IllegalArgumentException) {
                                value = this@ColorpickerButton.color.toHexString()
                            }
                        }
                    }
                }
                slider {
                    style {
                        width = LinearDimension.Percent(100.0)
                        padding(length { 0.px })
                        margins(length { 0.px })
                    }
                    label = "saturation"
                    range = Range(0.0, 1.0)
                    bind(cp::saturation)
                }
                slider {
                    style {
                        width = LinearDimension.Percent(100.0)
                        padding(length { 0.px })
                        margins(length { 0.px })
                    }
                    label = "alpha"
                    range = Range(0.0, 1.0)
                    value = this@ColorpickerButton.color.alpha
                    events.valueChanged.listen {
                        this@ColorpickerButton.color = this@ColorpickerButton.color.copy(alpha = it.newValue)
                    }
                }
                // the colors picked before, a click picks one again
                if (recentColors.isNotEmpty()) {
                    div("recent-colors") {
                        style {
                            display = Display.FLEX
                            flexDirection = FlexDirection.Row
                            columnGap = length { 4.px }
                            height = LinearDimension.Auto
                        }
                        for (recent in recentColors) {
                            div("recent-color") {
                                style {
                                    width = length { 18.px }
                                    height = length { 18.px }
                                    borderRadius = length { 3.px }
                                    background = Color.RGBa(recent)
                                }
                                tooltip = recent.toHexString()
                                mouse.clicked.listen {
                                    this@ColorpickerButton.color = recent
                                    cp.color = recent.copy(alpha = 1.0)
                                    it.cancelPropagation()
                                }
                            }
                        }
                    }
                }
                hexField = hex
                (root() as? Body)?.controlManager?.keyboardInput?.requestFocus(cp)
            }
        }
        requestRedraw()
    }

    init {
        // the hex field of the open picker follows the color, unless it is typed in
        events.valueChanged.listen {
            hexField?.let { hex ->
                if (hex.parent != null && ElementPseudoClass("focus") !in hex.pseudoClasses) hex.value = it.color.toHexString()
            }
        }
        mouse.pressed.listen {
            it.cancelPropagation()
        }
        mouse.clicked.listen {
            // a click while the color picker is open closes it
            if (isOpen) closePicker() else openPicker()
            it.cancelPropagation()
        }
        keyboard.pressed.listen {
            when (it.key) {
                KEY_ENTER, KEY_SPACEBAR -> if (isOpen) closePicker() else openPicker()
                KEY_ESCAPE -> if (isOpen) closePicker() else return@listen
                else -> return@listen
            }
            it.cancelPropagation()
        }
    }

    companion object {
        /** the colors picked before, the most recent first, shared by all color picker buttons */
        val recentColors = mutableListOf<ColorRGBa>()

        /** the most recent colors that are kept */
        var recentColorCount = 8

        fun addRecentColor(color: ColorRGBa) {
            recentColors.remove(color)
            recentColors.add(0, color)
            while (recentColors.size > recentColorCount) recentColors.removeAt(recentColors.lastIndex)
        }
    }

    override fun append(element: Element) {
        when (element) {
            is Item, is SlideOut -> super.append(element)
            else -> throw RuntimeException("only item and slideout")
        }
        super.append(element)
    }

    override fun close() {
        super.close()
        events.close()
    }

    override fun draw(drawer: Drawer) {

        drawer.fill = ((computedStyle.background as? Color.RGBa)?.color ?: ColorRGBa.PINK)
        drawer.stroke = null
        drawer.strokeWeight = 0.0
        drawer.box(Rectangle(0.0, 0.0, screenArea.width, screenArea.height), cornerRadius)

        panelText?.let { text ->
            val face = text.face(computedStyle)
            val offset = Math.round((layout.screenWidth - text.width(computedStyle, label)) / 2.0).toDouble()
            val baseline = Math.round((layout.screenHeight / 2) + face.ascent / 2.0) - 2.0
            text.drawLine(drawer, this, "label", computedStyle, label, offset, baseline,
                (computedStyle.color as? Color.RGBa)?.color ?: ColorRGBa.WHITE)
            drawer.stroke = color
            drawer.pushStyle()
            drawer.strokeWeight = 4.0
            drawer.lineCap = LineCap.ROUND
            drawer.lineSegment(2.0, layout.screenHeight - 2.0, layout.screenWidth - 2.0, layout.screenHeight - 2.0)
            drawer.popStyle()
        }
    }
}

fun ColorpickerButton.bind(property: KMutableProperty0<ColorRGBa>, program: Program? = null) {
    val program = program ?: (root() as? Body)?.controlManager?.program
    Binding0(program ?: error("no program"), this, this.events.valueChanged, property, { it.color }, { color = it })
}

fun ColorpickerButton.bind(container: Any, property: KMutableProperty1<Any, ColorRGBa>, program: Program? = null) {
    val program = program ?: (root() as? Body)?.controlManager?.program
    Binding1(
        program ?: error("no program"),
        this,
        this.events.valueChanged,
        container,
        property,
        { it.color },
        { color = it })
}

/** [this] as `#rrggbb` in sRGB */
fun ColorRGBa.toHexString(): String {
    val c = toSRGB()
    fun hex(v: Double) = (v.coerceIn(0.0, 1.0) * 255.0).roundToInt().toString(16).padStart(2, '0')
    return "#" + hex(c.r) + hex(c.g) + hex(c.b)
}
