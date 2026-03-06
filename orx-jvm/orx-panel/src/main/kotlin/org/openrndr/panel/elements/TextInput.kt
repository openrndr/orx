package org.openrndr.panel.elements

import org.openrndr.KEY_ARROW_LEFT
import org.openrndr.KEY_ARROW_RIGHT
import org.openrndr.KEY_BACKSPACE
import org.openrndr.KEY_END
import org.openrndr.KEY_HOME
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.LineCap
import org.openrndr.panel.style.*
import org.openrndr.KeyModifier
import org.openrndr.Program
import org.openrndr.events.Event
import org.openrndr.extra.textwriter.Cursor
import org.openrndr.extra.textwriter.writer
import org.openrndr.math.Vector2
import org.openrndr.panel.binding.Binding0
import org.openrndr.panel.binding.Binding1
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


    private var inputIndex = value.length-1
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
            if (it.key == KEY_HOME) {
                inputIndex = -1
            }

            if (it.key == KEY_END) {
                inputIndex = value.length-1
            }

            if (it.key == KEY_ARROW_LEFT) {
                inputIndex = max(-1, inputIndex - 1)
            }

            if (it.key == KEY_ARROW_RIGHT) {
                inputIndex = min(value.length-1, inputIndex + 1)
            }

            if (it.key == KEY_BACKSPACE) {
                if (value.isNotEmpty()) {
                    val oldValue = value

                    if (inputIndex == value.length-1) {
                        ivalue = value.dropLast(1)
                    }
else
                    if (inputIndex > -1) {
                        ivalue = value.take(inputIndex) + value.drop(inputIndex+1)
                    }
                    inputIndex = max(-1, inputIndex - 1)
                    events.valueChanged.trigger(ValueChangedEvent(this, oldValue, value))
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
        drawer.stroke= ((computedStyle.borderColor as? Color.RGBa)?.color ?: ColorRGBa.TRANSPARENT)

        drawer.rectangle(layout.boundsAtOrigin)

        (root() as? Body)?.controlManager?.fontManager?.let {
            val font = it.font(computedStyle)

            drawer.fontMap = (font)
            val textHeight = font.ascenderLength


            val yOffset = Math.round((layout.screenHeight / 2) + textHeight / 2.0 - 2.0) * 1.0


            //drawer.rectangle(layout.contentBoundsAtOrigin)

            drawer.drawStyle.clip = layout.contentBounds

            drawer.fill = ((computedStyle.color as? Color.RGBa)?.color ?: ColorRGBa.WHITE)

            var cursorX = 0.0
            val xOffset = layout.contentBoundsAtOrigin.x
            writer(drawer) {
                val emWidth = textWidth("m") * 0
                cursor = Cursor(xOffset, yOffset)
                text(value, visible = false)
//                val caretPosition = if (glyphRectangles.isEmpty() || inputIndex == -1) 0.0 else if (inputIndex < value.length-1) (glyphRectangles[inputIndex-1].second.position(1.0, 0.0).x)
//                else cursor.x
                val width = cursor.x - xOffset
                val scroll =
                    if (width > screenArea.width - emWidth) {
                        screenArea.width - emWidth - width
                    } else {
                        0.0
                    }
                cursor = Cursor(  xOffset + scroll, yOffset)
                text(value)
                cursorX = cursor.x
                glyphRectangles = glyphOutput.rectangles
            }

            if (ElementPseudoClass("active") in pseudoClasses) {
                drawer.stroke = ColorRGBa.WHITE

                if (glyphRectangles.isNotEmpty()) {
                    if (inputIndex >= 0) {
                        val last = glyphRectangles[inputIndex]
                        drawer.lineSegment(last.second.position(1.0, 1.0), last.second.position(1.0, 1.0 - Vector2(0.0, textHeight).length))
                    } else {
                        val last = glyphRectangles.first()
                        drawer.lineSegment(last.second.position(0.0, 1.0), last.second.position(0.0, 1.0 - Vector2(0.0, textHeight).length))
                    }
                }
                else {
                    drawer.lineSegment(cursorX + 1.0, yOffset, cursorX + 1.0, yOffset - textHeight)
                }
            }
            drawer.drawStyle.clip = null

            drawer.stroke = ((computedStyle.color as? Color.RGBa)?.color ?: ColorRGBa.WHITE)
            drawer.strokeWeight = 1.0

            drawer.stroke = computedStyle.effectiveColor?.shade(0.25)
            drawer.lineCap = LineCap.ROUND
        }
    }

    override fun close() {
        super.close()
        events.close()
    }
}

fun TextInput.bind(property: KMutableProperty0<String>, program: Program? = null) {
    val program = program ?: (root() as? Body)?.controlManager?.program
    Binding0(program ?: error("no program"), this, this.events.valueChanged, property, { it.newValue }, { value = it })
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