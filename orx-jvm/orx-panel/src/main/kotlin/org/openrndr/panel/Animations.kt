package org.openrndr.panel

import org.openrndr.extra.style.TimingFunction
import org.openrndr.math.Vector2
import org.openrndr.panel.elements.Body
import org.openrndr.panel.elements.Element
import org.openrndr.panel.elements.requestRedraw
import org.openrndr.panel.elements.style
import org.openrndr.panel.style.opacity

/** an animation of [element] that calls [step] with its eased progress from 0 to 1 for [duration] seconds */
internal class Animation(
    val element: Element, val key: String, val duration: Double, val timing: TimingFunction, val step: (Double) -> Unit
) {
    var start = -1.0
}

/**
 * The animations of the elements of a control manager: moves of elements from where they were to where they are laid
 * out, see [Element.animateMoveFrom], fades, see [Element.fadeIn], and any other, see [ControlManager.animate]
 */
internal class Animations {
    private val running = mutableListOf<Animation>()
    private val pendingMoves = mutableListOf<Pair<Element, Vector2>>()

    val active: Boolean get() = running.isNotEmpty() || pendingMoves.isNotEmpty()

    fun add(animation: Animation) {
        // an animation replaces the one of the same element and kind
        running.removeAll { it.element === animation.element && it.key == animation.key }
        running.add(animation)
    }

    fun moveFrom(element: Element, from: Vector2) {
        pendingMoves.removeAll { it.first === element }
        pendingMoves.add(element to from)
    }

    /** the animations go on, before the styles and the layout */
    fun step(now: Double) {
        if (running.isEmpty()) return
        for (a in running.toList()) {
            if (a.element.disposed) { running.remove(a); continue }
            if (a.start < 0.0) a.start = now
            val t = ((now - a.start) / a.duration).coerceIn(0.0, 1.0)
            a.step(a.timing.ease(t))
            if (t >= 1.0) running.remove(a)
        }
    }

    /** the elements that moved start where they were */
    fun afterLayout(now: Double) {
        if (pendingMoves.isEmpty()) return
        for ((element, from) in pendingMoves) {
            if (element.disposed) continue
            val offset = from - Vector2(element.layout.screenX, element.layout.screenY)
            if (offset.length < 0.5) continue
            element.layoutOffset = offset
            (element.parent ?: element).requestRedraw()
            add(Animation(element, "move", 0.22, TimingFunction.EASE_OUT) { t ->
                element.layoutOffset = offset * (1.0 - t)
                (element.parent ?: element).requestRedraw()
            }.also { it.start = now })
        }
        pendingMoves.clear()
    }
}

private val Element.controlManager: ControlManager? get() = (root() as? Body)?.controlManager

/**
 * Moves the element from [from], a point of the window where its top-left corner was, to where it is laid out in the
 * next frame, like when it moved to another place in a list
 */
fun Element.animateMoveFrom(from: Vector2) {
    controlManager?.animations?.moveFrom(this, from)
}

/** fades the element in over [duration] seconds */
fun Element.fadeIn(duration: Double = 0.15) {
    val cm = controlManager ?: return
    style { opacity = 0.0 }
    cm.animate(this, duration, key = "fade") { t ->
        if (t >= 1.0) style?.removeProperty("opacity") else style { opacity = t }
    }
}
