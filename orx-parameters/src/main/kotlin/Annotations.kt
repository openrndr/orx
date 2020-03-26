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

//<editor-fold desc="1. Add an annotation class" defaultstate="collapsed">
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
 * DoubleListParameter annotation for a double precision parameter
 * @property label a short description of the parameter
 * @property low the lowest value this parameter should be assigned
 * @property high the highest value this parameter should be assigned
 * @property minimumListLength the minimum amount of entries the annotated list should contain
 * @property maximumListLength the maximum amount of entries the annotated list should contain
 * @property precision a hint for precision in user interfaces
 * @property order hint for where to place the parameter in user interfaces
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class DoubleListParameter(
        val label: String,
        val low: Double = -1.0,
        val high: Double = 1.0,
        val minimumListLength: Int = 1,
        val maximumListLength: Int = 16,
        val precision: Int = 3,
        val order: Int = Integer.MAX_VALUE
)

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
        val maxX: Double = 1.0,
        val minY: Double = -1.0,
        val maxY: Double = 1.0,
        val precision: Int = 2,
        val showVector: Boolean = false,
        val invertY: Boolean = true,
        val order: Int = Integer.MAX_VALUE
)

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Vector2Parameter(
        val label: String,
        val min: Double = -1.0,
        val max: Double = 1.0,
        val precision: Int = 2,
        val order: Int = Integer.MAX_VALUE
)

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Vector3Parameter(
        val label: String,
        val min: Double = -1.0,
        val max: Double = 1.0,
        val precision: Int = 2,
        val order: Int = Integer.MAX_VALUE
)

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Vector4Parameter(
        val label: String,
        val min: Double = -1.0,
        val max: Double = 1.0,
        val precision: Int = 2,
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

//</editor-fold>
//<editor-fold desc="2. Add an entry to ParameterType" id="add-parameter-type" defaultstate="collapsed">
enum class ParameterType(val annotationClass: KClass<out Annotation>) {
    Double(DoubleParameter::class),
    Int(IntParameter::class),
    Boolean(BooleanParameter::class),
    Action(ActionParameter::class),
    Text(TextParameter::class),
    Color(ColorParameter::class),
    XY(XYParameter::class),
    DoubleList(DoubleListParameter::class),
    Vector2(Vector2Parameter::class),
    Vector3(Vector3Parameter::class),
    Vector4(Vector4Parameter::class)
    ;

    companion object {
        fun forParameterAnnotationClass(annotation: Annotation): ParameterType =
                values().find { it.annotationClass == annotation.annotationClass } ?: error("no type for $annotation")

        val parameterAnnotationClasses get() = values().map { it.annotationClass }
    }
}
//</editor-fold>
//<editor-fold desc="3. Add extra fields (if any) to Parameter" defaultstate="collapsed">
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
 * @property invertY should the y-axis of [XYParameter] be inverted?
 * @property order a hint for where in the ui this parameter is placed, lower value means higher priority
 */
class Parameter(
        val parameterType: ParameterType,
        val property: KMutableProperty1<out Any, Any?>?,
        val function: KCallable<Unit>?,
        val label: String,
        val doubleRange: ClosedRange<Double>?,
        val vectorRange: Pair<Vector2, Vector2>?,
        val sizeRange: ClosedRange<Int>?,
        val intRange: IntRange?,
        val precision: Int?,
        val invertY: Boolean?,
        val showVector: Boolean?,
        val order: Int)
//</editor-fold>
//<editor-fold desc="4. Add handling annotation code to listParameters" defaultstate="collapsed">
/**
 * List all parameters, (public var properties with a parameter annotation)
 */
fun Any.listParameters(): List<Parameter> {
    return (this::class.memberProperties.filter { property ->
        !property.isConst &&
                property is KMutableProperty1<*, *> &&
                property.visibility == KVisibility.PUBLIC &&
                property.annotations.map { it.annotationClass }.intersect(ParameterType.parameterAnnotationClasses).isNotEmpty()
    }.map { property ->
        val annotations = property.annotations.filter { it.annotationClass in ParameterType.parameterAnnotationClasses }
        var intRange: IntRange? = null
        var doubleRange: ClosedRange<Double>? = null
        var sizeRange: ClosedRange<Int>? = null
        var order: Int = Integer.MAX_VALUE
        var label = ""
        var precision: Int? = null
        var type: ParameterType? = null
        var vectorRange = Pair(Vector2(-1.0, -1.0), Vector2(1.0, 1.0))
        var invertY: Boolean? = null
        var showVector: Boolean? = null

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
                    precision = it.precision
                    invertY = it.invertY
                    showVector = it.showVector
                }
                is DoubleListParameter -> {
                    label = it.label
                    order = it.order
                    doubleRange = it.low..it.high
                    precision = it.precision
                    sizeRange = it.minimumListLength..it.maximumListLength
                }
                is Vector2Parameter -> {
                    label = it.label
                    order = it.order
                    doubleRange = it.min..it.max
                    precision = it.precision
                }
                is Vector3Parameter -> {
                    label = it.label
                    order = it.order
                    doubleRange = it.min..it.max
                    precision = it.precision
                }
                is Vector4Parameter -> {
                    label = it.label
                    order = it.order
                    doubleRange = it.min..it.max
                    precision = it.precision
                }
            }
        }
        Parameter(
                parameterType = type ?: error("no type"),
                property = property as KMutableProperty1<out Any, Any?>,
                function = null,
                label = label,
                doubleRange = doubleRange,
                vectorRange = vectorRange,
                sizeRange = sizeRange,
                intRange = intRange,
                precision = precision,
                showVector = showVector,
                invertY = invertY,
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
                sizeRange = null,
                vectorRange = null,
                precision = null,
                showVector = null,
                invertY = null,
                order = order
        )
    }).sortedBy { it.order }
}
//</editor-fold>

fun Any.title() = this::class.findAnnotation<Description>()?.title

fun Any.description() = this::class.findAnnotation<Description>()?.description
