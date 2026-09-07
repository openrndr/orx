package org.openrndr.panel.collections

import org.openrndr.events.Event

/**
 * A wrapper around a List that adds selection capability.
 * This class provides a selectedIndex property and a selected property to get the currently selected item.
 * It triggers a changed event whenever the selected index is modified.
 *
 * @param E the type of elements in this list
 * @property selectedIndex the index of the currently selected element, or -1 if no element is selected
 */

class SelectableList<E>(
    private val list: List<E>,
    selectedIndex: Int = -1
) : List<E> by list, AutoCloseable {

    val changed = Event<SelectableList<E>>()

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
        get() = if (_selectedIndex in list.indices) list[_selectedIndex] else null

    override fun close() {
        changed.close()
    }
}
