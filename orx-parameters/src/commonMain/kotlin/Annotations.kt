package org.openrndr.extra.parameters

import org.openrndr.math.Vector2
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1

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
 * OptionParameter annotation for a double precision parameter
 * @property label a short description of the parameter
 * @property order hint for where to place the parameter in user interfaces
 */

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class OptionParameter(val label: String, val order: Int = Int.MAX_VALUE)

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
annotation class DoubleParameter(val label: String, val low: Double, val high: Double, val precision: Int = 3, val order: Int = Int.MAX_VALUE)

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
        val order: Int = Int.MAX_VALUE
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
annotation class IntParameter(val label: String, val low: Int, val high: Int, val order: Int = Int.MAX_VALUE)

/**
 * BooleanParameter annotation for a boolean parameter
 * @property label a short description of the parameter
 * @property order hint for where to place the parameter in user interfaces
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class BooleanParameter(val label: String, val order: Int = Int.MAX_VALUE)

/**
 * TextParameter annotation for a text parameter
 * @property label a short description of the parameter
 * @property order hint for where to place the parameter in user interfaces
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class TextParameter(val label: String, val order: Int = Int.MAX_VALUE)

/**
 * ColorParameter annotation for a ColorRGBa parameter
 * @property label a short description of the parameter
 * @property order hint for where to place the parameter in user interfaces
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class ColorParameter(val label: String, val order: Int = Int.MAX_VALUE)


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
        val order: Int = Int.MAX_VALUE
)

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Vector2Parameter(
        val label: String,
        val min: Double = -1.0,
        val max: Double = 1.0,
        val precision: Int = 2,
        val order: Int = Int.MAX_VALUE
)

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Vector3Parameter(
        val label: String,
        val min: Double = -1.0,
        val max: Double = 1.0,
        val precision: Int = 2,
        val order: Int = Int.MAX_VALUE
)

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Vector4Parameter(
        val label: String,
        val min: Double = -1.0,
        val max: Double = 1.0,
        val precision: Int = 2,
        val order: Int = Int.MAX_VALUE
)

/**
 * ActionParameter annotation for functions without arguments
 * @property label a short description of the parameter
 * @property order hint for where to place the parameter in user interfaces
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ActionParameter(val label: String, val order: Int = Int.MAX_VALUE)

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
    Vector4(Vector4Parameter::class),
    Option(OptionParameter::class)
    ;

    companion object {

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
//        val optionEnum: Enum<*>,
        val order: Int)
//</editor-fold>
//<editor-fold desc="4. Add handling annotation code to listParameters" defaultstate="collapsed">
/**
 * List all parameters, (public var properties with a parameter annotation)
 */

//</editor-fold>


expect fun Any.title(): String?

expect fun Any.description(): String?

