package org.openrndr.extra.propertywatchers

import org.openrndr.events.Event
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0

/**
 * Property watcher delegate
 * @see watchingProperty
 * @since 0.4.3
 */
class PropertyWatcherDelegate<V, R>(
    private val property: KProperty0<V>,
    private val valueChangedEvent: Event<R>? = null,
    private val cleaner: ((R) -> Unit)? = null,
    val function: (V) -> R
) {
    private var watchValue: V? = null
    private var value: R? = null
    operator fun getValue(any: Any?, property: KProperty<*>): R {
        val ref = this.property.get()
        if (watchValue != ref) {
            watchValue = ref
            value?.let {
                cleaner?.invoke(it)
            }
            value = function(ref)
            valueChangedEvent?.trigger(value ?: error("no value"))
        }
        return value ?: error("no value?")
    }
}

/**
 * Property watcher delegate
 * @see watchingProperties
 * @since 0.4.3
 */
class PropertyWatcherDelegate2<V0, V1, R>(
    private val toWatch0: KProperty0<V0>,
    private val toWatch1: KProperty0<V1>,
    private val valueChangedEvent: Event<R>? = null,
    private val cleaner: ((R) -> Unit)? = null,
    private val function: (V0, V1) -> R
) {
    private var watchValue0: V0? = null
    private var watchValue1: V1? = null
    private var value: R? = null
    operator fun getValue(any: Any?, property: KProperty<*>): R {
        val ref0 = toWatch0.get()
        val ref1 = toWatch1.get()
        if (watchValue0 != ref0 || watchValue1 != ref1) {
            watchValue0 = ref0
            watchValue1 = ref1

            value?.let {
                cleaner?.invoke(it)
            }
            value = function(ref0, ref1)
            valueChangedEvent?.trigger(value ?: error("no value"))
        }
        return value ?: error("no value?")
    }
}

/**
 * Property watcher delegate
 * @see watchingProperties
 * @since 0.4.3
 */
class PropertyWatcherDelegate3<V0, V1, V2, R>(
    private val toWatch0: KProperty0<V0>,
    private val toWatch1: KProperty0<V1>,
    private val toWatch2: KProperty0<V2>,
    private val valueChangedEvent: Event<R>? = null,
    private val cleaner: ((R) -> Unit)? = null,
    private val function: (V0, V1, V2) -> R
) {
    private var watchValue0: V0? = null
    private var watchValue1: V1? = null
    private var watchValue2: V2? = null
    private var value: R? = null
    operator fun getValue(any: Any?, property: KProperty<*>): R {
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
            valueChangedEvent?.trigger(value ?: error("no value"))
        }
        return value ?: error("no value?")
    }
}


/**
 * Delegate property value to a function for which the value of a single property is watched
 * @param property the property for which to watch for value changes
 * @param valueChangedEvent an optional event that is triggered on value change
 * @param cleaner an optional cleaner function that is invoked to clean up the old value
 * @param function a function that maps the property value to a new value
 * @since 0.4.3
 */
fun <V, R> watchingProperty(
    property: KProperty0<V>,
    valueChangedEvent: Event<R>? = null,
    cleaner: ((R) -> Unit)? = null,
    function: (value: V) -> R
): PropertyWatcherDelegate<V, R> = PropertyWatcherDelegate(property, valueChangedEvent, cleaner, function)

/**
 * Delegate property value to a function for which the values of 2 properties are watched
 * @param property0 the first property for which to watch for value changes
 * @param property1 the second property which to watch for value changes
 * @param function a function that maps the two property values to a new value
 * @since 0.4.3
 */
fun <V0, V1, R> watchingProperties(
    property0: KProperty0<V0>,
    property1: KProperty0<V1>,
    valueChangedEvent: Event<R>? = null,
    cleaner: ((R) -> Unit)?,
    function: (value0: V0, value1: V1) -> R
): PropertyWatcherDelegate2<V0, V1, R> =
    PropertyWatcherDelegate2(property0, property1, valueChangedEvent, cleaner, function)

/**
 * Delegate property value to a function for which the values of 3 properties are watched
 * @param property0 the first property for which to watch for value changes
 * @param property1 the second property which to watch for value changes
 * @param property2 the third property which to watch for value changes
 * @param function a function that maps the three property values to a new value
 * @since 0.4.3
 */
fun <V0, V1, V2, R> watchingProperties(
    property0: KProperty0<V0>,
    property1: KProperty0<V1>,
    property2: KProperty0<V2>,
    valueChangedEvent: Event<R>? = null,
    cleaner: ((R) -> Unit)? = null,
    function: (value0: V0, value1: V1, value2: V2) -> R
): PropertyWatcherDelegate3<V0, V1, V2, R> =
    PropertyWatcherDelegate3(property0, property1, property2, valueChangedEvent, cleaner, function)