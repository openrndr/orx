package org.openrndr.panel.elements

import kotlinx.coroutines.*
import org.openrndr.KeyModifier
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.events.Event
import org.openrndr.extra.textwriter.Cursor
import org.openrndr.extra.textwriter.TextWriter
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.panel.style.effectiveColor
import org.openrndr.panel.tools.Tooltip
import org.openrndr.shape.Rectangle
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt

class SequenceEditor : SequenceEditorBase("sequence-editor") {
    var value
        get() = baseValue
        set(value) {
            baseValue = value
        }

    public override var maximumSequenceLength = 16
    public override var minimumSequenceLength = 1

    class ValueChangedEvent(val source: SequenceEditorBase,
                            val oldValue: List<Double>,
                            val newValue: List<Double>)

    class Events {
        val valueChanged = Event<ValueChangedEvent>("sequence-editor-value-changed")
    }

    val events = Events()

    init {
        baseEvents.valueChanged.listen {
            events.valueChanged.trigger(ValueChangedEvent(this, it.oldValue, it.newValue))
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
open class SequenceEditorBase(type: String = "sequence-editor-base") : Element(ElementType(type)), DisposableElement {
    override var disposed = false

    internal var baseValue = mutableListOf(0.0)
    var label = "sequence"
    var precision = 2
    internal open var maximumSequenceLength = 16
    internal open var minimumSequenceLength = 1
    var range: ClosedRange<Double> = -1.0..1.0

    private var selectedIndex: Int? = null
    private var tooltip: Tooltip? = null

    private val footerHeight = 20.0

    internal class ValueChangedEvent(val source: SequenceEditorBase,
        val oldValue: List<Double>,
        val newValue: List<Double>)

    internal class Events {
        val valueChanged = Event<ValueChangedEvent>("sequence-editor-base-value-changed")
    }

    internal val baseEvents = Events()

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
            if (baseValue.isNotEmpty()) {
                val dx = (layout.screenWidth / (baseValue.size + 1))
                val index = (it.position.x - layout.screenX) / dx

                val d = index - round(index)
                val dp = d * dx
                val dpa = abs(dp)

                if (dpa < 10.0) {
                    selectedIndex = if (KeyModifier.CTRL !in it.modifiers) {
                        round(index).toInt()
                    } else {
                        if (baseValue.size > minimumSequenceLength) {
                            val oldValue = baseValue.map { it }
                            baseValue.removeAt(round(index).toInt() - 1)
                            baseEvents.valueChanged.trigger(ValueChangedEvent(this, oldValue, baseValue))
                        }
                        null
                    }
                } else {
                    if (KeyModifier.CTRL !in it.modifiers) {
                        if (baseValue.size < maximumSequenceLength) {
                            val q = query(it.position)
                            val oldValue = baseValue.map { it }
                            baseValue.add(index.toInt(), q.y.map(-1.0, 1.0, range.start, range.endInclusive))
                            baseEvents.valueChanged.trigger(ValueChangedEvent(this, oldValue, baseValue))
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

            if (baseValue.isNotEmpty()) {
                val dx = (layout.screenWidth / (baseValue.size + 1))
                val index = (it.position.x - layout.screenX) / dx
                val d = index - round(index)
                val dp = d * dx
                val dpa = abs(dp)

                if (dpa < 10.0) {
                    hoverJob = GlobalScope.launch {
                        val readIndex = index.roundToInt() - 1
                        if (readIndex >= 0 && readIndex < baseValue.size) {
                            val value = String.format("%.0${precision}f", baseValue[readIndex])
                            tooltip = Tooltip(this@SequenceEditorBase, it.position - Vector2(layout.screenX, layout.screenY), "$value")
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
                if (writeIndex >= 0 && writeIndex < baseValue.size) {
                    val oldValue = baseValue.map { it }
                    baseValue[writeIndex] = q.y.coerceIn(-1.0, 1.0).map(-1.0, 1.0, range.start, range.endInclusive)
                    baseEvents.valueChanged.trigger(ValueChangedEvent(this, oldValue, baseValue))
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

        for (i in baseValue.indices) {
            val dx = layout.screenWidth / (baseValue.size + 1)
            val height = -baseValue[i].map(range.start, range.endInclusive, -1.0, 1.0).coerceIn(-1.0, 1.0) * controlArea.height / 2.0

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
                val writer = TextWriter(drawer)
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
