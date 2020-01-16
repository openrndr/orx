package org.openrndr.extra.gui

import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.panel.ControlManager
import org.openrndr.panel.controlManager
import org.openrndr.panel.elements.*
import org.openrndr.panel.style.*
import kotlin.reflect.*
import kotlin.reflect.full.declaredMemberProperties

@Target(AnnotationTarget.PROPERTY)
annotation class GuiDoubleParam(val label: String, val low: Double, val high: Double, val precision: Int)

@Target(AnnotationTarget.PROPERTY)
annotation class GuiIntParam(val label: String, val low: Int, val high: Int, val precision: Int)

@Target(AnnotationTarget.PROPERTY)
annotation class GuiBooleanParam(val label: String)

@Target(AnnotationTarget.PROPERTY)
annotation class GuiButtonParam(val label: String)

@Target(AnnotationTarget.CLASS)
annotation class GuiTitle(val title: String)

@Suppress("unused")
class GUI : Extension {
    private var onChangeListener: ((name: String, value: Any?) -> Unit)? = null
    override var enabled: Boolean = true

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
                        for ((params, props) in trackedParams) {
                            try {
                                val titleAnnotations = params::class.annotations.filter {
                                    it.annotationClass == GuiTitle::class
                                }

                                if (titleAnnotations.isNotEmpty()) {
                                    val annotation = titleAnnotations.first() as GuiTitle

                                    h3{ annotation.title }
                                }
                            } catch (e: Throwable) {
                                println("GUI: Something wrong with setting the title")
                                println(e.message)
                            }


                            for (param in props) {
                                val annotation = param.annotations.first()
                                val paramName = param.name

                                try {
                                    val mp = param as KMutableProperty1<Any, Any?>

                                    addSlider(params, annotation, mp)
                                } catch (e: Throwable) {
                                    println("GUI: Could not setup <$paramName>")
                                    println(e.message)
                                }
                            }
                        }
                    }
                }
            }
        }

        panel.enabled = enabled

        program.extend(panel)
    }

    private fun Div.addSlider(
            params: Any,
            annotation: Annotation,
            mp: KMutableProperty1<Any, Any?>
    ) {
        when (annotation.annotationClass) {
            GuiIntParam::class -> {
                val guiParam = annotation as GuiIntParam

                slider {
                    label = guiParam.label
                    range = Range(guiParam.low.toDouble(), guiParam.high.toDouble())
                    precision = guiParam.precision
                    value = (mp.get(params) as Int).toDouble()
                    events.valueChanged.subscribe {
                        value = it.newValue
                        mp.set(params, it.newValue.toInt())

                        onChangeListener?.invoke(mp.name, it.newValue)
                    }
                }
            }
            GuiDoubleParam::class -> {
                val guiParam = annotation as GuiDoubleParam

                slider {
                    label = guiParam.label
                    range = Range(guiParam.low, guiParam.high)
                    precision = guiParam.precision
                    value = (mp.get(params) as Double)
                    events.valueChanged.subscribe {
                        value = it.newValue
                        mp.set(params, it.newValue)

                        onChangeListener?.invoke(mp.name, it.newValue)
                    }
                }
            }
            GuiButtonParam::class -> {
                val guiParam = annotation as GuiButtonParam

                button {
                    label = guiParam.label
                    events.clicked.subscribe {
                        mp.call()

                        onChangeListener?.invoke(mp.name, null)
                    }
                }
            }
            GuiBooleanParam::class -> {
                val guiParam = annotation as GuiBooleanParam
                val initialVal = if ((mp.get(params) as Boolean)) 1.0 else 0.0

                slider {
                    label = guiParam.label
                    range = Range(0.0, 1.0)
                    precision = 0
                    value = initialVal
                    events.valueChanged.subscribe {
                        value = it.newValue

                        mp.set(params, it.newValue == 1.0)

                        onChangeListener?.invoke(mp.name, it.newValue == 1.0)
                    }
                }
            }
        }
    }

    private inline fun <reified T> KAnnotatedElement.findAnnotation(): T? = annotations.filterIsInstance<T>().firstOrNull()

    private inline fun <reified T> getAnnotations(params: Any): List<KProperty1<out Any, Any?>> =
            params::class.declaredMemberProperties.filter {
                it.visibility == KVisibility.PUBLIC && it.findAnnotation<T>() != null
            }

    val trackedParams = mutableMapOf<Any, List<KProperty1<out Any, Any?>>>()

    fun add(params: Any) {
        val doubleProperties = getAnnotations<GuiDoubleParam>(params)
        val intProperties = getAnnotations<GuiIntParam>(params)
        val booleanProperties = getAnnotations<GuiBooleanParam>(params)
        val buttonProperties = getAnnotations<GuiButtonParam>(params)

        trackedParams[params] = doubleProperties + intProperties + booleanProperties + buttonProperties
    }
}

