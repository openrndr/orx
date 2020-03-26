package org.openrndr.panel.elements

import kotlinx.coroutines.*
import org.openrndr.KeyModifier
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.LineCap
import org.openrndr.draw.isolated
import org.openrndr.events.Event
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.panel.style.Color
import org.openrndr.panel.style.color
import org.openrndr.panel.style.effectiveColor
import org.openrndr.panel.tools.Tooltip
import org.openrndr.shape.Rectangle
import org.openrndr.text.Cursor
import org.openrndr.text.Writer
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt

class SequenceEditor : Element(ElementType("sequence-editor")) {

    var value = mutableListOf(0.0)
    var label = "sequence"
    var precision = 2
    var maximumSequenceLength = 16
    var minimumSequenceLength = 1
    var range: ClosedRange<Double> = -1.0..1.0

    private var selectedIndex: Int? = null
    private var tooltip: Tooltip? = null

    private val footerHeight = 20.0

    class ValueChangedEvent(val source: SequenceEditor,
                            val oldValue: List<Double>,
                            val newValue: List<Double>)

    class Events {
        val valueChanged = Event<ValueChangedEvent>("sequence-editor-value-changed")
    }

    val events = Events()

    init {
        fun query(position: Vector2): Vector2 {
            val x = (position.x - layout.screenX) / layout.screenWidth
            val y = 1.0 - ((position.y - layout.screenY) / ((layout.screenHeight - footerHeight) * 0.5))
            return Vector2(x, y)
        }

        mouse.clicked.listen {
            it.cancelPropagation()
            requestRedraw()
        }
        mouse.pressed.listen {
            if (value.isNotEmpty()) {
                val dx = (layout.screenWidth / (value.size + 1))
                val index = (it.position.x - layout.screenX) / dx

                val d = index - round(index)
                val dp = d * dx
                val dpa = abs(dp)

                if (dpa < 10.0) {
                    selectedIndex = if (KeyModifier.CTRL !in it.modifiers) {
                        round(index).toInt()
                    } else {
                        if (value.size > minimumSequenceLength) {
                            val oldValue = value.map { it }
                            value.removeAt(round(index).toInt() - 1)
                            events.valueChanged.trigger(ValueChangedEvent(this, oldValue, value))
                        }
                        null
                    }
                } else {
                    if (KeyModifier.CTRL !in it.modifiers) {
                        if (value.size < maximumSequenceLength) {
                            val q = query(it.position)
                            val oldValue = value.map { it }
                            value.add(index.toInt(), q.y.map(-1.0, 1.0, range.start, range.endInclusive))
                            events.valueChanged.trigger(ValueChangedEvent(this, oldValue, value))
                        }
                    }
                }
            }
            it.cancelPropagation()
        }

        var hoverJob: Job? = null

        mouse.exited.listen {
            hoverJob?.cancel()
            if (tooltip != null) {
                tooltip = null
                requestRedraw()
            }
        }

        mouse.moved.listen {
            hoverJob?.let { job ->
                job.cancel()
            }
            if (tooltip != null) {
                tooltip = null
                requestRedraw()
            }

            if (value.isNotEmpty()) {
                val dx = (layout.screenWidth / (value.size + 1))
                val index = (it.position.x - layout.screenX) / dx
                val d = index - round(index)
                val dp = d * dx
                val dpa = abs(dp)

                if (dpa < 10.0) {
                    hoverJob = GlobalScope.launch {
                        val readIndex = index.roundToInt() - 1
                        if (readIndex >= 0 && readIndex < value.size) {
                            val value = String.format("%.0${precision}f", value[readIndex])
                            tooltip = Tooltip(this@SequenceEditor, it.position - Vector2(layout.screenX, layout.screenY), "$value")
                            requestRedraw()
                        }
                    }
                }
            }
        }
        mouse.dragged.listen {
            val q = query(it.position)
            selectedIndex?.let { index ->
                val writeIndex = index - 1
                if (writeIndex >= 0 && writeIndex < value.size) {
                    val oldValue = value.map { it }
                    value[writeIndex] = q.y.coerceIn(-1.0, 1.0).map(-1.0, 1.0, range.start, range.endInclusive)
                    events.valueChanged.trigger(ValueChangedEvent(this, oldValue, value))
                }
                requestRedraw()
            }
        }
    }

    override fun draw(drawer: Drawer) {
        val controlArea = Rectangle(0.0, 0.0, layout.screenWidth, layout.screenHeight - footerHeight)

        drawer.stroke = computedStyle.effectiveColor?.opacify(0.25)
        drawer.strokeWeight = (1.0)


        val zeroHeight = 0.0.map(range.start, range.endInclusive, -1.0, 1.0).coerceIn(-1.0, 1.0) * controlArea.height / -2.0
        drawer.lineSegment(0.0, controlArea.height / 2.0 + zeroHeight, layout.screenWidth, controlArea.height / 2.0 + zeroHeight)

        drawer.strokeWeight = 7.0
        drawer.fill = computedStyle.effectiveColor

        for (i in value.indices) {
            val dx = layout.screenWidth / (value.size + 1)
            val height = -value[i].map(range.start, range.endInclusive, -1.0, 1.0).coerceIn(-1.0, 1.0) * controlArea.height / 2.0

            val x = dx * (i + 1)
            drawer.lineCap = LineCap.ROUND
            drawer.stroke = computedStyle.effectiveColor
            drawer.lineSegment(x, controlArea.height / 2.0 + zeroHeight, x, controlArea.height / 2.0 + height)

            drawer.stroke = computedStyle.effectiveColor?.shade(1.1)
            drawer.fill = ColorRGBa.PINK
            drawer.circle(x, controlArea.height / 2.0 + height, 7.0)
        }

        drawer.isolated {
            drawer.translate(0.0, controlArea.height)
            drawer.fill = computedStyle.effectiveColor
            (root() as? Body)?.controlManager?.fontManager?.let {
                val font = it.font(computedStyle)
                val writer = Writer(drawer)
                drawer.fontMap = (font)
                drawer.fill = computedStyle.effectiveColor
                writer.cursor = Cursor(0.0, 4.0)
                writer.box = Rectangle(0.0, 4.0, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)
                writer.newLine()
                writer.text(label)
            }
        }

        tooltip?.draw(drawer)
    }
}
