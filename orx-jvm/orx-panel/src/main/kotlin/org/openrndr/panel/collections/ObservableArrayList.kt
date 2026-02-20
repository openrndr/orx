package org.openrndr.panel.collections

import org.openrndr.events.Event
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A specialized implementation of ArrayList that triggers an event whenever the list is modified.
 * This class supports observing additions, removals, and clear operations, and notifies listeners
 * via the `changed` event.
 *
 * @param E the type of elements in this list
 */
class ObservableArrayList<E> : ArrayList<E>(), AutoCloseable {

    val changed = Event<ObservableArrayList<E>>()

    override fun addAll(elements: Collection<E>): Boolean {
        return if (super.addAll(elements)) {
            changed.trigger(this)
            true
        } else {
            false
        }
    }

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

    override fun removeAll(elements: Collection<E>): Boolean {
        return if (super.removeAll(elements)) {
            changed.trigger(this)
            true
        } else {
            false
        }
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        return if (super.retainAll(elements)) {
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

    override fun close() {
        // prevent emitting change events while closing
        super.clear()
        changed.close()
    }
}