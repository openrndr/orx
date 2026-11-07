package org.openrndr.panel.elements

import org.openrndr.color.ColorRGBa

import kotlinx.coroutines.DelicateCoroutinesApi
import org.openrndr.*
import org.openrndr.draw.Drawer
import org.openrndr.draw.LineCap
import org.openrndr.events.Event
import org.openrndr.extra.expressions.ExpressionException
import org.openrndr.extra.expressions.evaluateExpression
import org.openrndr.math.Vector2
import org.openrndr.panel.binding.Binding0
import org.openrndr.panel.binding.Binding1
import org.openrndr.panel.style.Color
import org.openrndr.panel.style.color
import org.openrndr.panel.style.effectiveColor
import org.openrndr.shape.Rectangle
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KMutableProperty1

@JvmRecord
data class Range(val min: Double, val max: Double) {
    val span: Double get() = max - min
}

enum class SliderMode {
    RANGE,
    POINT,
    SEGMENT
}

class Slider : Element(ElementType("slider")) {

    override val handlesKeyboardFocus = true

    var label = ""
    var precision = 3
    var mode = SliderMode.RANGE

    var value: Double
        set(v) {
            val oldV = realValue
            realValue = clean(v)
            if (realValue != oldV) {
                draw.dirty = true
                events.valueChanged.trigger(ValueChangedEvent(this, false, oldV, realValue))
            }
        }
        get() = realValue

    private var interactiveValue: Double
        set(v) {
            val oldV = realValue
            realValue = clean(v)
            if (realValue != oldV) {
                draw.dirty = true
                events.valueChanged.trigger(ValueChangedEvent(this, true, oldV, realValue))
            }
        }
        get() = realValue


    var range = Range(0.0, 10.0)
        set(value) {
            field = value
            this.value = this.value
        }
    private var realValue = 0.0

    fun clean(value: Double): Double {
        val cleanV = value.coerceIn(range.min, range.max)
        val quantized = String.format("%.0${precision}f", cleanV).replace(",", ".").toDouble()
        return quantized
    }

    class ValueChangedEvent(
        val source: Slider,
        val interactive: Boolean,
        val oldValue: Double,
        val newValue: Double
    )

    class Events : AutoCloseable {
        val valueChanged = Event<ValueChangedEvent>("slider-value-changed")
        override fun close() {
            valueChanged.close()
        }
    }

    val events = Events()

    private val margin = 7.0
    private var keyboardInput = ""
    private var inputIndex = -1

    init {
        mouse.pressed.listen {
            val t = (it.position.x - layout.screenX - margin) / (layout.screenWidth - 2.0 * margin)
            interactiveValue = t * range.span + range.min
            it.cancelPropagation()
        }
        mouse.clicked.listen {
            val t = (it.position.x - layout.screenX - margin) / (layout.screenWidth - 2.0 * margin)
            interactiveValue = t * range.span + range.min
            it.cancelPropagation()
        }
        mouse.dragged.listen {
            val t = (it.position.x - layout.screenX - margin) / (layout.screenWidth - 2.0 * margin)
            interactiveValue = t * range.span + range.min
            it.cancelPropagation()
        }

        mouse.scrolled.listen {
            if (Math.abs(it.rotation.y) < 0.001) {
                interactiveValue += range.span * 0.001 * it.rotation.x
                it.cancelPropagation()
            }
        }

        keyboard.focusLost.listen {
            keyboardInput = ""
            inputIndex = -1
            draw.dirty = true
        }

        keyboard.character.listen {
            val candidate =
                keyboardInput.take(inputIndex + 1) + it.character.toString() + keyboardInput.drop(inputIndex + 1)

            keyboardInput = candidate
            inputIndex = inputIndex + 1
            requestRedraw()
            it.cancelPropagation()
        }


        keyboard.repeated.listen {
            val delta = Math.pow(10.0, -(precision - 0.0))
            if (it.key == KEY_ARROW_RIGHT) {
                interactiveValue += delta
                it.cancelPropagation()
            }

            if (it.key == KEY_ARROW_LEFT) {
                if (keyboardInput.isEmpty()) {
                    interactiveValue -= delta
                }
                it.cancelPropagation()
            }

        }
        keyboard.pressed.listen {
            val delta = Math.pow(10.0, -(precision - 0.0))

            if (it.key == KEY_ARROW_RIGHT) {
                if (keyboardInput.isEmpty()) {
                    interactiveValue += delta

                } else {
                    inputIndex = min(keyboardInput.length - 1, inputIndex + 1)
                    requestRedraw()
                }
                it.cancelPropagation()
            }

            if (it.key == KEY_ARROW_LEFT) {
                if (keyboardInput.isEmpty()) {
                    interactiveValue -= delta
                    it.cancelPropagation()
                } else {
                    inputIndex = max(-1, inputIndex - 1)
                    requestRedraw()
                }
            }

            if (it.key == KEY_BACKSPACE) {
                if (!keyboardInput.isEmpty()) {
                    if (inputIndex > 1) {
                        keyboardInput = keyboardInput.substring(0, inputIndex - 1) + keyboardInput.substring(inputIndex)
                    } else {
                        keyboardInput = keyboardInput.drop(1)
                    }
                    inputIndex = max(-1, inputIndex - 1)
                    draw.dirty = true
                }
                it.cancelPropagation()
            }

            if (it.key == KEY_ESCAPE) {
                keyboardInput = ""
                inputIndex = -1
                draw.dirty = true
                it.cancelPropagation()
            }

            if (it.key == KEY_ENTER) {
                try {
                    val number = evaluateExpression(keyboardInput, constants = mapOf("_" to value)) ?: value
                    interactiveValue = number.coerceIn(range.min, range.max)
                } catch (_: ExpressionException) {
                    // -- silently (but safely) ignore the exception
                }
                keyboardInput = ""
                inputIndex = -1
                draw.dirty = true
                it.cancelPropagation()
            }

            if (it.key == KEY_HOME) {
                interactiveValue = range.min
                keyboardInput = ""
                it.cancelPropagation()
            }

            if (it.key == KEY_END) {
                interactiveValue = range.max
                keyboardInput = ""
                it.cancelPropagation()
            }
        }
    }

    override fun draw(drawer: Drawer) {
        val text = panelText ?: return
        val face = text.face(computedStyle)
        drawer.translate(0.0, (layout.screenHeight - (10.0 + face.ascent + face.descent)) / 2)

        drawer.fill = ((computedStyle.color as Color.RGBa).color)
        drawer.stroke = ((computedStyle.color as Color.RGBa).color)
        drawer.strokeWeight = (8.0)
        drawer.lineCap = (LineCap.ROUND)
        val x = ((value - range.min) / range.span) * (layout.screenWidth - 2 * margin)

        drawer.stroke = ((computedStyle.color as Color.RGBa).color.opacify(0.25))
        drawer.lineSegment(margin + 0.0, 2.0, margin + layout.screenWidth - 2 * margin, 2.0)

        if (mode == SliderMode.RANGE) {
            drawer.stroke = ((computedStyle.color as Color.RGBa).color.opacify(1.0))
            drawer.lineSegment(margin, 2.0, margin + x, 2.0)

            drawer.fill = ((computedStyle.color as Color.RGBa).color.opacify(1.0))
            drawer.stroke = null
            drawer.strokeWeight = 0.0
            drawer.circle(margin + x, 2.0, 5.0)
        }

        if (mode == SliderMode.POINT && precision == 0) {
            val lineSegments = mutableListOf<Vector2>()
            for (i in range.min.toInt()..range.max.toInt()) {
                val lx = ((i - range.min) / range.span) * (layout.screenWidth - 2 * margin)
                drawer.strokeWeight = 1.0
                drawer.stroke = ((computedStyle.color as Color.RGBa).color.opacify(0.5))
                lineSegments.add(Vector2(margin + lx, -2.0))
                lineSegments.add(Vector2(margin + lx, 4.0))
            }
            drawer.lineSegments(lineSegments)
        }

        if (mode == SliderMode.SEGMENT) {
            drawer.stroke = ((computedStyle.color as Color.RGBa).color.opacify(1.0))

            val sx = ((value - range.min) / (range.span + 1.0)) * (layout.screenWidth - 2 * margin) + margin
            val ex = (((value + 1) - range.min) / (range.span + 1.0)) * (layout.screenWidth - 2 * margin) + margin

            drawer.strokeWeight = 8.0
            drawer.lineSegment(sx, 2.0, ex, 2.0)

            drawer.stroke = null
            drawer.strokeWeight = 0.0

            val lineSegments = mutableListOf<Vector2>()
            for (i in range.min.toInt()..(range.max.toInt() + 1)) {
                val lx = ((i - range.min) / (range.span + 1.0)) * (layout.screenWidth - 2 * margin)
                drawer.strokeWeight = 1.0
                drawer.stroke = ((computedStyle.color as Color.RGBa).color.opacify(0.5))
                lineSegments.add(Vector2(margin + lx, -2.0))
                lineSegments.add(Vector2(margin + lx, 4.0))
            }
            drawer.lineSegments(lineSegments)
        }


        if (mode == SliderMode.POINT) {
            drawer.fill = ((computedStyle.color as Color.RGBa).color.opacify(1.0))
            drawer.stroke = null
            drawer.circle(margin + x, 2.0, 8.0)
        }


        // the label at the left and the value at the right, a line below the track
        val color = computedStyle.effectiveColor ?: ColorRGBa.WHITE
        val baseline = 8.0 + face.ascent - face.descent + face.lineGap
        text.drawLine(drawer, this, "label", computedStyle, label, 0.0, baseline, color)
        val shown = keyboardInput.ifEmpty { String.format("%.0${precision}f", value) }
        val x0 = layout.screenWidth - text.width(computedStyle, shown)
        text.drawLine(drawer, this, "value", computedStyle, shown, x0, baseline, color)
        if (keyboardInput.isNotEmpty()) {
            // the caret after the character at the input index, before the first when it is -1
            val carets = text.carets(computedStyle, keyboardInput)
            val x = x0 + carets[(inputIndex + 1).coerceIn(0, keyboardInput.length)]
            drawer.stroke = color.opacify(0.5)
            drawer.strokeWeight = 1.0
            drawer.lineSegment(x, baseline, x, baseline - (face.ascent + face.descent))
        }
    }

    override fun close() {
        super.close()
        events.close()
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun Slider.bind(
    property: KMutableProperty0<Double>,
    program: Program? = null
): Binding0<Slider.ValueChangedEvent, Double> {
    val program = program ?: (root() as? Body)?.controlManager?.program ?: error("no program")
    return Binding0(program, this, this.events.valueChanged, property, { it.newValue }, { value = it })
}

@JvmName("bindInt")
fun Slider.bind(property: KMutableProperty0<Int>, program: Program? = null): Binding0<Slider.ValueChangedEvent, Int> {
    val program = program ?: (root() as? Body)?.controlManager?.program
    return Binding0(
        program ?: error("no program"),
        this,
        this.events.valueChanged,
        property,
        { it.newValue.toInt() },
        { value = it.toDouble() })
}

@OptIn(DelicateCoroutinesApi::class)
fun Slider.bind(
    container: Any,
    property: KMutableProperty1<Any, Double>,
    program: Program? = null
): Binding1<Slider.ValueChangedEvent, Double> {
    val program = program ?: (root() as? Body)?.controlManager?.program
    return Binding1(
        program ?: error("no program"),
        this,
        this.events.valueChanged,
        container,
        property,
        { it.newValue },
        { value = it })
}

@JvmName("bindInt")
@OptIn(DelicateCoroutinesApi::class)
fun Slider.bind(
    container: Any,
    property: KMutableProperty1<Any, Int>,
    program: Program? = null
): Binding1<Slider.ValueChangedEvent, Int> {
    val program = program ?: (root() as? Body)?.controlManager?.program
    return Binding1(
        program ?: error("no program"),
        this,
        this.events.valueChanged,
        container,
        property,
        { it.newValue.toInt() },
        { value = it.toDouble() })
}