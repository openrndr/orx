package org.openrndr.extra.parameters

import kotlinx.serialization.Serializable
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4

/**
 * The value of a parameter as it is saved and loaded, see `ParameterBindings`: the field that matches the type of
 * the parameter is set, the others are null.
 */
@Serializable
class ParameterValue(
    var doubleValue: Double? = null,
    var intValue: Int? = null,
    var booleanValue: Boolean? = null,
    var colorValue: ColorRGBa? = null,
    var vector2Value: Vector2? = null,
    var vector3Value: Vector3? = null,
    var vector4Value: Vector4? = null,
    var doubleListValue: MutableList<Double>? = null,
    var intListValue: MutableList<Int>? = null,
    var stringListValue: MutableList<String>? = null,
    var textValue: String? = null,
    var optionValue: String? = null,
    var minValue: Double? = null,
    var maxValue: Double? = null,
    var selectedIndex: Int? = null,
    var listElementType: String? = null
)
