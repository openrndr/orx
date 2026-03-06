package org.openrndr.panel.elements

import org.openrndr.KEY_ENTER
import org.openrndr.KEY_ESCAPE
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.color.Linearity
import org.openrndr.color.rgb
import org.openrndr.draw.Drawer
import org.openrndr.draw.LineCap

import org.openrndr.events.Event
import org.openrndr.extra.textwriter.TextWriter
import org.openrndr.panel.binding.Binding0
import org.openrndr.panel.binding.Binding1
import org.openrndr.panel.style.*

import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KMutableProperty1

class ColorpickerButton : Element(ElementType("colorpicker-button")) {

    var label: String = "OK"
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

    init {
        mouse.pressed.listen {
            it.cancelPropagation()
        }
        mouse.clicked.listen {
            replace {
                slideOut(0.0, screenArea.height, screenArea.width, 200.0) {

                    val cp = colorpicker {
                        style {
                            padding(length { 0.px })
                            margins(length { 0.px })
                        }
                        bind(this@ColorpickerButton::color)
                    }
                    textInput {
                        value = ""
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
                                value = ""
                            }
                            if (it.key == KEY_ENTER) {
                                try {
                                    this@ColorpickerButton.color = rgb(value)
                                } catch (e: IllegalArgumentException) {
                                    value = ""
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
                }
            }
            it.cancelPropagation()
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
        drawer.rectangle(0.0, 0.0, screenArea.width, screenArea.height)

        (root() as? Body)?.controlManager?.fontManager?.let {
            val font = it.font(computedStyle)

            val writer = TextWriter(drawer)
            drawer.fontMap = (font)

            val text = "$label"

            val textWidth = writer.textWidth(text)
            val textHeight = font.ascenderLength

            val offset = Math.round((layout.screenWidth - textWidth) / 2.0)
            val yOffset = Math.round((layout.screenHeight / 2) + textHeight / 2.0) - 2.0

            drawer.fill = (computedStyle.color as? Color.RGBa)?.color ?: ColorRGBa.WHITE
            drawer.fontMap = font
            drawer.text(text, 0.0 + offset, 0.0 + yOffset)
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