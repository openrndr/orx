package org.openrndr.panel.elements

import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.events.Event
import org.openrndr.extra.textwriter.Cursor
import org.openrndr.extra.textwriter.writer
import org.openrndr.panel.binding.Binding0
import org.openrndr.panel.binding.Binding1
import org.openrndr.panel.style.Color
import org.openrndr.panel.style.borderColor
import org.openrndr.panel.style.color
import org.openrndr.panel.style.effectiveBackground
import org.openrndr.shape.Rectangle
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KMutableProperty1

class TextInput : Element(ElementType("text-input")) {

    private var glyphRectangles: MutableList<Pair<Rectangle, Rectangle>> = mutableListOf()
    private var ivalue: String = ""

    var value: String
        set(value) {
            if (ivalue != value) {
                ivalue = value
                inputIndex = value.length - 1
                requestRedraw()
            }
        }
        get() = ivalue


    private var inputIndex = value.length - 1

    class ValueChangedEvent(val source: TextInput, val oldValue: String, val newValue: String)
    class Events : AutoCloseable {
        val valueChanged = Event<ValueChangedEvent>("text-input-value-changed")
        override fun close() {
            valueChanged.close()
        }

    }

    val events = Events()

    init {
        keyboard.pressed.listen {
            if (KeyModifier.CTRL in it.modifiers || KeyModifier.SUPER in it.modifiers) {
                if (it.name == "v") {
                    val oldValue = value
                    (root() as Body).controlManager?.program?.clipboard?.contents?.let {
                        ivalue += it
                    }
                    events.valueChanged.trigger(ValueChangedEvent(this, oldValue, value))
                    it.cancelPropagation()
                }
            }
            when (it.key) {
                KEY_HOME -> inputIndex = -1
                KEY_END -> inputIndex = value.length - 1
                KEY_ARROW_LEFT -> inputIndex = max(-1, inputIndex - 1)
                KEY_ARROW_RIGHT -> inputIndex = min(value.length - 1, inputIndex + 1)

                KEY_DELETE -> {
                    if (value.isNotEmpty()) {
                        val oldValue = value

                        if (inputIndex == -1) {
                            ivalue = value.drop(1)
                        } else if (inputIndex < value.length - 1) {
                            ivalue = value.take(inputIndex + 1) + value.drop(inputIndex + 2)
                        }
                        inputIndex = min(inputIndex, value.length - 1)
                        events.valueChanged.trigger(ValueChangedEvent(this, oldValue, value))
                    }
                }

                KEY_BACKSPACE -> {
                    if (value.isNotEmpty()) {
                        val oldValue = value

                        if (inputIndex == value.length - 1) {
                            ivalue = value.dropLast(1)
                        } else if (inputIndex > -1) {
                            ivalue = value.take(inputIndex) + value.drop(inputIndex + 1)
                        }
                        inputIndex = max(-1, inputIndex - 1)
                        events.valueChanged.trigger(ValueChangedEvent(this, oldValue, value))
                    }
                }
            }
            requestRedraw()
            it.cancelPropagation()
        }

        keyboard.character.listen {
            it.cancelPropagation()
            val oldValue = value
            ivalue = value.take(inputIndex + 1) + it.character.toString() + value.drop(inputIndex + 1)
            inputIndex++
            events.valueChanged.trigger(ValueChangedEvent(this, oldValue, value))
            requestRedraw()
        }

        mouse.pressed.listen {
            it.cancelPropagation()
        }
        mouse.clicked.listen {
            it.cancelPropagation()
        }
    }

    override fun draw(drawer: Drawer) {
        drawer.fill = computedStyle.effectiveBackground
        drawer.stroke = ((computedStyle.borderColor as? Color.RGBa)?.color ?: ColorRGBa.TRANSPARENT)

        drawer.rectangle(layout.boundsAtOrigin)

        (root() as? Body)?.controlManager?.fontManager?.let {
            val font = it.font(computedStyle)
            val textHeight = font.ascenderLength
            val yOffset = ((layout.screenHeight / 2) + textHeight / 2.0 - 2.0).round(0)

            //drawer.rectangle(layout.contentBoundsAtOrigin)

            drawer.drawStyle.clip = layout.contentBounds
            drawer.fontMap = font
            drawer.fill = ((computedStyle.color as? Color.RGBa)?.color ?: ColorRGBa.WHITE)

            val xOffset = layout.contentBoundsAtOrigin.x
            var caretX: Double? = null
            writer(drawer) {
                cursor = Cursor(xOffset, yOffset)
                text(value, visible = false)
                glyphRectangles = glyphOutput.rectangles

                var scroll = 2.0
                if (ElementPseudoClass("active") in pseudoClasses) {
                    caretX = if (glyphRectangles.isNotEmpty()) {
                        if (inputIndex == glyphRectangles.lastIndex)
                            glyphRectangles.last().second.position(1.0, 0.0).x
                        else
                            glyphRectangles[inputIndex + 1].second.position(0.0, 0.0).x
                    } else xOffset
                    // Calculate scroll value when the caret is outside the text box
                    val layoutMaxX = layout.contentBounds.position(1.0, 0.0).x
                    val rightPadding = textWidth("m") * 2
                    if (caretX > layoutMaxX - rightPadding) {
                        scroll = layoutMaxX - rightPadding - caretX
                    }
                    caretX += scroll
                }

                cursor = Cursor(xOffset + scroll, yOffset)
                text(value)
            }

            caretX?.let { x ->
                val y = yOffset - font.descenderLength
                drawer.stroke = ColorRGBa.WHITE
                drawer.lineSegment(x, y, x, y - textHeight)
            }

            drawer.drawStyle.clip = null
        }
    }

    override fun close() {
        super.close()
        events.close()
    }
}

fun TextInput.bind(property: KMutableProperty0<String>, program: Program? = null) {
    val program = program ?: (root() as? Body)?.controlManager?.program
    Binding0(
        program ?: error("no program"),
        this,
        this.events.valueChanged,
        property,
        { it.newValue },
        { value = it })
}

fun TextInput.bind(container: Any, property: KMutableProperty1<Any, String>, program: Program? = null) {
    val program = program ?: (root() as? Body)?.controlManager?.program
    Binding1(
        program ?: error("no program"),
        this,
        this.events.valueChanged,
        container,
        property,
        { it.newValue },
        { value = it })
}