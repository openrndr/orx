package org.openrndr.panel.elements

import org.openrndr.events.Event
import org.openrndr.math.Vector2

/**
 * A drag of [data] from [source], at [position], a point of the window in the coordinates of the element it is given to
 * before its transforms, like those of mouse events, see [DragAndDrop]
 */
class DragEvent(val data: Any, val source: Element, val position: Vector2)

/**
 * What an element does in drag and drop: it can be dragged when it has [data], and things are dropped on it when
 * [accepts] accepts their data. While something is dragged its source is `:dragging`, and the drop target under the
 * pointer that accepts it is `:drag-over`; a ghost with [label] follows the pointer.
 */
class DragAndDrop : AutoCloseable {
    /** the data the element is dragged with, null when it can not be dragged */
    var data: (() -> Any?)? = null

    /** the text of the ghost that follows the pointer, the label or text of the element when null */
    var label: (() -> String)? = null

    /** true when the element accepts drops of the data, null when it accepts none */
    var accepts: ((Any) -> Boolean)? = null

    /** something that the element accepts is dragged onto it */
    val entered = Event<DragEvent>("drag-entered")

    /** something that the element accepts moves over it */
    val over = Event<DragEvent>("drag-over")

    /** something is dragged off the element */
    val left = Event<DragEvent>("drag-left")

    /** something is dropped on the element */
    val dropped = Event<DragEvent>("drag-dropped")

    /** a drag of the element ended, dropped or not */
    val ended = Event<DragEvent>("drag-ended")

    override fun close() {
        entered.close(); over.close(); left.close(); dropped.close(); ended.close()
    }
}
