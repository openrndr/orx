package org.openrndr.panel.elements

import org.openrndr.KEY_BACKSPACE
import org.openrndr.KEY_ENTER
import org.openrndr.KEY_ESCAPE
import org.openrndr.MouseEvent
import org.openrndr.color.ColorHSVa
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.Drawer
import org.openrndr.draw.colorBuffer
import org.openrndr.events.Event
import org.openrndr.panel.style.Color
import org.openrndr.panel.style.color

class Colorpicker : Element {

    internal var colorMap: ColorBuffer? = null

    var label: String = "Color"

    var saturation = 0.5
    var color: ColorRGBa
        set(value) {
            realColor = value
            saturation = color.toHSVa().s
            generateColorMap()
            draw.dirty = true
        }
        get() {
            return realColor
        }

    private var realColor = ColorRGBa.WHITE
    private var focussed = false

    class ColorChangedEvent(val source: Colorpicker,
                            val oldColor: ColorRGBa,
                            val newColor: ColorRGBa)

    class Events {
        val colorChanged = Event<ColorChangedEvent>()
    }

    val events = Events()

    private var keyboardInput = ""
    private fun pick(e: MouseEvent) {
        val dx = e.position.x - layout.screenX
        var dy = e.position.y - layout.screenY

        dy = 50.0 - dy
        val oldColor = color
        val hsv = ColorHSVa(360.0 / layout.screenWidth * dx, saturation, dy / 50.0)
        realColor = hsv.toRGBa()
        draw.dirty = true
        events.colorChanged.trigger(ColorChangedEvent(this, oldColor, realColor))
        e.cancelPropagation()
    }
    constructor() : super(ElementType("colorpicker")) {
        generateColorMap()

        mouse.exited.listen {
            focussed = false
        }

        mouse.scrolled.listen {
            if (colorMap != null) {
                //if (focussed) {
                saturation = (saturation - it.rotation.y * 0.01).coerceIn(0.0, 1.0)
                generateColorMap()
                colorMap?.shadow?.upload()
                it.cancelPropagation()
                pick(it)
                requestRedraw()
                //}
            }
        }

        keyboard.focusLost.listen {
            keyboardInput = ""
            draw.dirty = true
        }

        keyboard.character.listen {
            keyboardInput += it.character
            draw.dirty = true
            it.cancelPropagation()
        }

        keyboard.pressed.listen {
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
                val number = if (keyboardInput.length == 6) keyboardInput.toIntOrNull(16) else null

                number?.let {
                    val r = (number shr 16) and 0xff
                    val g = (number shr 8) and 0xff
                    val b = number and 0xff
                    val oldColor = color
                    color = ColorRGBa(r / 255.0, g / 255.0, b / 255.0)
                    events.colorChanged.trigger(ColorChangedEvent(this, oldColor, realColor))
                    keyboardInput = ""
                    draw.dirty = true

                }
                it.cancelPropagation()
            }
        }


        mouse.pressed.listen { it.cancelPropagation(); focussed = true }
        mouse.clicked.listen { it.cancelPropagation(); pick(it); focussed = true; }
        mouse.dragged.listen { it.cancelPropagation(); pick(it); focussed = true; }
    }

    private fun generateColorMap() {
        colorMap?.shadow?.let {
            for (y in 0..49) {
                for (x in 0 until it.colorBuffer.width) {
                    val hsv = ColorHSVa(360.0 / it.colorBuffer.width * x, saturation, (49 - y) / 49.0)
                    it.write(x, y, hsv.toRGBa())
                }
            }
            it.upload()
        }
    }

    override fun draw(drawer: Drawer) {
        if (colorMap == null) {
            colorMap = colorBuffer(layout.screenWidth.toInt(), 50, 1.0)
            generateColorMap()
        }

        drawer.image(colorMap!!, 0.0, 0.0)
        drawer.fill = (color)
        drawer.stroke = null
        drawer.shadeStyle = null
        drawer.rectangle(0.0, 50.0, layout.screenWidth, 20.0)

        val f = (root() as? Body)?.controlManager?.fontManager?.font(computedStyle)!!
        drawer.fontMap = f
        drawer.fill = ((computedStyle.color as Color.RGBa).color)

        if (keyboardInput.isNotBlank()) {
            drawer.text("input: $keyboardInput", 0.0, layout.screenHeight)
        }
    }
}