package org.openrndr.panel.elements

import org.openrndr.*
import org.openrndr.color.ColorHSVa
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.Drawer
import org.openrndr.draw.colorBuffer
import org.openrndr.events.Event
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.math.smoothstep
import org.openrndr.panel.binding.Binding0
import org.openrndr.panel.binding.Binding1
import org.openrndr.panel.style.Color
import org.openrndr.panel.style.color
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KMutableProperty1

class Colorpicker : Element {

    internal var colorMap: ColorBuffer? = null

    var label: String = "Color"
    private var dirtyColorMap = true

    var saturation = 0.5
        set(value) {
            if (field != value) {
                field = value
                color = color.toHSVa().copy(s = value).toRGBa()
                dirtyColorMap = true
                draw.dirty = true
            }
        }
    var color: ColorRGBa = ColorRGBa.WHITE
        set(value) {
            if (field != value) {
                field = value
                saturation = color.toHSVa().s
                dirtyColorMap = true
                draw.dirty = true
            }
        }

    private var focussed = false

    class ColorChangedEvent(
        val source: Colorpicker,
        val oldColor: ColorRGBa,
        val newColor: ColorRGBa
    )

    class Events : AutoCloseable {
        val colorChanged = Event<ColorChangedEvent>()
        override fun close() {
            colorChanged.close()
        }
    }

    val events = Events()

    private fun pick(e: MouseEvent) {
        val dx = (e.position.x - layout.screenX - layout.paddingLeft).coerceIn(0.0, layout.screenWidth -1)
        var dy = (e.position.y - layout.screenY - layout.paddingTop).coerceIn(0.0, layout.screenHeight -1)

        val h = colorMap!!.height - 1.0
        dy = h - dy
        val oldColor = color
        val hsv = ColorHSVa(360.0 / layout.screenWidth * dx, saturation, dy / h)
        color = hsv.toRGBa()
        draw.dirty = true
        events.colorChanged.trigger(ColorChangedEvent(this, oldColor, color))
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

        keyboard.pressed.listen {
            val f = if (KeyModifier.SHIFT in it.modifiers) 0.1 else 1.0
            when (it.key) {
                KEY_ARROW_LEFT -> {
                    val hue = (color.toHSVa().h - 10.0 * f).mod(360.0)
                    color = ColorHSVa(hue, color.toHSVa().s, color.toHSVa().v).toRGBa()
                    events.colorChanged.trigger(ColorChangedEvent(this, color, color))
                }
                KEY_ARROW_RIGHT -> {
                    val hue = (color.toHSVa().h + 10.0 * f).mod(360.0)
                    color = ColorHSVa(hue, color.toHSVa().s, color.toHSVa().v).toRGBa()
                    events.colorChanged.trigger(ColorChangedEvent(this, color, color))
                }
                KEY_ARROW_UP -> {
                    val value = (color.toHSVa().v + 0.1 * f).coerceIn(0.0, 1.0)
                    color = ColorHSVa(color.toHSVa().h, color.toHSVa().s, value).toRGBa()
                    events.colorChanged.trigger(ColorChangedEvent(this, color, color))
                }
                KEY_ARROW_DOWN -> {
                    val value = (color.toHSVa().v - 0.1 * f).coerceIn(0.0, 1.0)
                    color = ColorHSVa(color.toHSVa().h, color.toHSVa().s, value).toRGBa()
                    events.colorChanged.trigger(ColorChangedEvent(this, color, color))
                }
            }
        }

        mouse.pressed.listen { it.cancelPropagation(); focussed = true }
        mouse.clicked.listen { it.cancelPropagation(); pick(it); focussed = true; }
        mouse.dragged.listen { it.cancelPropagation(); pick(it); focussed = true; }
    }

    private fun generateColorMap() {
        colorMap?.shadow?.let {

            val h = it.colorBuffer.height - 1.0

            for (y in 0 until it.colorBuffer.height) {
                for (x in 0 until it.colorBuffer.width) {
                    val hsv = ColorHSVa(360.0 / it.colorBuffer.width * x, saturation, (h - y) / h)
                    it.write(x, y, hsv.toRGBa().toLinear())
                }
            }
            it.upload()
        }
    }

    override fun draw(drawer: Drawer) {
        if (colorMap == null) {
            val contentBounds = layout.contentBoundsAtOrigin
            colorMap = colorBuffer(contentBounds.width.toInt(), layout.contentBounds.height.toInt(), 1.0)
            dirtyColorMap = true
        }

        if (dirtyColorMap) {
            generateColorMap()
            dirtyColorMap = false
        }

        drawer.imageFit(colorMap!!, layout.contentBoundsAtOrigin)
        drawer.fill = color
        drawer.stroke = null
        drawer.shadeStyle = null

        drawer.stroke =  ColorRGBa.WHITE.mix(ColorRGBa.BLACK, smoothstep(0.45, 0.55, color.luminance))
        drawer.strokeWeight = 2.0

        val x = color.toHSVa().h/360.0 * colorMap!!.width.toDouble()
        val y =(1.0 -  color.toHSVa().v/1.0) * colorMap!!.height.toDouble()
        drawer.circle(x, y, 10.0)

        val f = (root() as? Body)?.controlManager?.fontManager?.font(computedStyle)!!
        drawer.fontMap = f
        drawer.fill = ((computedStyle.color as Color.RGBa).color)

    }

    override fun close() {
        super.close()
        events.close()
    }
}

fun Colorpicker.bind(
    property: KMutableProperty0<ColorRGBa>,
    program: Program? = null
): Binding0<Colorpicker.ColorChangedEvent, ColorRGBa> {
    val program = program ?: (root() as? Body)?.controlManager?.program ?: error("no program")
    return Binding0(program, this, events.colorChanged, property, { it.newColor }, { color = it })
}

fun Colorpicker.bind(
    container: Any,
    property: KMutableProperty1<Any, ColorRGBa>,
    program: Program? = null
): Binding1<Colorpicker.ColorChangedEvent, ColorRGBa> {
    val program = program ?: (root() as? Body)?.controlManager?.program ?: error("no program")
    return Binding1(program, this, events.colorChanged, container, property, { it.newColor }, { color = it })
}