package org.openrndr.extra.parameters

import org.openrndr.math.Vector2
import kotlin.reflect.KCallable
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.memberProperties

fun ParameterType.Companion.forParameterAnnotationClass(annotation: Annotation): ParameterType =
        ParameterType.values().find { it.annotationClass == annotation.annotationClass } ?: error("no type for $annotation")

val ParameterType.Companion.parameterAnnotationClasses get() = ParameterType.values().map { it.annotationClass }

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
    }).sortedBy { it.order }
}

actual fun Any.title() = this::class.findAnnotation<Description>()?.title

actual fun Any.description(): String? = this::class.findAnnotation<Description>()?.description


