package org.openrndr.extra.parameters

/**
 * A list with a selected element, the value of a property with a [ListParameter] or a [MutableListParameter]
 * annotation, like the `SelectableList` and `SelectableMutableList` of orx-panel. Lets the selection be saved and
 * loaded without knowing the class of the list.
 */
interface Selectable {
    /** the index of the selected element, -1 when no element is selected */
    var selectedIndex: Int
}
