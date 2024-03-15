package org.openrndr.panel.elements

import org.openrndr.color.ColorRGBa

import org.openrndr.draw.Drawer
import org.openrndr.extra.textwriter.Cursor
import org.openrndr.extra.textwriter.TextWriter

import org.openrndr.math.Vector2
import org.openrndr.panel.style.*

class EnvelopeButton : Element(ElementType("envelope-button")) {

    var label = "OK"
    var envelope = Envelope()
    set(value) {
        field = value
        envelopeSubscription?.let {
            value.events.envelopeChanged.cancel(it)
        }
        envelopeSubscription = value.events.envelopeChanged.listen {
            draw.dirty = true
        }
    }


    var envelopeSubscription: ((Envelope.EnvelopeChangedEvent)->Unit)? = null

    init {
        mouse.clicked.listen {
            append(SlideOut(0.0, screenArea.height, screenArea.width, 200.0, this))
        }
        envelopeSubscription = envelope.events.envelopeChanged.listen {
            draw.dirty = true
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
        drawer.rectangle(0.0, 0.0, screenArea.width, screenArea.height)

        (root() as? Body)?.controlManager?.fontManager?.let {
            var chartHeight = 0.0

            (root() as? Body)?.controlManager?.fontManager?.let {
                val font = it.font(computedStyle)

                val writer = TextWriter(drawer)
                drawer.fontMap = (font)
                drawer.fill = (ColorRGBa.BLACK)
                writer.cursor = Cursor(0.0,layout.screenHeight - 4.0)
                chartHeight = writer.cursor.y - font.height-4
                writer.text("$label")
            }


            val w = layout.screenWidth
            val h = chartHeight
            val m = envelope.points.map {
                val v = (Vector2(w, h) * it)
                Vector2(v.x, h - v.y)
            }

            if (m.size > 1) {
                drawer.stroke = (ColorRGBa.WHITE)
                drawer.strokeWeight = (2.0)
                drawer.lineStrip(m)
            }
            if (m.size == 1) {
                drawer.stroke = (ColorRGBa.WHITE)
                drawer.strokeWeight = (2.0)
                drawer.lineSegment(0.0, m[0].y, layout.screenWidth, m[0].y)
            }

            drawer.stroke = (ColorRGBa.BLACK.opacify(0.25))
            drawer.strokeWeight = (1.0)
            drawer.lineSegment(envelope.offset * w, 0.0, envelope.offset * w, chartHeight)

            drawer.lineSegment(0.0, 0.0, 3.0, 0.0)
            drawer.lineSegment(0.0, 0.0, 0.0, chartHeight)
            drawer.lineSegment(0.0, chartHeight, 3.0, chartHeight)

            drawer.lineSegment(w, 0.0, w-3.0, 0.0)
            drawer.lineSegment(w, 0.0, w, chartHeight)
            drawer.lineSegment(w, chartHeight, w-3.0, chartHeight)
        }
    }


    class SlideOut(val x: Double, val y: Double, val width: Double, val height: Double, parent: EnvelopeButton) : Element(ElementType("envelope-slide-out")) {

        init {

            mouse.clicked.listen {
                it.cancelPropagation()
            }
            style = StyleSheet(CompoundSelector.DUMMY).apply {
                position = Position.ABSOLUTE
                left = LinearDimension.PX(x)
                top = LinearDimension.PX(y)
                width = LinearDimension.PX(this@SlideOut.width)
                height = LinearDimension.Auto//LinearDimension.PX(this@SlideOut.height)
                overflow = Overflow.Scroll
                zIndex = ZIndex.Value(1)
                background = Color.RGBa(ColorRGBa(0.3, 0.3, 0.3))
            }

            append(EnvelopeEditor().apply {
                envelope = parent.envelope
            })

            append(Button().apply {
                label = "done"
                events.clicked.listen {
                    //parent.value = it.source.data as Item
                    //parent.events.valueChanged.onNext(ValueChangedEvent(parent, it.source.data as Item))
                    dispose()
                }
            })
        }

        override fun draw(drawer: Drawer) {
            drawer.fill = ((computedStyle.background as? Color.RGBa)?.color ?: ColorRGBa.PINK)
            drawer.rectangle(0.0, 0.0, screenArea.width, screenArea.height)
        }

        fun dispose() {
            parent?.remove(this)
        }
    }
}