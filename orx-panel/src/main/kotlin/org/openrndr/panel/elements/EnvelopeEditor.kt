package org.openrndr.panel.elements

import org.openrndr.MouseButton
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.math.Vector2
import org.openrndr.KeyModifier
import org.openrndr.events.Event

class Envelope(constant:Double = 0.5) {

    val points = mutableListOf(Vector2(0.5, constant))
    var activePoint: Vector2? = null

    var offset:Double = 0.0
        set(value) { field = value; events.envelopeChanged.trigger(EnvelopeChangedEvent(this))}

    class EnvelopeChangedEvent(val envelope: Envelope)

    class Events {
        val envelopeChanged = Event<EnvelopeChangedEvent>("envelope-changed")
    }
    val events = Events()

    fun insertPoint(v: Vector2) {
        for (i in 0 until points.size) {
            if (points[i].x > v.x) {
                points.add(i, v)
                activePoint = v
                events.envelopeChanged.trigger(EnvelopeChangedEvent(this))
                return
            }
        }
        points.add(v)
        activePoint = v
        fixBounds()
        events.envelopeChanged.trigger(EnvelopeChangedEvent(this))
    }

    fun findNearestPoint(v: Vector2) = points.minBy { (it - v).length }

    fun removePoint(v: Vector2) {
        points.remove(v)
        if (v === activePoint) {
            activePoint = null
        }
        fixBounds()
        events.envelopeChanged.trigger(EnvelopeChangedEvent(this))
    }

    private fun fixBounds() {
        if (points.size >= 2) {
            if (points[0].x != 0.0) {
                points[0].copy(x=0.0).let {
                    if (activePoint === points[0]) {
                        activePoint = it
                    }
                    points[0] = it
                }
            }
            if (points[points.size-1].x != 1.0) {
                points[points.size-1].copy(x=1.0).let {
                    if (activePoint === points[points.size-1]) {
                        activePoint = it
                    }
                    points[points.size-1] = it
                }
            }
        }
    }

    fun updatePoint(old: Vector2, new: Vector2) {
        val index = points.indexOf(old)
        if (index != -1) {
            points[index] = new
        }
        if (old === activePoint) {
            activePoint = new
        }
        points.sortBy { it.x }

        fixBounds()
        events.envelopeChanged.trigger(EnvelopeChangedEvent(this))
    }

    fun value(t: Double): Double {

        val st = t.coerceIn(0.0, 1.0)

        if (points.size == 1) {
            return points[0].y
        }
        else if (points.size == 2) {
            return points[0].y * (1.0-st) + points[1].y * st
        } else {
            if (st == 0.0) {
                return points[0].y
            }
            if (st == 1.0) {
                return points[points.size-1].y
            }

            for (i in 0 until points.size-1) {
                if (points[i].x <= st && points[i+1].x > st) {
                    val left = points[i]
                    var right = points[i+1]

                    val dt = right.x - left.x
                    if (dt > 0.0) {
                        val f = (t - left.x) / dt
                        return left.y * (1.0-f) + right.y * f
                    } else {
                        return left.y
                    }

                }
            }
            return points[0].y

        }

    }

}

// --

class EnvelopeEditor : Element(ElementType("envelope-editor")) {

    var envelope = Envelope()

    init {

        fun query(position: Vector2): Vector2 {
            val x = (position.x - layout.screenX) / layout.screenWidth
            val y = 1.0 - ((position.y - layout.screenY) / layout.screenHeight)

            return Vector2(x, y)
        }

        mouse.clicked.listen {
            val query = query(it.position)
            val nearest = envelope.findNearestPoint(query)
            val distance = nearest?.let { (it - query).length }

            if (it.button == MouseButton.LEFT && !it.modifiers.contains(KeyModifier.CTRL)) {
                when {
                    distance == null -> {
                        envelope.insertPoint(query)
                        draw.dirty = true
                    }
                    distance < 0.05 -> {
                        envelope.activePoint = nearest
                    }
                    else -> {
                        envelope.insertPoint(query)
                        draw.dirty = true
                    }
                }
            } else if (it.button == MouseButton.LEFT) {
                if (distance != null && distance < 0.1) {
                    envelope.removePoint(nearest)
                    draw.dirty = true
                }
            }
            it.cancelPropagation()
        }

        mouse.pressed.listen {
            val query = query(it.position)
            val nearest = envelope.findNearestPoint(query)
            val distance = nearest?.let { it - query }?.length

            if (distance == null) {
                envelope.activePoint = null
                draw.dirty = true
            } else if (distance < 0.1) {
                envelope.activePoint = nearest
            } else {
                envelope.activePoint = null
            }
            it.cancelPropagation()
        }

        mouse.dragged.listen {
            envelope.activePoint?.let { activePoint ->
                val query = query(it.position)
                if (!it.modifiers.contains(KeyModifier.SHIFT)) {
                    envelope.updatePoint(activePoint, query)
                } else {
                    envelope.updatePoint(activePoint, Vector2(activePoint.x, query.y))
                }
                draw.dirty = true
            }
            it.cancelPropagation()
        }
    }

    override fun draw(drawer: Drawer) {
        val w = layout.screenWidth
        val h = layout.screenHeight

        val m = envelope.points.map {
            val v = (it * Vector2(w, h))
            Vector2(v.x, h - v.y)
        }

        drawer.stroke = (ColorRGBa.BLACK.opacify(0.25))
        drawer.strokeWeight = (1.0)
        drawer.lineSegment(layout.screenWidth/2.0, 0.0, layout.screenWidth/2.0,layout.screenHeight)
        drawer.lineSegment(0.0,layout.screenHeight/2.0,layout.screenWidth, layout.screenHeight/2.0)

        if (m.size > 1) {
            drawer.stroke = (ColorRGBa.WHITE)
            drawer.strokeWeight = (2.0)
            drawer.lineStrip(m)
            drawer.fill = (ColorRGBa.WHITE)
            drawer.stroke = null
            drawer.circles(m, 4.0)
        } else if (m.size == 1) {
            drawer.stroke = (ColorRGBa.WHITE)
            drawer.strokeWeight = (2.0)
            drawer.lineSegment(0.0, m[0].y, layout.screenWidth, m[0].y)
            drawer.fill = (ColorRGBa.WHITE)
            drawer.stroke = null
            drawer.circle(m[0], 4.0)
        }
    }
}
