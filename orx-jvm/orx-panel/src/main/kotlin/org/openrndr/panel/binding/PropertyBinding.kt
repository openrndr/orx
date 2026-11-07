package org.openrndr.panel.binding

import org.openrndr.events.Event
import org.openrndr.panel.elements.Element
import kotlin.reflect.KMutableProperty0

/**
 * A two-way binding of the value of an element to a value outside it, like a property: a change of the element by
 * the user sets the value, and a change of the value, by anything, shows in the element in the next frame. The
 * control manager checks the value once a frame, see [Element.bindValue]; the binding works before the element is in
 * a document, and ends with [close] or when the element is closed.
 */
class PropertyBinding<T> internal constructor(
    private val element: Element,
    changed: Event<*>,
    private val get: () -> T,
    private val set: (T) -> Unit,
    private val elementValue: () -> T,
    private val setElementValue: (T) -> Unit
) : AutoCloseable {
    private var last: T = get()

    /** true while the binding shows the value in the element, whose changes then are not the user's */
    private var showing = false

    private val listener: (Any) -> Unit = {
        if (!showing) changedByElement()
    }

    private fun changedByElement() {
        val v = elementValue()
        if (v != last) {
            last = v
            set(v)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private val event = changed as Event<Any>

    init {
        show(last)
        event.listen(listener)
    }

    private fun show(v: T) {
        showing = true
        try {
            setElementValue(v)
        } finally {
            showing = false
        }
    }

    /** shows a change of the value in the element */
    internal fun poll() {
        val v = get()
        if (v != last) {
            last = v
            show(v)
        }
    }

    override fun close() {
        event.listeners.remove(listener)
        element.valueBindings.remove(this)
    }
}

/**
 * Binds the value of this element to [get] and [set]: when [changed] happens, [elementValue] is set with [set], and
 * when [get] changes, [setElementValue] shows it in the element
 */
fun <T> Element.bindValue(
    changed: Event<*>, get: () -> T, set: (T) -> Unit, elementValue: () -> T, setElementValue: (T) -> Unit
): PropertyBinding<T> = PropertyBinding(this, changed, get, set, elementValue, setElementValue).also { valueBindings.add(it) }

/** binds the value of this element to [property], see [bindValue] */
fun <T> Element.bindValue(
    changed: Event<*>, property: KMutableProperty0<T>, elementValue: () -> T, setElementValue: (T) -> Unit
): PropertyBinding<T> = bindValue(changed, { property.get() }, { property.set(it) }, elementValue, setElementValue)
