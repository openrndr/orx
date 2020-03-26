package org.openrndr.panel.elements

import kotlinx.coroutines.*
import org.openrndr.KeyModifier
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.LineCap
import org.openrndr.events.Event
import org.openrndr.math.Vector2
import org.openrndr.panel.tools.Tooltip
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt

class SequenceEditor : Element(ElementType("sequence-editor")) {

    var value = mutableListOf(0.0)
    var precision = 2
    var maximumSequenceLength = 16
    var minimumSequenceLength = 1

    private var selectedIndex: Int? = null
    private var tooltip: Tooltip? = null

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
            val y = 1.0 - ((position.y - layout.screenY) / (layout.screenHeight * 0.5))
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
                            value.add(index.toInt(), q.y)
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
                            tooltip = Tooltip(this@SequenceEditor, it.position - Vector2(layout.screenX, layout.screenY), "index: ${index.roundToInt()}, $value")
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
                    value[writeIndex] = q.y.coerceIn(-1.0, 1.0)
                    events.valueChanged.trigger(ValueChangedEvent(this, oldValue, value))
                }
                requestRedraw()
            }
        }
    }

    override fun draw(drawer: Drawer) {
        drawer.stroke = (ColorRGBa.BLACK.opacify(0.25))
        drawer.strokeWeight = (1.0)
        drawer.lineSegment(0.0, layout.screenHeight / 2.0, layout.screenWidth, layout.screenHeight / 2.0)

        drawer.strokeWeight = 1.0
        drawer.stroke = ColorRGBa.WHITE
        for (i in value.indices) {
            val dx = layout.screenWidth / (value.size + 1)
            val height = -value[i] * layout.screenHeight / 2.0

            val x = dx * (i + 1)
            drawer.lineCap = LineCap.ROUND
            drawer.lineSegment(x, layout.screenHeight / 2.0, x, layout.screenHeight / 2.0 + height)
            drawer.circle(x, layout.screenHeight / 2.0 + height, 5.0)
        }
        tooltip?.draw(drawer)
    }
}
