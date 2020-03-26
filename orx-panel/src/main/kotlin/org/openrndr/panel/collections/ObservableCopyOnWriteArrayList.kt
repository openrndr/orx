package org.openrndr.panel.collections

import org.openrndr.events.Event
import java.util.concurrent.CopyOnWriteArrayList

class ObservableCopyOnWriteArrayList<E> : CopyOnWriteArrayList<E>() {

    val changed = Event<ObservableCopyOnWriteArrayList<E>>()
    override fun add(element: E): Boolean {
        return if (super.add(element)) {
            changed.trigger(this)
            true
        } else {
            false
        }
    }

    override fun remove(element: E): Boolean {
        return if (super.remove(element)) {
            changed.trigger(this)
            true
        } else {
            false
        }
    }

    override fun clear() {
        super.clear()
        changed.trigger(this)
    }
}