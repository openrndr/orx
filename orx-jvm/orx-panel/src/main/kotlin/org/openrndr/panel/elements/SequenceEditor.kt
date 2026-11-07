package org.openrndr.panel.elements

import kotlinx.coroutines.*
import org.openrndr.KEY_ARROW_DOWN
import org.openrndr.KEY_ARROW_LEFT
import org.openrndr.KEY_ARROW_RIGHT
import org.openrndr.KEY_ARROW_UP
import org.openrndr.KeyModifier
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.events.Event
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.panel.binding.Binding0
import org.openrndr.panel.binding.Binding1
import org.openrndr.panel.style.effectiveColor
import org.openrndr.panel.tools.Tooltip
import org.openrndr.shape.Rectangle
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KMutableProperty1

class SequenceEditor : SequenceEditorBase("sequence-editor") {
    var value
        get() = baseValue
        set(value) {
            baseValue = value
        }

    public override var maximumSequenceLength = 16
    public override var minimumSequenceLength = 1

    class ValueChangedEvent(
        val source: SequenceEditorBase,
        val oldValue: List<Double>,
        val newValue: List<Double>
    )

    class Events : AutoCloseable {
        val valueChanged = Event<ValueChangedEvent>("sequence-editor-value-changed")
        override fun close() {
            valueChanged.close()
        }
    }

    val events = Events()

    init {
        baseEvents.valueChanged.listen {
            events.valueChanged.trigger(ValueChangedEvent(this, it.oldValue, it.newValue))
        }
    }

    override fun close() {
        super.close()
        events.close()
    }
}

@OptIn(DelicateCoroutinesApi::class)
open class SequenceEditorBase(type: String = "sequence-editor-base") : Element(ElementType(type)) {

    internal var baseValue = mutableListOf(0.0)
    var label = "sequence"
    var precision = 2
    internal open var maximumSequenceLength = 16
    internal open var minimumSequenceLength = 1
    var range: ClosedRange<Double> = -1.0..1.0

    override val handlesKeyboardFocus = true

    /** the value that is dragged or changed by the keyboard, counted from 1 */
    private var selectedIndex: Int? = null
    private var valueTooltip: Tooltip? = null

    private val footerHeight = 20.0

    internal class ValueChangedEvent(
        val source: SequenceEditorBase,
        val oldValue: List<Double>,
        val newValue: List<Double>
    )

    internal class Events : AutoCloseable {
        val valueChanged = Event<ValueChangedEvent>("sequence-editor-base-value-changed")
        override fun close() {
            valueChanged.close()
        }
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
            if (valueTooltip != null) {
                valueTooltip = null
                requestRedraw()
            }
        }

        mouse.moved.listen {
            hoverJob?.let { job ->
                job.cancel()
            }
            if (valueTooltip != null) {
                valueTooltip = null
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
                            valueTooltip = Tooltip(
                                this@SequenceEditorBase,
                                it.position - Vector2(layout.screenX, layout.screenY),
                                "$value"
                            )
                            requestRedraw()
                        }
                    }
                }
            }
        }
        // left and right select a value, up and down change it, finer with shift
        keyboard.pressed.listen { event ->
            if (baseValue.isEmpty()) return@listen
            val index = selectedIndex?.coerceIn(1, baseValue.size)
            val step = (range.endInclusive - range.start) / if (KeyModifier.SHIFT in event.modifiers) 1000.0 else 100.0
            when (event.key) {
                KEY_ARROW_LEFT -> selectedIndex = ((index ?: 2) - 1).coerceIn(1, baseValue.size)
                KEY_ARROW_RIGHT -> selectedIndex = ((index ?: 0) + 1).coerceIn(1, baseValue.size)
                KEY_ARROW_UP, KEY_ARROW_DOWN -> {
                    val i = (index ?: 1) - 1
                    selectedIndex = i + 1
                    val oldValue = baseValue.map { it }
                    baseValue[i] = (baseValue[i] + if (event.key == KEY_ARROW_UP) step else -step)
                        .coerceIn(range.start, range.endInclusive)
                    baseEvents.valueChanged.trigger(ValueChangedEvent(this, oldValue, baseValue))
                }
                else -> return@listen
            }
            requestRedraw()
            event.cancelPropagation()
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

    /** with the tooltip of the value */
    override val paintArea: Rectangle
        get() {
            val area = super.paintArea
            val tip = valueTooltip?.area ?: return area
            val x0 = minOf(area.x, layout.screenX + tip.x)
            val y0 = minOf(area.y, layout.screenY + tip.y)
            val x1 = maxOf(area.x + area.width, layout.screenX + tip.x + tip.width + 2.0)
            val y1 = maxOf(area.y + area.height, layout.screenY + tip.y + tip.height + 2.0)
            return Rectangle(x0, y0, x1 - x0, y1 - y0)
        }

    override fun draw(drawer: Drawer) {
        val controlArea = Rectangle(0.0, 0.0, layout.screenWidth, layout.screenHeight - footerHeight)

        drawer.stroke = computedStyle.effectiveColor?.opacify(0.25)
        drawer.strokeWeight = (1.0)


        val zeroHeight =
            0.0.map(range.start, range.endInclusive, -1.0, 1.0).coerceIn(-1.0, 1.0) * controlArea.height / -2.0
        drawer.lineSegment(
            0.0,
            controlArea.height / 2.0 + zeroHeight,
            layout.screenWidth,
            controlArea.height / 2.0 + zeroHeight
        )

        drawer.strokeWeight = 7.0
        drawer.fill = computedStyle.effectiveColor

        for (i in baseValue.indices) {
            val dx = layout.screenWidth / (baseValue.size + 1)
            val height = -baseValue[i].map(range.start, range.endInclusive, -1.0, 1.0)
                .coerceIn(-1.0, 1.0) * controlArea.height / 2.0

            val x = dx * (i + 1)
            drawer.lineCap = LineCap.ROUND
            drawer.stroke = computedStyle.effectiveColor
            drawer.lineSegment(x, controlArea.height / 2.0 + zeroHeight, x, controlArea.height / 2.0 + height)

            drawer.stroke = computedStyle.effectiveColor?.shade(1.1)
            drawer.fill = ColorRGBa.PINK
            // the stroke of the knob stays inside its radius
            drawer.strokeAlignment = StrokeAlignment.INSIDE
            drawer.circle(x, controlArea.height / 2.0 + height, 7.0)
            // the value that the keyboard changes
            if (selectedIndex == i + 1 && ElementPseudoClass("focus") in pseudoClasses) {
                drawer.fill = null
                drawer.strokeWeight = 2.0
                drawer.strokeAlignment = StrokeAlignment.CENTER
                drawer.circle(x, controlArea.height / 2.0 + height, 11.0)
                drawer.strokeWeight = 7.0
            }
        }

        drawer.isolated {
            drawer.translate(0.0, controlArea.height)
            drawer.fill = computedStyle.effectiveColor
            panelText?.let { text ->
                val face = text.face(computedStyle)
                // a line below the top of the footer
                text.drawLine(drawer, this, "label", computedStyle, label, 0.0,
                    4.0 + face.ascent - face.descent + face.lineGap, computedStyle.effectiveColor ?: ColorRGBa.WHITE)
            }
        }

        valueTooltip?.draw(drawer)
    }

    override fun close() {
        super.close()
        baseEvents.close()
    }
}

fun SequenceEditor.bind(
    property: KMutableProperty0<List<Double>>,
    program: Program? = null
): Binding0<SequenceEditor.ValueChangedEvent, List<Double>> {
    val program = program ?: (root() as? Body)?.controlManager?.program ?: error("no program")
    return Binding0(program, this, events.valueChanged, property, { it.newValue }, { value = it.toMutableList() })
}

fun SequenceEditor.bind(
    container: Any,
    property: KMutableProperty1<Any, List<Double>>,
    program: Program? = null
): Binding1<SequenceEditor.ValueChangedEvent, List<Double>> {
    val program = program ?: (root() as? Body)?.controlManager?.program ?: error("no program")
    return Binding1(
        program,
        this,
        events.valueChanged,
        container,
        property,
        { it.newValue },
        { value = it.toMutableList() })
}