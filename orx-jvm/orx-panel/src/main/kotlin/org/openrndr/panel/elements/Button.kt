package org.openrndr.panel.elements

import org.openrndr.KEY_SPACEBAR
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.StrokeAlignment

import org.openrndr.draw.isolated
import org.openrndr.events.Event
import org.openrndr.panel.style.*
import org.openrndr.shape.Rectangle

import kotlin.math.round


class Button : Element(ElementType("button")) {

    override val handlesKeyboardFocus = true
    /** the label, the attribute `label` */
    var label: String by attribute("label", "OK")

    class ButtonEvent(val source: Button)
    class Events: AutoCloseable {
        val clicked: Event<ButtonEvent> = Event()
        override fun close() {
            clicked.close()
        }
    }

    var data: Any? = null

    val events = Events()

    init {
        mouse.pressed.listen {
            it.cancelPropagation()
        }

        mouse.clicked.listen {
            if (disabled !in pseudoClasses) {
                events.clicked.trigger(ButtonEvent(this))
            }
            it.cancelPropagation()
        }

        keyboard.pressed.listen {
            if (it.key == KEY_SPACEBAR) {
                it.cancelPropagation()
                if (disabled !in pseudoClasses) {
                    events.clicked.trigger(ButtonEvent(this))
                }
            }
        }
    }

    override val widthHint: Double
        get() = panelText?.width(computedStyle, label) ?: error("no control manager")

    override fun draw(drawer: Drawer) {

        computedStyle.let {
            drawer.pushTransforms()
            drawer.pushStyle()
            drawer.fill = ((it.background as? Color.RGBa)?.color ?: ColorRGBa.PINK)

            drawer.isolated {
                drawer.stroke = computedStyle.effectiveBorderColor
                drawer.strokeWeight = layout.borderWidth
                // the background covers the box, the border is drawn inside it
                drawer.strokeAlignment = StrokeAlignment.INSIDE
                drawer.box(layout.boundsAtOrigin, cornerRadius)
            }

            val color = ((computedStyle.color as? Color.RGBa)?.color ?: ColorRGBa.WHITE).opacify(
                if (disabled in pseudoClasses) 0.25 else 1.0
            )
            panelText?.drawInBox(
                drawer, this, "label", computedStyle, label,
                layout.contentBoundsAtOrigin.let { it.copy(width = it.width + 1.0, height = it.height + 1.0) },
                computedStyle.computedTextHorizontalAlign, computedStyle.computedTextVerticalAlign, color
            )

            drawer.popStyle()
            drawer.popTransforms()
        }
    }

    override fun close() {
        super.close()
        events.close()
    }
}