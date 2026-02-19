package org.openrndr.extra.gui.custom

import org.openrndr.color.ColorRGBa
import org.openrndr.extra.parameters.ParameterType
import org.openrndr.extra.parameters.listParameters
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import org.openrndr.panel.elements.Div
import org.openrndr.panel.elements.Element
import org.openrndr.panel.elements.Range
import org.openrndr.panel.elements.bind
import org.openrndr.panel.elements.button
import org.openrndr.panel.elements.colorpickerButton
import org.openrndr.panel.elements.div
import org.openrndr.panel.elements.slider
import org.openrndr.panel.elements.slidersVector2
import org.openrndr.panel.elements.slidersVector3
import org.openrndr.panel.elements.slidersVector4
import org.openrndr.panel.elements.textfield
import org.openrndr.panel.elements.toggle
import kotlin.reflect.KMutableProperty1

fun Element.uiForParameters(obj: Any): Div {
    return div {
        for (parameter in obj.listParameters().sortedBy { it.order }) {
            when (parameter.parameterType) {

                ParameterType.Action -> {
                    button {
                        label = parameter.label
                        events.clicked.listen {
                            parameter.function!!.call(obj)
                        }
                    }
                }
                ParameterType.Int -> {
                    slider {
                        range = Range(parameter.intRange!!.start.toDouble(), parameter.intRange!!.endInclusive.toDouble())
                        label = parameter.label
                        precision = 0
                        value = (parameter.property as KMutableProperty1<Any, Int>).get(obj).toDouble()
                        bind(obj, parameter.property as KMutableProperty1<Any, Int>)
                    }
                }
                ParameterType.Double -> {
                    slider {
                        range = Range(parameter.doubleRange!!.start, parameter.doubleRange!!.endInclusive)
                        label = parameter.label
                        precision = parameter.precision!!
                        value = (parameter.property as KMutableProperty1<Any, Double>).get(obj)
                        bind(obj, parameter.property as KMutableProperty1<Any, Double>)
                    }
                }
                ParameterType.Color -> {
                    colorpickerButton {
                        label = parameter.label
                        color = (parameter.property as KMutableProperty1<Any, ColorRGBa>).get(obj)
                        bind(obj, parameter.property as KMutableProperty1<Any, ColorRGBa>)
                    }
                }
                ParameterType.Boolean -> {
                    toggle {
                        label = parameter.label
                        value = (parameter.property as KMutableProperty1<Any, Boolean>).get(obj)
                        bind(obj, parameter.property as KMutableProperty1<Any, Boolean>)
                    }
                }
                ParameterType.Text -> {
                    textfield {
                        label = parameter.label
                        value = (parameter.property as KMutableProperty1<Any, String>).get(obj)
                        bind(obj, parameter.property as KMutableProperty1<Any, String>)
                    }
                }

                ParameterType.Vector2 -> {
                    slidersVector2 {
                        label = parameter.label
                        range = parameter.doubleRange!!
                        precision = parameter.precision!!
                        value = (parameter.property as KMutableProperty1<Any, Vector2>).get(obj)
                        bind(obj, parameter.property as KMutableProperty1<Any, Vector2>)
                    }
                }

                ParameterType.Vector3 -> {
                    slidersVector3 {
                        label = parameter.label
                        range = parameter.doubleRange!!
                        precision = parameter.precision!!
                        value = (parameter.property as KMutableProperty1<Any, Vector3>).get(obj)
                        bind(obj, parameter.property as KMutableProperty1<Any, Vector3>)
                    }
                }

                ParameterType.Vector4 -> {
                    slidersVector4 {
                        label = parameter.label
                        range = parameter.doubleRange!!
                        precision = parameter.precision!!
                        value = (parameter.property as KMutableProperty1<Any, Vector4>).get(obj)
                        bind(obj, parameter.property as KMutableProperty1<Any, Vector4>)
                    }
                }
                else -> {}
            }
        }
    }
}