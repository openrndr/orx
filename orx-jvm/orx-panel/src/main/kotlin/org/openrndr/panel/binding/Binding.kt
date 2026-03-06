package org.openrndr.panel.binding

import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield
import org.openrndr.Program
import org.openrndr.events.Event
import org.openrndr.launch
import org.openrndr.panel.elements.Element
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KMutableProperty1

/**
 * A utility class for bi-directionally binding a property of type [T] to an [Element],
 * where changes in one are synchronized with the other. The binding operates on the
 * provided [Program] instance's coroutine scope and listens for events of type [E],
 * which are used to update the property value.
 *
 * @param E The type of the input event used for updating the property.
 * @param T The data type of the property being bound.
 * @param program The program instance managing the coroutine lifecycle for this binding.
 * @param element The UI element associated with this binding.
 * @param event The event to listen to for changes triggering property updates.
 * @param property A mutable property reference to bind to the element value.
 * @param newValueFromEvent A lambda function that converts the event of type [E] into a new property value of type [T].
 * @param setElementValue A lambda function that updates the element with the current property value.
 *
 * This class continuously monitors changes to the bound property and the associated element:
 * - When an [E] event is captured, it invokes [newValueFromEvent] to compute a new property value.
 *   If the value has changed, the property is updated, and the change is propagated to the element.
 * - Concurrently, it checks for any modifications in the property value, ensuring the changes reflect
 *   back onto the UI element using [setElementValue].
 *
 * The binding operates until closed explicitly using the [close] method, or if the parent element is disposed.
 *
 * Implements [AutoCloseable] to ensure proper cleanup of resources like coroutines and event listeners.
 *
 * Properties:
 * - `currentValue` maintains the current state of the bound property.
 * - `closed` indicates whether the binding has been closed.
 *
 * Closing the binding stops its coroutine, removes event listeners, and marks it as inactive.
 */
class Binding0<E: Any, T: Any> (program: Program,
                                val element: Element,
                                val event: Event<E>,
                                val property: KMutableProperty0<T>,
                                newValueFromEvent: (E) -> T,
                                setElementValue: (T) -> Unit

    ): AutoCloseable {
    private var currentValue: T
    var closed: Boolean = false
        private set

    private var job: Job? = null
    private var listener: ((E) -> Unit)? = null

    init {
        currentValue = property.get()
        setElementValue(currentValue)
        listener = event.listen {
            val candidate = newValueFromEvent(it)
            if (candidate != currentValue) {
                currentValue = candidate
                property.set(currentValue)
            }
        }
        job = program.launch {
            while (this.isActive && !element.disposed && !closed) {
                val candidate = property.get()
                if (candidate != currentValue) {
                    currentValue = candidate
                    setElementValue(currentValue)
                }
                yield()
            }
            event.listeners.remove(listener)
            listener = null
        }
    }

    override fun close() {
        closed = true
        job?.cancel()
    }
}

/**
 * A class that creates a two-way binding between an event-driven system and a property of a container,
 * ensuring synchronization of state between a `KMutableProperty1` and an external element's value.
 *
 * @param E The type of the event payload.
 * @param T The type of the property being bound.
 * @param program The program execution context for managing coroutines.
 * @param element The external UI element or container being synchronized.
 * @param event The event that triggers updates to the binding.
 * @param container The container object that owns the property being synchronized.
 * @param property The mutable property of the container to be synchronized.
 * @param newValueFromEvent A lambda mapping an event payload to the new property value.
 * @param setElementValue A lambda to update the external element's value.
 */

class Binding1<E: Any, T: Any> (program: Program,
                                val element: Element,
                                val event: Event<E>,
                                val container: Any,
                                val property: KMutableProperty1<Any, T>,
                                newValueFromEvent: (E) -> T,
                                setElementValue: (T) -> Unit

): AutoCloseable {
    var currentValue: T
    var closed: Boolean = false
        private set

    private var job: Job? = null
    private var listener: ((E) -> Unit)? = null

    init {
        listener = event.listen {
            val candidate = newValueFromEvent(it)
            if (candidate != currentValue) {
                currentValue = candidate
                property.set(container, currentValue)
            }
        }
        currentValue = property.get(container)
        job = program.launch {
            while (this.isActive && !element.disposed && !closed) {
                val candidate = property.get(container)
                if (candidate != currentValue) {
                    currentValue = candidate
                    setElementValue(currentValue)
                }
                yield()
            }
            event.listeners.remove(listener)
            listener = null
        }
    }

    override fun close() {
        closed = true
        job?.cancel()
    }
}