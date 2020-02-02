package org.openrndr.extra.parameters

import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Description(val title: String, val description: String = "")

/**
 * DoubleParameter annotation for a double precision parameter
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
 * IntParameter annotation for an integer parameter
 * @property label a short description of the parameter
 * @property low the lowest value this parameter should be assigned
 * @property high the highest value this parameter should be assigned
 * @property order hint for where to place the parameter in user interfaces
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class IntParameter(val label: String, val low: Int, val high: Int, val order: Int = Integer.MAX_VALUE)

/**
 * BooleanParameter annotation for a boolean parameter
 * @property label a short description of the parameter
 * @property order hint for where to place the parameter in user interfaces
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class BooleanParameter(val label: String, val order: Int = Integer.MAX_VALUE)

/**
 * Text annotation for a text parameter
 * @property label a short description of the parameter
 * @property order hint for where to place the parameter in user interfaces
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class TextParameter(val label: String, val order: Int = Integer.MAX_VALUE)


/**
 * ButtonParameter annotation for button parameter
 * @property label a short description of the parameter
 * @property order hint for where to place the parameter in user interfaces
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class ButtonParameter(val label: String, val order: Int = Integer.MAX_VALUE)

enum class ParameterType(val annotation: KClass<out Annotation>) {
    Double(DoubleParameter::class),
    Int(IntParameter::class),
    Boolean(BooleanParameter::class),
    Button(ButtonParameter::class),
    Text(TextParameter::class)
    ;

    companion object {
        fun forParameterAnnotationClass(annotation: Annotation): ParameterType {
            return when (annotation) {
                is DoubleParameter -> Double
                is IntParameter -> Int
                is BooleanParameter -> Boolean
                is ButtonParameter -> Button
                is TextParameter -> Text
                else -> error("no type for $annotation")
            }
        }
    }
}


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
        val parameterType: ParameterType,
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
                        it.findAnnotation<DoubleParameter>() != null ||
                        it.findAnnotation<ButtonParameter>() != null) ||
                it.findAnnotation<TextParameter>() != null
    }.map {
        val annotations = listOfNotNull(it.findAnnotation<BooleanParameter>(),
                it.findAnnotation<IntParameter>(),
                it.findAnnotation<DoubleParameter>(),
                it.findAnnotation<ButtonParameter>(),
                it.findAnnotation<TextParameter>())
        var intRange: IntRange? = null
        var doubleRange: ClosedRange<Double>? = null
        var order: Int = Integer.MAX_VALUE
        var label = ""
        var precision: Int? = null
        var type: ParameterType? = null

        annotations.forEach {
            type = ParameterType.forParameterAnnotationClass(it)
            when (it) {
                is BooleanParameter -> {
                    label = it.label
                    order = it.order
                }
                is DoubleParameter -> {
                    label = it.label
                    doubleRange = it.low..it.high
                    precision = it.precision
                    order = it.order
                }
                is IntParameter -> {
                    label = it.label
                    intRange = it.low..it.high
                    order = it.order
                }
                is ButtonParameter -> {
                    label = it.label
                    order = it.order
                }
                is TextParameter -> {
                    label = it.label
                    order = it.order
                }
            }
        }
        Parameter(type
                ?: error("no type"), it as KMutableProperty1<out Any, Any?>, label, doubleRange, intRange, precision, order)
    }.sortedBy { it.order }
}

fun Any.title() = this::class.findAnnotation<Description>()?.title

fun Any.description() = this::class.findAnnotation<Description>()?.description
