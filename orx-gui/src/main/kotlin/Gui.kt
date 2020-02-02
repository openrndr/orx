package org.openrndr.extra.gui

import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.parameters.*
import org.openrndr.panel.ControlManager
import org.openrndr.panel.controlManager
import org.openrndr.panel.elements.*
import org.openrndr.panel.style.*
import kotlin.reflect.KMutableProperty1

@Suppress("unused", "UNCHECKED_CAST")
class GUI : Extension {
    private var onChangeListener: ((name: String, value: Any?) -> Unit)? = null
    override var enabled = true

    private lateinit var panel: ControlManager

    fun onChange(listener: (name: String, value: Any?) -> Unit) {
        onChangeListener = listener
    }

    override fun setup(program: Program) {
        panel = program.controlManager {
            styleSheet(has class_ "container") {
                this.display = Display.FLEX
                this.flexDirection = FlexDirection.Row
                this.width = 200.px
                this.height = 100.percent
            }

            styleSheet(has class_ "sidebar") {
                this.width = 200.px
                this.paddingBottom = 20.px
                this.paddingTop = 10.px
                this.paddingLeft = 10.px
                this.paddingRight = 10.px
                this.marginRight = 2.px
                this.height = 100.percent
                this.background = Color.RGBa(ColorRGBa.GRAY.copy(a = 0.2))
            }

            styleSheet(has type "dropdown-button") {
                this.width = 175.px
            }

            styleSheet(has class_ "filter") {
                this.width = 185.px
                this.paddingBottom = 20.px
                this.paddingTop = 10.px
                this.marginRight = 2.px
                this.paddingRight = 5.px
            }

            layout {
                div("container") {
                    id = "container"
                    div("sidebar") {
                        id = "sidebar"
                        for ((obj, parameters) in trackedParams) {
                            h3 { obj.title() ?: "untitled" }

                            for (parameter in parameters) {
                                addSlider(obj, parameter)
                            }
                        }
                    }
                }
            }
        }

        panel.enabled = enabled

        program.extend(panel)
    }

    private fun Div.addSlider(obj: Any, parameter: Parameter) {

        when(val type = parameter.parameterType) {
            ParameterType.Int -> {
                slider {
                    label = parameter.label
                    range = Range(parameter.intRange!!.start.toDouble(), parameter.intRange!!.endInclusive.toDouble())
                    precision = parameter.precision!!
                    value = (parameter.property as KMutableProperty1<Any, Int>).get(obj).toDouble()
                    events.valueChanged.subscribe {
                        (parameter.property as KMutableProperty1<Any, Int>).set(obj, value.toInt())
                        onChangeListener?.invoke(parameter.property.name, it.newValue)
                    }
                }
            }
            ParameterType.Double -> {
                slider {
                    label = parameter.label
                    range = Range(parameter.doubleRange!!.start, parameter.doubleRange!!.endInclusive.toDouble())
                    precision = parameter.precision!!
                    value = (parameter.property as KMutableProperty1<Any, Double>).get(obj)
                    events.valueChanged.subscribe {
                        (parameter.property as KMutableProperty1<Any, Double>).set(obj, value)
                        onChangeListener?.invoke(parameter.property.name, it.newValue)
                    }
                }
            }

            ParameterType.Button -> {
                button {
                    label = parameter.label
                    events.clicked.subscribe {
                        parameter.property.call()
                        onChangeListener?.invoke(parameter.property.name, null)
                    }
                }
            }

            ParameterType.Boolean -> {
                slider {
                    label = parameter.label
                    range = Range(0.0, 1.0)
                    precision = 0
                    value = if ((parameter.property as KMutableProperty1<Any, Boolean>).get(obj)) 1.0 else 0.0
                    events.valueChanged.subscribe {
                        value = it.newValue
                        (parameter.property as KMutableProperty1<Any, Boolean>).set(obj, value > 0.5)
                        onChangeListener?.invoke(parameter.property.name, it.newValue)
                    }
                }
            }
        }
    }

    val trackedParams = mutableMapOf<Any, List<Parameter>>()

    /**
     * Add an object to the GUI
     */
    fun add(objectWithParameters: Any) {
        val parameters = objectWithParameters.listParameters()
        if (parameters.size > 0) {
            trackedParams[objectWithParameters] = parameters
        }
    }
}
