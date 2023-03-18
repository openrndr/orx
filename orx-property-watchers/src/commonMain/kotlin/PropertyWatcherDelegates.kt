package org.openrndr.extra.propertywatchers

import org.openrndr.events.Event
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0

/**
 * Property watcher delegate
 * @see watchingProperty
 */
class PropertyWatcherDelegate<V, R>(
    private val property: KProperty0<V>,
    private val valueChangedEvent: Event<V>,
    private val cleaner: ((R) -> Unit)? = null,
    val function: (V) -> R
) {
    private var watchValue: V? = null
    private var value: R? = null
    operator fun getValue(any: Any, property: KProperty<*>): R {
        val ref = this.property.get()
        if (watchValue != ref) {
            watchValue = ref
            value?.let {
                cleaner?.invoke(it)
            }
            value = function(ref)
        }
        return value ?: error("no value?")
    }
}

/**
 * Property watcher delegate
 * @see watchingProperties
 */
class PropertyWatcherDelegate2<V0, V1, R>(
    private val toWatch0: KProperty0<V0>,
    private val toWatch1: KProperty0<V1>,
    private val cleaner: ((R) -> Unit)? = null,
    private val function: (V0, V1) -> R
) {
    private var watchValue0: V0? = null
    private var watchValue1: V1? = null
    private var value: R? = null
    operator fun getValue(any: Any, property: KProperty<*>): R {
        val ref0 = toWatch0.get()
        val ref1 = toWatch1.get()
        if (watchValue0 != ref0 || watchValue1 != ref1) {
            watchValue0 = ref0
            watchValue1 = ref1

            value?.let {
                cleaner?.invoke(it)
            }
            value = function(ref0, ref1)
        }
        return value ?: error("no value?")
    }
}

/**
 * Property watcher delegate
 * @see watchingProperties
 */
class PropertyWatcherDelegate3<V0, V1, V2, R>(
    private val toWatch0: KProperty0<V0>,
    private val toWatch1: KProperty0<V1>,
    private val toWatch2: KProperty0<V2>,
    private val cleaner: ((R) -> Unit)? = null,
    private val function: (V0, V1, V2) -> R
) {
    private var watchValue0: V0? = null
    private var watchValue1: V1? = null
    private var watchValue2: V2? = null
    private var value: R? = null
    operator fun getValue(any: Any, property: KProperty<*>): R {
        val ref0 = toWatch0.get()
        val ref1 = toWatch1.get()
        val ref2 = toWatch2.get()
        if (watchValue0 != ref0 || watchValue1 != ref1 || watchValue2 != ref2) {
            watchValue0 = ref0
            watchValue1 = ref1
            value?.let {
                cleaner?.invoke(it)
            }
            value = function(ref0, ref1, ref2)
        }
        return value ?: error("no value?")
    }
}


/**
 * Delegate property value to a function for which the value of a single property is watched
 * @param property the property for which to watch for value changes
 * @param function a function that maps the property value to a new value
 */
fun <V, R> watchingProperty(
    property: KProperty0<V>,
    cleaner: ((R) -> Unit)? = null,
    function: (value: V) -> R
): PropertyWatcherDelegate<V, R> {
    return PropertyWatcherDelegate(property, Event("value-changed-${property.name}"), cleaner, function)
}

/**
 * Delegate property value to a function for which the values of 2 properties are watched
 * @param property0 the first property for which to watch for value changes
 * @param property1 the second property which to watch for value changes
 * @param function a function that maps the two property values to a new value
 */
fun <V0, V1, R> watchingProperties(
    property0: KProperty0<V0>,
    property1: KProperty0<V1>,
    cleaner: ((R) -> Unit)?,
    function: (value0: V0, value1: V1) -> R
): PropertyWatcherDelegate2<V0, V1, R> {
    return PropertyWatcherDelegate2(property0, property1, cleaner, function)
}

/**
 * Delegate property value to a function for which the values of 3 properties are watched
 * @param property0 the first property for which to watch for value changes
 * @param property1 the second property which to watch for value changes
 * @param property2 the third property which to watch for value changes
 * @param function a function that maps the three property values to a new value
 */
fun <V0, V1, V2, R> watchingProperties(
    property0: KProperty0<V0>,
    property1: KProperty0<V1>,
    property2: KProperty0<V2>,
    cleaner: ((R) -> Unit)? = null,
    function: (value0: V0, value1: V1, value2: V2) -> R
): PropertyWatcherDelegate3<V0, V1, V2, R> {
    return PropertyWatcherDelegate3(property0, property1, property2, cleaner, function)
}