package org.openrndr.extra.parameters

import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Description(val description: String)

/**
 * DoubleParameter annotation for a double precision Filter parameter
 * @property label a short description of the parameter
 * @property low the lowest value this parameter should be assigned
 * @property high the highest value this parameter should be assigned
 * @property precision a hint for precision in user interfaces
 * @property order hint for where to place the parameter in user interfaces
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class DoubleParameter(val label: String, val low: Double, val high: Double, val precision: Int = 3, val order: Int = Integer.MAX_VALUE)

/**
 * IntParameter annotation for an integer Filter parameter
 * @property label a short description of the parameter
 * @property low the lowest value this parameter should be assigned
 * @property high the highest value this parameter should be assigned
 * @property order hint for where to place the parameter in user interfaces
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class IntParameter(val label: String, val low: Int, val high: Int, val order: Int = Integer.MAX_VALUE)

/**
 * BooleanParameter annotation for an integer Filter parameter
 * @property label a short description of the parameter
 * @property order hint for where to place the parameter in user interfaces
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class BooleanParameter(val label: String, val order: Int = Integer.MAX_VALUE)

/**
 * Parameter summary class. This is used by [listParameters] as a way to report parameters in
 * a unified form.
 * @property property the property that received any of the parameter annotations
 * [BooleanParameter], [IntParameter], or [DoubleParameter]
 * @property label a label that describes the property
 * @property doubleRange a floating point based range in case [DoubleParameter] is used
 * @property intRange an integer range in case [IntParameter] is used
 * @property precision a precision hint in case a [DoubleParameter] annotation is used
 * @property order a hint for where in the ui this parameter is placed, lower value means higher priority
 */
class Parameter(
        val property: KMutableProperty1<out Any, Any?>,
        val label: String,
        val doubleRange: ClosedRange<Double>?,
        val intRange: IntRange?,
        val precision: Int?,
        val order: Int)

/**
 * List all parameters, (public var properties with a parameter annotation)
 */
fun Any.listParameters(): List<Parameter> {
    return this::class.declaredMemberProperties.filter {
        !it.isConst &&
                it.visibility == KVisibility.PUBLIC &&
                (it.findAnnotation<BooleanParameter>() != null ||
                        it.findAnnotation<IntParameter>() != null ||
                        it.findAnnotation<DoubleParameter>() != null)
    }.map {
        val annotations = listOf(it.findAnnotation<BooleanParameter>(), it.findAnnotation<IntParameter>(), it.findAnnotation<DoubleParameter>()).filterNotNull()
        var intRange: IntRange? = null
        var doubleRange: ClosedRange<Double>? = null
        var order: Int = Integer.MAX_VALUE
        var label: String = ""
        var precision: Int? = null

        annotations.forEach {
            when(it) {
                is BooleanParameter -> {
                    label = it.label
                    order = it.order
                }
                is DoubleParameter -> {
                    label = it.label
                    doubleRange = it.low .. it.high
                    precision = it.precision
                    order = it.order
                }
                is IntParameter -> {
                    label = it.label
                    intRange = it.low .. it.high
                    order = it.order
                }
            }
        }
        Parameter(it as KMutableProperty1<out Any, Any?>, label, doubleRange, intRange, precision, order)
    }.sortedBy { it.order }
}