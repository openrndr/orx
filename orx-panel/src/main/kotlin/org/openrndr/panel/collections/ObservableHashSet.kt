package org.openrndr.panel.collections

import org.openrndr.events.Event
import java.util.*

class ObservableHashSet<E> : HashSet<E>() {

    class ChangeEvent<E>(val source: ObservableHashSet<E>, val added: Set<E>, val removed: Set<E>)

    val changed = Event<ChangeEvent<E>>()

    override fun add(element: E): Boolean {
        return if (super.add(element)) {
            changed.trigger(ChangeEvent(this, setOf(element), emptySet()))
            true
        } else {
            false
        }
    }

    override fun remove(element: E): Boolean {
        return if (super.remove(element)) {
            changed.trigger(ChangeEvent(this, emptySet(), setOf(element)))
            true
        } else {
            false
        }
    }

    override fun clear() {
        val old = this.toSet()
        super.clear()
        changed.trigger(ChangeEvent(this, emptySet(), old))
    }

}