package org.openrndr.panel.collections

import org.openrndr.events.Event
import java.util.function.Predicate
import java.util.function.UnaryOperator

/**
 * A mutable list with selection capability.
 * Similar to [ObservableArrayList] but with [selectedIndex] and [selected] properties.
 * It triggers the [changed] event whenever the list is modified or [selectedIndex] changes.
 *
 * @param E the type of elements in this list
 * @param elements the initial collection of elements
 * @param selectedIndex the index of the currently selected element, or -1 if no element is selected
 */
open class SelectableMutableList<E>(
    elements: Collection<E> = emptyList(),
    selectedIndex: Int = -1
) : ArrayList<E>(elements), AutoCloseable {

    val changed = Event<SelectableMutableList<E>>()

    private inline fun <T> triggerChangeEventIfNeeded(f: () -> T): T {
        val result = f()
        _selectedIndex = _selectedIndex.coerceAtMost(lastIndex)
        if (result !is Boolean || result)
            changed.trigger(this)

        return result
    }
    
    private var _selectedIndex = selectedIndex

    /**
     * The index of the currently selected element.
     * Setting this property will trigger the changed event.
     */
    var selectedIndex: Int
        get() = _selectedIndex
        set(value) {
            if (value != _selectedIndex) {
                _selectedIndex = value
                changed.trigger(this)
            }
        }

    /**
     * The currently selected element, or null if no element is selected or the index is out of bounds.
     */
    val selected: E?
        get() = if (_selectedIndex in indices) get(_selectedIndex) else null

    override fun add(element: E) = triggerChangeEventIfNeeded {
        super.add(element)
    }

    override fun add(index: Int, element: E) = triggerChangeEventIfNeeded {
        super.add(index, element)
    }

// Java 21
//    override fun addFirst(element: E) = triggerChangeEventIfNeeded {
//        super.addFirst(element)
//    }

// Java 21
//    override fun addLast(element: E) = triggerChangeEventIfNeeded {
//        super.addLast(element)
//    }

    override fun addAll(elements: Collection<E>) = triggerChangeEventIfNeeded {
        super.addAll(elements)
    }

    override fun addAll(index: Int, elements: Collection<E>) = triggerChangeEventIfNeeded {
        super.addAll(index, elements)
    }

    override fun clear() = triggerChangeEventIfNeeded {
        super.clear()
    }

    override fun remove(element: E) = triggerChangeEventIfNeeded {
        super.remove(element)
    }

    override fun removeAll(elements: Collection<E>) = triggerChangeEventIfNeeded {
        super.removeAll(elements)
    }

    override fun removeAt(index: Int) = triggerChangeEventIfNeeded {
        super.removeAt(index)
    }

    override fun removeIf(filter: Predicate<in E>) = triggerChangeEventIfNeeded {
        super.removeIf(filter)
    }

// Java 21
//    override fun removeFirst() = triggerChangeEventIfNeeded {
//        super.removeFirst()
//    }

// Java 21
//    override fun removeLast() = triggerChangeEventIfNeeded {
//        super.removeLast()
//    }

    override fun retainAll(elements: Collection<E>) = triggerChangeEventIfNeeded {
        super.retainAll(elements)
    }

    override fun replaceAll(operator: UnaryOperator<E>) = triggerChangeEventIfNeeded {
        super.replaceAll(operator)
    }

    override fun sort(c: Comparator<in E>?) = triggerChangeEventIfNeeded {
        super.sort(c)
    }

    override fun set(index: Int, element: E) = triggerChangeEventIfNeeded {
        super.set(index, element)
    }

    override fun close() {
        // prevent emitting change events while closing
        super.clear()
        changed.close()
    }
}
