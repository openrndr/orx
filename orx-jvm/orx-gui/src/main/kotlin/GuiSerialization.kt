package org.openrndr.extra.gui

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.parameters.ParameterType
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import org.openrndr.panel.collections.SelectableList
import org.openrndr.panel.collections.SelectableMutableList
import kotlin.reflect.KMutableProperty1

/*
Serialization and deserialization of the parameter values of the objects tracked by a [GUI].
 */

private val logger = KotlinLogging.logger { }

/**
 * Convert the current parameter values of the tracked objects into [GUI.ParameterValue]s, keyed by compartment label
 * and parameter name.
 */
internal fun Map<LabeledObject, TrackedObjectBinding>.toParameterValues(): Map<String, Map<String, GUI.ParameterValue>> {
    fun <T> KMutableProperty1<out Any, Any?>?.qget(obj: Any): T {
        return (this as KMutableProperty1<Any, T>).get(obj)
    }

    return entries.associate { (lo, b) ->
        Pair(lo.label, b.parameterControls.keys.associate { k ->
            Pair(
                k.property?.name ?: k.function?.name
                ?: error("no name"), when (k.parameterType) {
                    /* 3) setup serializers */
                    ParameterType.Double -> GUI.ParameterValue(
                        doubleValue = k.property.qget(lo.obj) as Double,
                        minValue = k.doubleRange?.start,
                        maxValue = k.doubleRange?.endInclusive
                    )

                    ParameterType.Int -> GUI.ParameterValue(
                        intValue = k.property.qget(lo.obj) as Int,
                        minValue = k.intRange?.start?.toDouble(),
                        maxValue = k.intRange?.endInclusive?.toDouble()
                    )

                    ParameterType.Action -> GUI.ParameterValue()
                    ParameterType.Color -> GUI.ParameterValue(colorValue = k.property.qget(lo.obj) as ColorRGBa)
                    ParameterType.Text -> GUI.ParameterValue(textValue = k.property.qget(lo.obj) as String)
                    ParameterType.Boolean -> GUI.ParameterValue(booleanValue = k.property.qget(lo.obj) as Boolean)
                    ParameterType.XY -> GUI.ParameterValue(vector2Value = k.property.qget(lo.obj) as Vector2)
                    ParameterType.DoubleList -> GUI.ParameterValue(
                        doubleListValue = k.property.qget(
                            lo.obj
                        ) as MutableList<Double>,
                        minValue = k.doubleRange?.start,
                        maxValue = k.doubleRange?.endInclusive
                    )

                    ParameterType.Vector2 -> GUI.ParameterValue(
                        vector2Value = k.property.qget(lo.obj) as Vector2,
                        minValue = k.doubleRange?.start,
                        maxValue = k.doubleRange?.endInclusive
                    )

                    ParameterType.Vector3 -> GUI.ParameterValue(
                        vector3Value = k.property.qget(lo.obj) as Vector3,
                        minValue = k.doubleRange?.start,
                        maxValue = k.doubleRange?.endInclusive
                    )

                    ParameterType.Vector4 -> GUI.ParameterValue(
                        vector4Value = k.property.qget(lo.obj) as Vector4,
                        minValue = k.doubleRange?.start,
                        maxValue = k.doubleRange?.endInclusive
                    )

                    ParameterType.Path -> GUI.ParameterValue(textValue = k.property.qget(lo.obj) as String)

                    ParameterType.Option -> GUI.ParameterValue(optionValue = (k.property.qget(lo.obj) as Enum<*>).name)

                    ParameterType.List -> {
                        val selectableList = k.property.qget(lo.obj) as SelectableList<*>
                        val elementType = when (val first = selectableList.firstOrNull()) {
                            is String -> "String"
                            is Int -> "Int"
                            is Double -> "Double"
                            else -> "String"
                        }
                        GUI.ParameterValue(
                            selectedIndex = selectableList.selectedIndex,
                            listElementType = elementType
                        )
                    }

                    ParameterType.MutableList -> {
                        val selectableMutableList = k.property.qget(lo.obj) as SelectableMutableList<*>
                        val elementType = when (val first = selectableMutableList.firstOrNull()) {
                            is String -> "String"
                            is Int -> "Int"
                            is Double -> "Double"
                            else -> "String"
                        }
                        val elements = selectableMutableList.map {
                            when (it) {
                                is String -> it
                                is Int -> it
                                is Double -> it
                                else -> it.toString()
                            }
                        }
                        GUI.ParameterValue(
                            selectedIndex = selectableMutableList.selectedIndex,
                            listElementType = elementType,
                            stringListValue = if (elementType == "String") elements as? MutableList<String> else null,
                            intListValue = if (elementType == "Int") elements as? MutableList<Int> else null,
                            doubleListValue = if (elementType == "Double") elements as? MutableList<Double> else null
                        )
                    }
                }
            )
        })
    }
}

/**
 * Apply [labeledValues] to the parameters of the tracked objects. Values for unknown compartments or parameters are
 * ignored.
 */
internal fun Map<LabeledObject, TrackedObjectBinding>.applyParameterValues(
    labeledValues: Map<String, Map<String, GUI.ParameterValue>>
) {
    fun <T> KMutableProperty1<out Any, Any?>?.qset(obj: Any, value: T) =
        (this as KMutableProperty1<Any, T>).set(obj, value)

    fun KMutableProperty1<out Any, Any?>?.enumSet(obj: Any, value: String) {
        val v = (this as KMutableProperty1<Any, Enum<*>>).get(obj)

        try {
            @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
            val enumValue =
                (v as java.lang.Enum<*>).getDeclaringClass().getEnumConstants().find { it.name == value }
                    ?: error("cannot map value $value to enum")
            (this as KMutableProperty1<Any, Enum<*>>).set(obj, enumValue)
        } catch (e: Throwable) {
            logger.warn { e.message }
        }
    }

    labeledValues.forEach { (label, ps) ->
        keys.find { it.label == label }?.let { lo ->
            val binding = this[lo]!!
            ps.forEach { (parameterName, parameterValue) ->
                binding.parameters.find { it.property?.name == parameterName }?.let { parameter ->
                    when (parameter.parameterType) {
                        /* 4) Set up deserializers */
                        ParameterType.Double -> parameterValue.doubleValue?.let {
                            parameter.property.qset(lo.obj, it)
                        }

                        ParameterType.Int -> parameterValue.intValue?.let {
                            parameter.property.qset(lo.obj, it)
                        }

                        ParameterType.Text -> parameterValue.textValue?.let {
                            parameter.property.qset(lo.obj, it)
                        }

                        ParameterType.Color -> parameterValue.colorValue?.let {
                            parameter.property.qset(lo.obj, it)
                        }

                        ParameterType.XY -> parameterValue.vector2Value?.let {
                            parameter.property.qset(lo.obj, it)
                        }

                        ParameterType.DoubleList -> parameterValue.doubleListValue?.let {
                            parameter.property.qset(lo.obj, it)
                        }

                        ParameterType.Boolean -> parameterValue.booleanValue?.let {
                            parameter.property.qset(lo.obj, it)
                        }

                        ParameterType.Vector2 -> parameterValue.vector2Value?.let {
                            parameter.property.qset(lo.obj, it)
                        }

                        ParameterType.Vector3 -> parameterValue.vector3Value?.let {
                            parameter.property.qset(lo.obj, it)
                        }

                        ParameterType.Vector4 -> parameterValue.vector4Value?.let {
                            parameter.property.qset(lo.obj, it)
                        }

                        ParameterType.Option -> parameterValue.optionValue?.let {
                            parameter.property.enumSet(lo.obj, it)
                        }

                        ParameterType.Path -> parameterValue.textValue?.let {
                            parameter.property.qset(lo.obj, it)
                        }

                        ParameterType.List -> {
                            parameterValue.selectedIndex?.let { selectedIndex ->
                                parameterValue.listElementType?.let { elementType ->
                                    val prop = parameter.property!!
                                    val state = compartmentState()[lo.label]
                                    if (state != null) {
                                        state.parameterValues["${prop.name}_selectedIndex"] = selectedIndex
                                    }
                                }
                            }
                        }

                        ParameterType.MutableList -> {
                            parameterValue.selectedIndex?.let { selectedIndex ->
                                parameterValue.listElementType?.let { elementType ->
                                    val elements = when (elementType) {
                                        "String" -> parameterValue.stringListValue
                                        "Int" -> parameterValue.intListValue
                                        "Double" -> parameterValue.doubleListValue
                                        else -> null
                                    }
                                    elements?.let { elementList ->
                                        val prop = parameter.property!!
                                        val state = compartmentState()[lo.label]
                                        if (state != null) {
                                            state.parameterValues["${prop.name}_selectedIndex"] = selectedIndex
                                            state.parameterValues["${prop.name}_elements"] = elementList
                                        }
                                    }
                                }
                            }
                        }

                        ParameterType.Action -> {
                            // intentionally do nothing
                        }
                    }
                }
            }
        }
    }
}

internal fun parameterValuesToJson(labeledValues: Map<String, Map<String, GUI.ParameterValue>>): String =
    Gson().toJson(labeledValues)

internal fun parameterValuesFromJson(json: String): Map<String, Map<String, GUI.ParameterValue>> {
    val typeToken = object : TypeToken<Map<String, Map<String, GUI.ParameterValue>>>() {}
    return try {
        Gson().fromJson(json, typeToken.type)
    } catch (e: JsonSyntaxException) {
        println("could not parse json: $json")
        throw e
    }
}
