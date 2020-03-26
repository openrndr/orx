package org.openrndr.panel.elements

import org.openrndr.KEY_BACKSPACE
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.LineCap
import org.openrndr.panel.style.*
import kotlinx.coroutines.yield
import org.openrndr.KeyModifier
import org.openrndr.events.Event
import org.openrndr.launch
import org.openrndr.shape.Rectangle
import org.openrndr.text.Cursor
import org.openrndr.text.writer
import kotlin.reflect.KMutableProperty0

class Textfield : Element(ElementType("textfield")) {

    var value: String = ""
    var label: String = "label"

    class ValueChangedEvent(val source: Textfield, val oldValue: String, val newValue: String)
    class Events {
        val valueChanged = Event<ValueChangedEvent>("textfield-value-changed")
    }

    val events = Events()

    init {
        keyboard.repeated.listen {
            if (it.key == KEY_BACKSPACE) {
                if (value.isNotEmpty()) {
                    val oldValue = value
                    value = value.substring(0, value.length - 1)
                    events.valueChanged.trigger(ValueChangedEvent(this, oldValue, value))
                    requestRedraw()
                }

            }
            it.cancelPropagation()
        }

        keyboard.pressed.listen {
            if (KeyModifier.CTRL in it.modifiers || KeyModifier.SUPER in it.modifiers) {
                if (it.name == "v") {
                    val oldValue = value
                    (root() as Body).controlManager.program.clipboard.contents?.let {
                        value += it

                    }
                    events.valueChanged.trigger(ValueChangedEvent(this, oldValue, value))
                    it.cancelPropagation()
                }
            }
            if (it.key == KEY_BACKSPACE) {
                if (value.isNotEmpty()) {
                    val oldValue = value
                    value = value.substring(0, value.length - 1)
                    events.valueChanged.trigger(ValueChangedEvent(this, oldValue, value))
                }
            }
            requestRedraw()
            it.cancelPropagation()
        }

        keyboard.character.listen {
            val oldValue = value
            value += it.character
            events.valueChanged.trigger(ValueChangedEvent(this, oldValue, value))
            it.cancelPropagation()
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
        drawer.stroke = null
        drawer.rectangle(0.0, 0.0, layout.screenWidth, layout.screenHeight)

        (root() as? Body)?.controlManager?.fontManager?.let {
            val font = it.font(computedStyle)

            drawer.fontMap = (font)
            val textHeight = font.ascenderLength

            val offset = 5.0
            val yOffset = Math.round((layout.screenHeight / 2) + textHeight / 2.0 - 2.0) * 1.0

            drawer.fill = ((computedStyle.color as? Color.RGBa)?.color ?: ColorRGBa.WHITE)
            drawer.text("$label", 0.0 + offset, 0.0 + yOffset - textHeight * 1.5)

            drawer.fill = (((computedStyle.color as? Color.RGBa)?.color ?: ColorRGBa.WHITE).opacify(0.05))
            drawer.rectangle(0.0 + offset, 0.0 + yOffset - (textHeight + 2), layout.screenWidth - 10.0, textHeight + 8.0)

            drawer.drawStyle.clip = Rectangle(screenPosition.x + offset, screenPosition.y + yOffset - (textHeight + 2), layout.screenWidth - 10.0, textHeight + 8.0)

            drawer.fill = ((computedStyle.color as? Color.RGBa)?.color ?: ColorRGBa.WHITE)

            var cursorX = 0.0
            writer(drawer) {
                val emWidth = textWidth("m") * 2
                cursor = Cursor(offset, yOffset)
                text(value, visible = false)
                val width = cursor.x - offset
                val scroll =
                        if (width > screenArea.width - emWidth) {
                            screenArea.width - emWidth - width
                        } else {
                            0.0
                        }
                cursor = Cursor(offset + scroll, yOffset)
                text(value)
                cursorX = cursor.x
            }

            if (ElementPseudoClass("active") in pseudoClasses) {
                drawer.stroke = ColorRGBa.WHITE
                drawer.lineSegment(cursorX + 1.0, yOffset, cursorX + 1.0, yOffset - textHeight)
            }
            drawer.drawStyle.clip = null

            drawer.stroke = ((computedStyle.color as? Color.RGBa)?.color ?: ColorRGBa.WHITE)
            drawer.strokeWeight = 1.0

            drawer.stroke = computedStyle.effectiveColor?.shade(0.25)
            drawer.lineCap = LineCap.ROUND
            //drawer.lineSegment(0.0, yOffset + 4.0, layout.screenWidth, yOffset + 4.0)
        }
    }
}

fun Textfield.bind(property: KMutableProperty0<String>) {
    var currentValue = property.get()

    events.valueChanged.listen {
        currentValue = it.newValue
        property.set(it.newValue)
    }

    (root() as Body).controlManager.program.launch {
        while (true) {
            val cval = property.get()
            if (cval != currentValue) {
                currentValue = cval
                value = cval
            }
            yield()
        }
    }
}