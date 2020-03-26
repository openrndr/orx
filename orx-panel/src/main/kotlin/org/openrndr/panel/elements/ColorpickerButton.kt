package org.openrndr.panel.elements

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.LineCap
import org.openrndr.events.Event
import org.openrndr.panel.style.*
import org.openrndr.text.Writer

class ColorpickerButton : Element(ElementType("colorpicker-button")) {

    var label: String = "OK"
    var color: ColorRGBa = ColorRGBa(0.5, 0.5, 0.5)
            set(value)  {
                if (value != field) {
                    field = value
                    events.valueChanged.trigger(ColorChangedEvent(this, value))
                }
            }

    class ColorChangedEvent(val source: ColorpickerButton, val color: ColorRGBa)

    class Events {
        val valueChanged = Event<ColorChangedEvent>()
    }

    val events = Events()

    init {
        mouse.pressed.listen {
            it.cancelPropagation()
        }
        mouse.clicked.listen {
            append(SlideOut(0.0, screenArea.height, screenArea.width, 200.0, color, this))
            it.cancelPropagation()
        }
    }

    override fun append(element: Element) {
        when (element) {
            is Item, is SlideOut -> super.append(element)
            else -> throw RuntimeException("only item and slideout")
        }
        super.append(element)
    }

    fun items(): List<Item> = children.filter { it is Item }.map { it as Item }

    override fun draw(drawer: Drawer) {

        drawer.fill = ((computedStyle.background as? Color.RGBa)?.color ?: ColorRGBa.PINK)
        drawer.stroke = null
        drawer.strokeWeight = 0.0
        drawer.rectangle(0.0, 0.0, screenArea.width, screenArea.height)

        (root() as? Body)?.controlManager?.fontManager?.let {
            val font = it.font(computedStyle)

            val writer = Writer(drawer)
            drawer.fontMap = (font)

            val text = "$label"

            val textWidth = writer.textWidth(text)
            val textHeight = font.ascenderLength

            val offset = Math.round((layout.screenWidth - textWidth) / 2.0)
            val yOffset = Math.round((layout.screenHeight / 2) + textHeight / 2.0) - 2.0

            drawer.fill = ((computedStyle.color as? Color.RGBa)?.color ?: ColorRGBa.WHITE)
            drawer.fontMap = font
            drawer.text(text, 0.0 + offset, 0.0 + yOffset)
            drawer.stroke = (color)
            drawer.pushStyle()
            drawer.strokeWeight = (4.0)
            drawer.lineCap = (LineCap.ROUND)
            drawer.lineSegment(2.0, layout.screenHeight - 2.0, layout.screenWidth - 2.0, layout.screenHeight - 2.0)
            drawer.popStyle()
        }
    }

    class SlideOut(val x: Double, val y: Double, val width: Double, val height: Double, color:ColorRGBa, parent: Element) : Element(ElementType("slide-out")) {

        init {
            style = StyleSheet(CompoundSelector.DUMMY).apply {
                position = Position.ABSOLUTE
                left = LinearDimension.PX(x)
                top = LinearDimension.PX(y)
                width = LinearDimension.PX(this@SlideOut.width)
                height = LinearDimension.Auto//LinearDimension.PX(this@SlideOut.height)
                overflow = Overflow.Scroll
                zIndex = ZIndex.Value(1000)
                background = Color.RGBa(ColorRGBa(0.3, 0.3, 0.3))
            }

            val colorPicker = Colorpicker().apply {
                this.color = color
                label = (parent as ColorpickerButton).label
                events.colorChanged.listen {
                    parent.color = it.newColor
                    parent.events.valueChanged.trigger(ColorChangedEvent(parent, parent.color))
                }
            }
            append(colorPicker)

            mouse.exited.listen {
                dispose()
            }
        }

        override fun draw(drawer: Drawer) {
            (root() as Body).controlManager.keyboardInput.requestFocus(children[0])
            drawer.fill = ((computedStyle.background as? Color.RGBa)?.color ?: ColorRGBa.PINK)
            drawer.rectangle(0.0, 0.0, screenArea.width, screenArea.height)
        }

        fun dispose() {
            parent?.remove(this)
        }
    }
}