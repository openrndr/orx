package org.openrndr.extra.parameters

import org.openrndr.math.Vector2
import kotlin.reflect.KCallable
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.memberProperties

fun ParameterType.Companion.forParameterAnnotationClass(annotation: Annotation): ParameterType =
        ParameterType.values().find { it.annotationClass == annotation.annotationClass } ?: error("no type for $annotation")

val ParameterType.Companion.parameterAnnotationClasses get() = ParameterType.values().map { it.annotationClass }

/** the annotation classes of parameters that are read and written through a mutable property */
private val valueAnnotationClasses get() = ParameterType.parameterAnnotationClasses - NestedParameter::class

/**
 * List all parameters, (public var properties with a parameter annotation, functions with an [ActionParameter]
 * annotation and public properties with a [NestedParameter] annotation)
 */
fun Any.listParameters(): List<Parameter> = listParameters(mutableListOf())

/**
 * @param path the objects that contain this object through nested parameters, used to detect cycles
 */
private fun Any.listParameters(path: MutableList<Any>): List<Parameter> {
    path.add(this)
    val nested = this::class.memberProperties.filter { property ->
        property.visibility == KVisibility.PUBLIC && property.findAnnotation<NestedParameter>() != null
    }.map { property ->
        val annotation = property.findAnnotation<NestedParameter>()!!
        @Suppress("UNCHECKED_CAST")
        val value = (property as KProperty1<Any, Any?>).get(this)
        require(path.none { it === value }) {
            "nested parameter '${property.name}' holds an object that contains it"
        }
        Parameter(
            ParameterType.Nested,
            property = null,
            function = null,
            label = annotation.name,
            doubleRange = null,
            intRange = null,
            sizeRange = null,
            vectorRange = null,
            precision = null,
            showVector = null,
            invertY = null,
            absolutePath = null,
            pathContext = null,
            pathExtensions = null,
            pathIsDirectory = null,
            order = annotation.order,
            nestedProperty = property,
            nestedParameters = value?.listParameters(path) ?: emptyList()
        )
    }
    path.removeAt(path.lastIndex)

    return (this::class.memberProperties.filter { property ->
        !property.isConst &&
                property is KMutableProperty1<*, *> &&
                property.visibility == KVisibility.PUBLIC &&
                property.annotations.map { it.annotationClass }.intersect(valueAnnotationClasses.toSet()).isNotEmpty()
    }.map { property ->
        val annotations = property.annotations.filter { it.annotationClass in valueAnnotationClasses }
        var intRange: IntRange? = null
        var doubleRange: ClosedRange<Double>? = null
        var sizeRange: ClosedRange<Int>? = null
        var order: Int = Int.MAX_VALUE
        var label = ""
        var precision: Int? = null
        var type: ParameterType? = null
        var vectorRange = Pair(Vector2(-1.0, -1.0), Vector2(1.0, 1.0))
        var invertY: Boolean? = null
        var showVector: Boolean? = null
        var absolutePath: Boolean? = null
        var pathContext: String? = null
        var pathExtensions: Array<String>? = null
        var pathIsDirectory: Boolean? = null

        for (it in annotations) {
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
                is OptionParameter -> {
                    label = it.label
                    order = it.order
                }
                is PathParameter -> {
                    label = it.label
                    absolutePath =  it.absolute
                    pathContext = it.context
                    pathExtensions = it.extensions
                    pathIsDirectory = it.directory
                }
                is ListParameter -> {
                    label = it.label
                    order = it.order
                }
                is MutableListParameter -> {
                    label = it.label
                    order = it.order
                }
            }
        }
        @Suppress("UNCHECKED_CAST")
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
            absolutePath = absolutePath,
            pathContext =  pathContext,
            pathExtensions = pathExtensions,
            pathIsDirectory = pathIsDirectory,
            order = order,

        )
    } + this::class.declaredMemberFunctions.filter {
        it.findAnnotation<ActionParameter>() != null
    }.map {
        val annotation = it.findAnnotation<ActionParameter>()!!
        val label = annotation.label
        val order = annotation.order
        val type = ParameterType.Action
        @Suppress("UNCHECKED_CAST")
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
            absolutePath = null,
            pathContext = null,
            pathExtensions = null,
            pathIsDirectory = null,
            order = order
        )
    } + nested).sortedBy { it.order }
}

actual fun Any.title() = this::class.findAnnotation<Description>()?.title

actual fun Any.description(): String? = this::class.findAnnotation<Description>()?.description


