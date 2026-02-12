package org.openrndr.panel.elements

import org.openrndr.KEY_SPACEBAR
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer

import org.openrndr.draw.isolated
import org.openrndr.draw.loadFont
import org.openrndr.events.Event
import org.openrndr.extra.textwriter.TextWriter
import org.openrndr.panel.style.*
import org.openrndr.shape.Rectangle

import kotlin.math.round


class Button : Element(ElementType("button")) {

    override val handlesKeyboardFocus = true
    var label: String = "OK"

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
        get() {
            computedStyle.let { style ->
                val fontUrl = (root() as? Body)?.controlManager?.fontManager?.resolve(style.fontFamily) ?: "broken"
                val fontSize = (style.fontSize as? LinearDimension.PX)?.value ?: 14.0

                val program = (root() as? Body)?.controlManager?.program ?: error("no program")
                val fontMap =  program.loadFont(fontUrl, fontSize)

                val writer = TextWriter(null)

                writer.box = Rectangle(0.0,
                        0.0,
                        Double.POSITIVE_INFINITY,
                        Double.POSITIVE_INFINITY)

                writer.drawStyle.fontMap = fontMap
                writer.newLine()
                writer.text(label, visible = false)

                return writer.cursor.x
            }
        }

    override fun draw(drawer: Drawer) {

        computedStyle.let {

            drawer.pushTransforms()
            drawer.pushStyle()
            drawer.fill = ((it.background as? Color.RGBa)?.color ?: ColorRGBa.PINK)

            drawer.isolated {
                drawer.stroke = computedStyle.effectiveBorderColor
                drawer.strokeWeight = computedStyle.effectiveBorderWidth
                drawer.rectangle(layout.boundsAtOrigin)
            }

            (root() as? Body)?.controlManager?.fontManager?.let {
                val font = it.font(computedStyle)
                val writer = TextWriter(drawer)
                writer.box = layout.boundsAtOriginPadded(computedStyle).let { it.copy(width = it.width + 1.0, height = it.height + 1.0) }
                writer.verticalAlign =  computedStyle.computedTextVerticalAlign
                writer.horizontalAlign = computedStyle.computedTextHorizontalAlign

                drawer.fontMap = (font)
                val textWidth = writer.textWidth(label)
                val textHeight = font.ascenderLength

                val offset = round((layout.screenWidth - textWidth) / 2.0)
                val yOffset = round((layout.screenHeight / 2) + textHeight / 2.0 - 2.0) * 1.0

                drawer.fill = ((computedStyle.color as? Color.RGBa)?.color ?: ColorRGBa.WHITE).opacify(
                        if (disabled in pseudoClasses) 0.25 else 1.0
                )
                //drawer.text(label, 0.0 + offset, 0.0 + yOffset)
                writer.text(label)
            }

            drawer.popStyle()
            drawer.popTransforms()
        }
    }

    override fun close() {
        super.close()
        events.close()
    }
}