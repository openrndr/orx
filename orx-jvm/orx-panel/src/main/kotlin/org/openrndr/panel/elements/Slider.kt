package org.openrndr.panel.elements

import kotlinx.coroutines.*
import mu.KotlinLogging
import org.openrndr.*
import org.openrndr.draw.Cursor
import org.openrndr.draw.Drawer
import org.openrndr.draw.LineCap
import org.openrndr.draw.Writer
import org.openrndr.events.Event
import org.openrndr.math.Vector2
import org.openrndr.panel.style.Color
import org.openrndr.panel.style.color
import org.openrndr.panel.style.effectiveColor
import org.openrndr.shape.Rectangle
import java.text.NumberFormat
import java.text.ParseException
import kotlin.reflect.KMutableProperty0

private val logger = KotlinLogging.logger {}

data class Range(val min: Double, val max: Double) {
    val span: Double get() = max - min
}

enum class SliderMode {
    RANGE,
    POINT,
    SEGMENT
}

class Slider : Element(ElementType("slider")), DisposableElement {
    override var disposed = false
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

    class ValueChangedEvent(val source: Slider,
                            val interactive: Boolean,
                            val oldValue: Double,
                            val newValue: Double)

    class Events {
        val valueChanged = Event<ValueChangedEvent>("slider-value-changed")
    }

    val events = Events()

    private val margin = 7.0
    private var keyboardInput = ""

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
            draw.dirty = true
        }

        keyboard.character.listen {
            if (it.character in setOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.', ',', '-')) {
                try {
                    val candidate = keyboardInput + it.character.toString()
                    if (candidate.length > 1) {
                        NumberFormat.getInstance().parse(candidate).toDouble()
                    }
                    keyboardInput = candidate
                    requestRedraw()
                } catch (e: ParseException) {
                }
            }
            it.cancelPropagation()
        }


        keyboard.repeated.listen {
            val delta = Math.pow(10.0, -(precision - 0.0))
            if (it.key == KEY_ARROW_RIGHT) {
                interactiveValue += delta
                it.cancelPropagation()
            }

            if (it.key == KEY_ARROW_LEFT) {
                interactiveValue -= delta
                it.cancelPropagation()
            }

        }
        keyboard.pressed.listen {
            val delta = Math.pow(10.0, -(precision - 0.0))

            if (it.key == KEY_ARROW_RIGHT) {
                interactiveValue += delta
                it.cancelPropagation()
            }

            if (it.key == KEY_ARROW_LEFT) {
                interactiveValue -= delta
                it.cancelPropagation()
            }

            if (it.key == KEY_BACKSPACE) {
                if (!keyboardInput.isEmpty()) {
                    keyboardInput = keyboardInput.substring(0, keyboardInput.length - 1)
                    draw.dirty = true
                }
                it.cancelPropagation()
            }

            if (it.key == KEY_ESCAPE) {
                keyboardInput = ""
                draw.dirty = true
                it.cancelPropagation()
            }

            if (it.key == KEY_ENTER) {
                try {
                    val number = NumberFormat.getInstance().parse(keyboardInput).toDouble()
                    interactiveValue = number.coerceIn(range.min, range.max)
                } catch (e: ParseException) {
                    // -- silently (but safely) ignore the exception
                }
                keyboardInput = ""
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
        val f = (root() as? Body)?.controlManager?.fontManager?.font(computedStyle)!!
        drawer.translate(0.0, (layout.screenHeight - (10.0 + f.height)) / 2)

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

            val sx = ((value - range.min) / (range.span+1.0)) * (layout.screenWidth - 2 * margin) + margin
            val ex = (((value+1) - range.min) / (range.span+1.0)) * (layout.screenWidth - 2 * margin) + margin

            drawer.strokeWeight = 8.0
            drawer.lineSegment(sx, 2.0, ex, 2.0)

            drawer.stroke = null
            drawer.strokeWeight = 0.0


            val lineSegments = mutableListOf<Vector2>()
            for (i in range.min.toInt()..(range.max.toInt()+1)) {
                val lx = ((i - range.min) / (range.span+1.0)) * (layout.screenWidth - 2 * margin)
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


        (root() as? Body)?.controlManager?.fontManager?.let {
            val font = it.font(computedStyle)
            val writer = Writer(drawer)
            drawer.fontMap = (font)
            drawer.fill = computedStyle.effectiveColor
            writer.cursor = Cursor(0.0, 8.0)
            writer.box = Rectangle(0.0, 8.0, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)
            writer.newLine()
            writer.text(label)

            if (keyboardInput.isEmpty()) {
                val valueFormatted = String.format("%.0${precision}f", value)
                val tw = writer.textWidth(valueFormatted)
                writer.cursor.x = (layout.screenWidth - tw)
                writer.text(valueFormatted)
            } else {
                val tw = writer.textWidth(keyboardInput)
                writer.cursor.x = (layout.screenWidth - tw)
                writer.text(keyboardInput)
            }
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun Slider.bind(property: KMutableProperty0<Double>) {
    var currentValue: Double? = null

    events.valueChanged.listen {
        currentValue = it.newValue
        property.set(it.newValue)
    }
    GlobalScope.launch {
        while(!disposed) {
            val body = (root() as? Body)
            if (body != null) {
                fun update() {
                    if (property.get() != currentValue) {
                        val lcur = property.get()
                        currentValue = lcur
                        value = lcur.toDouble()
                    }
                }
                update()
                body.controlManager.program.launch {
                    while (!disposed) {
                        update()
                        yield()
                    }
                }
                break
            }
            yield()
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
@JvmName("bindInt")
fun Slider.bind(property: KMutableProperty0<Int>) {
    var currentValue: Int? = null
    events.valueChanged.listen {
        currentValue = it.newValue.toInt()
        property.set(it.newValue.toInt())
    }
    GlobalScope.launch {
        while(!disposed) {
            val body = (root() as? Body)
            if (body != null) {
                fun update() {
                    if (property.get() != currentValue) {
                        val lcur = property.get()
                        currentValue = lcur
                        value = lcur.toDouble()
                    }
                }
                update()
                body.controlManager.program.launch {
                    while (!disposed) {
                        update()
                        yield()
                    }
                }
                break
            }
            yield()
        }
    }
}
