package org.openrndr.extra.parameters

import org.openrndr.math.Vector2
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

/*  In case you are here to add an extra annotation type:
    1. Add an annotation class
    2. Add an entry to ParameterType
    3. Add extra fields (if any) to Parameter
    4. Add handling annotation code to listParameters
    5. Add a test in TestAnnotations.kt
 */

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
 * TextParameter annotation for a text parameter
 * @property label a short description of the parameter
 * @property order hint for where to place the parameter in user interfaces
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class TextParameter(val label: String, val order: Int = Integer.MAX_VALUE)

/**
 * ColorParameter annotation for a ColorRGBa parameter
 * @property label a short description of the parameter
 * @property order hint for where to place the parameter in user interfaces
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class ColorParameter(val label: String, val order: Int = Integer.MAX_VALUE)


/**
 * Vector2 annotation for a vector 2 parameter
 * @property label a short description of the parameter
 * @property order hint for where to place the parameter in user interfaces
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class XYParameter(
        val label: String,
        val minX: Double = -1.0,
        val minY: Double = -1.0,
        val maxX: Double = 1.0,
        val maxY: Double = 1.0,
        val precision: Int = 1,
        val keyboardIncrement: Double = 10.0,
        val showAngle: Boolean = false,
        val order: Int = Integer.MAX_VALUE
)



/**
 * ActionParameter annotation for functions without arguments
 * @property label a short description of the parameter
 * @property order hint for where to place the parameter in user interfaces
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ActionParameter(val label: String, val order: Int = Integer.MAX_VALUE)

enum class ParameterType(val annotationClass: KClass<out Annotation>) {
    Double(DoubleParameter::class),
    Int(IntParameter::class),
    Boolean(BooleanParameter::class),
    Action(ActionParameter::class),
    Text(TextParameter::class),
    Color(ColorParameter::class),
    XY(XYParameter::class)
    ;

    companion object {
        fun forParameterAnnotationClass(annotation: Annotation): ParameterType =
                values().find { it.annotationClass == annotation.annotationClass } ?: error("no type for $annotation")

        val parameterAnnotationClasses get() = values().map { it.annotationClass }
    }
}

/**
 * Parameter summary class. This is used by [listParameters] as a way to report parameters in
 * a unified form.
 * @property property a property that received any of the parameter annotations
 * [BooleanParameter], [IntParameter], or [DoubleParameter]
 * @property function a function that received the [ActionParameter]
 * @property label a label that describes the property
 * @property doubleRange a floating point based range in case [DoubleParameter] is used
 * @property intRange an integer range in case [IntParameter] is used
 * @property precision a precision hint in case a [DoubleParameter] annotation is used
 * @property keyboardIncrement how much change the keyboard makes in case a [XYParameter] annotation is used
 * @property order a hint for where in the ui this parameter is placed, lower value means higher priority
 */
class Parameter(
        val parameterType: ParameterType,
        val property: KMutableProperty1<out Any, Any?>?,
        val function: KCallable<Unit>?,
        val label: String,
        val doubleRange: ClosedRange<Double>?,
        val vectorRange: Pair<Vector2, Vector2>?,
        val intRange: IntRange?,
        val precision: Int?,
        val keyboardIncrement: Double?,
        val showAngle: Boolean?,
        val order: Int)

/**
 * List all parameters, (public var properties with a parameter annotation)
 */
fun Any.listParameters(): List<Parameter> {
    return (this::class.memberProperties.filter {
        !it.isConst &&
                it is KMutableProperty1<*, *> &&
                it.visibility == KVisibility.PUBLIC &&
                it.annotations.map { it.annotationClass }.intersect(ParameterType.parameterAnnotationClasses).isNotEmpty()
    }.map {
        val annotations = it.annotations.filter { it.annotationClass in ParameterType.parameterAnnotationClasses }
        var intRange: IntRange? = null
        var doubleRange: ClosedRange<Double>? = null
        var order: Int = Integer.MAX_VALUE
        var label = ""
        var precision: Int? = null
        var type: ParameterType? = null
        var vectorRange = Pair(Vector2(-1.0, -1.0), Vector2(1.0, 1.0))
        var keyboardIncrement: Double? = null
        var showAngle: Boolean? = null

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
                is TextParameter -> {
                    label = it.label
                    order = it.order
                }
                is ColorParameter -> {
                    label = it.label
                    order = it.order
                }
                is XYParameter -> {
                    label = it.label
                    order = it.order
                    vectorRange = Pair(Vector2(it.minX, it.minY), Vector2(it.maxX, it.maxY))
                    keyboardIncrement = it.keyboardIncrement
                    precision = it.precision
                    showAngle = it.showAngle
                }
            }
        }
        Parameter(
                parameterType = type ?: error("no type"),
                property = it as KMutableProperty1<out Any, Any?>,
                function = null,
                label = label,
                doubleRange = doubleRange,
                vectorRange = vectorRange,
                intRange = intRange,
                precision = precision,
                keyboardIncrement = keyboardIncrement,
                showAngle = showAngle,
                order = order
        )
    } + this::class.declaredMemberFunctions.filter {
        it.findAnnotation<ActionParameter>() != null
    }.map {
        val annotation = it.findAnnotation<ActionParameter>()!!
        val label = annotation.label
        val order = annotation.order
        val type = ParameterType.Action
        Parameter(
                type,
                property = null,
                function = it as KCallable<Unit>,
                label = label,
                doubleRange = null,
                intRange = null,
                vectorRange = null,
                precision = null,
                keyboardIncrement = null,
                showAngle = false,
                order = order
        )
    }).sortedBy { it.order }
}

fun Any.title() = this::class.findAnnotation<Description>()?.title

fun Any.description() = this::class.findAnnotation<Description>()?.description
