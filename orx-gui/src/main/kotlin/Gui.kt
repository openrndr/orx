package org.openrndr.extra.gui

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.openrndr.Extension
import org.openrndr.KEY_F11
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.dialogs.openFileDialog
import org.openrndr.dialogs.saveFileDialog
import org.openrndr.extra.parameters.*
import org.openrndr.internal.Driver
import org.openrndr.panel.ControlManager
import org.openrndr.panel.controlManager
import org.openrndr.panel.elements.*
import org.openrndr.panel.style.*

import java.io.File
import kotlin.reflect.KMutableProperty1

private data class LabeledObject(val label: String, val obj: Any)
private class CompartmentState(var collapsed: Boolean = false, val parameterValues: MutableMap<String, Any> = mutableMapOf())
private class TrackedObjectBinding(
        val parameters: List<Parameter>,
        val parameterControls: MutableMap<Parameter, Element> = mutableMapOf()
)

private val persistentCompartmentStates = mutableMapOf<Long, MutableMap<String, CompartmentState>>()

private fun <T : Any> getPersistedOrDefault(compartmentLabel: String, property: KMutableProperty1<Any, T>, obj: Any): T? {
    val state = persistentCompartmentStates[Driver.instance.contextID]!![compartmentLabel]
    if (state == null) {
        return property.get(obj)
    } else {
        return state.parameterValues[property.name] as? T?
    }
}

private fun <T : Any> setAndPersist(compartmentLabel: String, property: KMutableProperty1<Any, T>, obj: Any, value: T) {
    property.set(obj, value)
    val state = persistentCompartmentStates[Driver.instance.contextID]!![compartmentLabel]!!
    state.parameterValues[property.name] = value
}

@Suppress("unused", "UNCHECKED_CAST")
class GUI : Extension {
    private var onChangeListener: ((name: String, value: Any?) -> Unit)? = null
    override var enabled = true

    private lateinit var panel: ControlManager

    fun onChange(listener: (name: String, value: Any?) -> Unit) {
        onChangeListener = listener
    }

    override fun setup(program: Program) {

        program.keyboard.keyDown.listen {
            if (it.key == KEY_F11) {
                enabled = !enabled
                panel.enabled = enabled
            }
        }

        panel = program.controlManager {
            styleSheet(has class_ "container") {
                this.display = Display.FLEX
                this.flexDirection = FlexDirection.Column
                this.width = 200.px
                this.height = 100.percent
            }

            styleSheet(has class_ "toolbar") {
                this.height = 30.px
                this.width = 100.percent
                this.display = Display.FLEX
                this.flexDirection = FlexDirection.Row
                this.background = Color.RGBa(ColorRGBa.GRAY.copy(a = 0.99))
            }

            styleSheet(has class_ "collapsed") {
                this.display = Display.NONE
            }

            styleSheet(has class_ "compartment") {
                this.paddingBottom = 20.px
            }

            styleSheet(has class_ "sidebar") {
                this.width = 200.px
                this.paddingBottom = 20.px
                this.paddingTop = 10.px
                this.paddingLeft = 10.px
                this.paddingRight = 10.px
                this.marginRight = 2.px
                this.height = 100.percent
                this.background = Color.RGBa(ColorRGBa.GRAY.copy(a = 0.99))
                this.overflow = Overflow.Scroll

                descendant(has type "colorpicker-button") {
                    this.width = 175.px
                }

                descendant(has type "slider") {
                    this.width = 175.px
                }

                descendant(has type "button") {
                    this.width = 175.px
                }

                descendant(has type "textfield") {
                    this.width = 175.px
                }
            }

            styleSheet(has type "dropdown-button") {
                this.width = 175.px
            }


            layout {
                div("container") {
                    id = "container"
                    div("toolbar") {
                        button {
                            label = "Randomize"
                            clicked {
                                randomize()
                            }
                        }
                        button {
                            label = "Load"
                            clicked {
                                openFileDialog(supportedExtensions = listOf("json")) {
                                    loadParameters(it)
                                }
                            }
                        }
                        button {
                            label = "Save"
                            clicked {
                                saveFileDialog(supportedExtensions = listOf("json")) {
                                    saveParameters(it)
                                }
                            }
                        }
                    }

                    div("sidebar") {
                        id = "sidebar"
                        for ((labeledObject, binding) in trackedObjects) {
                            val (label, obj) = labeledObject

                            val header = h3 { label }
                            val collapsible = div("compartment") {
                                for (parameter in binding.parameters) {
                                    val element = addControl(labeledObject, parameter)
                                    binding.parameterControls[parameter] = element
                                }
                            }
                            val collapseClass = ElementClass("collapsed")

                            /* this is guaranteed to be in the dictionary after insertion through add() */
                            val collapseState = persistentCompartmentStates[Driver.instance.contextID]!![label]!!
                            if (collapseState.collapsed) {
                                collapsible.classes.add(collapseClass)
                            }

                            header.mouse.pressed.subscribe {
                                if (collapseClass in collapsible.classes) {
                                    collapseState.collapsed = false
                                    collapsible.classes.remove(collapseClass)
                                } else {
                                    collapseState.collapsed = true
                                    collapsible.classes.add(collapseClass)
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

    private fun Div.addControl(compartment: LabeledObject, parameter: Parameter): Element {
        val obj = compartment.obj

        return when (parameter.parameterType) {
            ParameterType.Int -> {
                slider {
                    label = parameter.label
                    range = Range(parameter.intRange!!.first.toDouble(), parameter.intRange!!.last.toDouble())
                    precision = 0
                    events.valueChanged.subscribe {
                        setAndPersist(compartment.label, parameter.property as KMutableProperty1<Any, Int>, obj, it.newValue.toInt())
                        (parameter.property as KMutableProperty1<Any, Int>).set(obj, value.toInt())
                        onChangeListener?.invoke(parameter.property!!.name, it.newValue)
                    }
                    getPersistedOrDefault(compartment.label, parameter.property as KMutableProperty1<Any, Int>, obj)?.let {
                        value = it.toDouble()
                    }
                }
            }
            ParameterType.Double -> {
                slider {
                    label = parameter.label
                    range = Range(parameter.doubleRange!!.start, parameter.doubleRange!!.endInclusive)
                    precision = parameter.precision!!
                    events.valueChanged.subscribe {
                        setAndPersist(compartment.label, parameter.property as KMutableProperty1<Any, Double>, obj, it.newValue)
                        onChangeListener?.invoke(parameter.property!!.name, it.newValue)
                    }
                    getPersistedOrDefault(compartment.label, parameter.property as KMutableProperty1<Any, Double>, obj)?.let {
                        value = it
                    }
                }
            }

            ParameterType.Action -> {
                button {
                    label = parameter.label
                    events.clicked.subscribe {
                        /* the `obj` we pass in here is the receiver */
                        parameter.function!!.call(obj)
                        onChangeListener?.invoke(parameter.function!!.name, null)
                    }
                }
            }

            ParameterType.Boolean -> {
                slider {
                    label = parameter.label
                    range = Range(0.0, 1.0)
                    precision = 0
                    events.valueChanged.subscribe {
                        value = it.newValue
                        (parameter.property as KMutableProperty1<Any, Boolean>).set(obj, value > 0.5)
                        onChangeListener?.invoke(parameter.property!!.name, it.newValue)
                    }
                    value = if ((parameter.property as KMutableProperty1<Any, Boolean>).get(obj)) 1.0 else 0.0
                }
            }

            ParameterType.Text -> {
                textfield {
                    label = parameter.label
                    events.valueChanged.subscribe {
                        setAndPersist(compartment.label, parameter.property as KMutableProperty1<Any, String>, obj, it.newValue)
                        onChangeListener?.invoke(parameter.property!!.name, it.newValue)
                    }
                    getPersistedOrDefault(compartment.label, parameter.property as KMutableProperty1<Any, String>, obj)?.let {
                        value = it
                    }
                }
            }

            ParameterType.Color -> {
                colorpickerButton {
                    label = parameter.label
                    events.valueChanged.subscribe {
                        setAndPersist(
                                compartment.label,
                                parameter.property as KMutableProperty1<Any, ColorRGBa>,
                                obj,
                                it.color
                        )
                        onChangeListener?.invoke(parameter.property!!.name, it.color)
                    }
                    getPersistedOrDefault(
                            compartment.label,
                            parameter.property as KMutableProperty1<Any, ColorRGBa>,
                            obj
                    )?.let {
                        color = it
                    }
                }
            }
        }
    }

    private val trackedObjects = mutableMapOf<LabeledObject, TrackedObjectBinding>()

    private fun updateControls() {
        for ((labeledObject, binding) in trackedObjects) {
            for ((parameter, control) in binding.parameterControls) {
                updateControl(labeledObject, parameter, control)
            }
        }
    }

    private class ParameterValue(var doubleValue: Double? = null,
                                 var intValue: Int? = null,
                                 var booleanValue: Boolean? = null,
                                 var colorValue: ColorRGBa? = null,
                                 var textValue: String? = null)


    private fun saveParameters(file: File) {
        fun <T> KMutableProperty1<out Any, Any?>?.qget(obj: Any): T {
            return (this as KMutableProperty1<Any, T>).get(obj)
        }

        val toSave =
                trackedObjects.entries.associate { (lo, b) ->
                    Pair(lo.label, b.parameterControls.keys.associate { k ->
                        Pair(k.property?.name ?: k.function?.name ?: error("no name"), when (k.parameterType) {
                            ParameterType.Double -> ParameterValue(doubleValue = k.property.qget(lo.obj) as Double)
                            ParameterType.Int -> ParameterValue(intValue = k.property.qget(lo.obj) as Int)
                            ParameterType.Action -> ParameterValue()
                            ParameterType.Color -> ParameterValue(colorValue = k.property.qget(lo.obj) as ColorRGBa)
                            ParameterType.Text -> ParameterValue(textValue = k.property.qget(lo.obj) as String)
                            ParameterType.Boolean -> ParameterValue(booleanValue = k.property.qget(lo.obj as Boolean))
                        })
                    })
                }
        file.writeText(Gson().toJson(toSave))
    }

    private fun loadParameters(file: File) {
        fun <T> KMutableProperty1<out Any, Any?>?.qset(obj: Any, value:T) {
            return (this as KMutableProperty1<Any, T>).set(obj, value)
        }
        val json = file.readText()
        val tt = object : TypeToken<Map<String, Map<String, ParameterValue>>>() {}
        val labeledValues: Map<String, Map<String, ParameterValue>> = Gson().fromJson(json, tt.type)

        labeledValues.forEach { label, ps ->
            trackedObjects.keys.find { it.label == label }?.let { lo ->
                val binding = trackedObjects[lo]!!
                ps.forEach { (parameterName, parameterValue) ->
                    binding.parameters.find { it.property?.name == parameterName}?.let { parameter ->
                        when (parameter.parameterType) {
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
                            ParameterType.Boolean -> parameterValue.booleanValue?.let {
                                parameter.property.qset(lo.obj, it)
                            }
                            ParameterType.Action -> {
                                // intentionally do nothing
                            }
                        }
                    }
                }
            }
        }
        updateControls()
    }

    private fun updateControl(labeledObject: LabeledObject, parameter: Parameter, control: Element) {
        when (parameter.parameterType) {
            ParameterType.Double -> {
                (control as Slider).value = (parameter.property as KMutableProperty1<Any, Double>).get(labeledObject.obj)
            }
            ParameterType.Int -> {
                (control as Slider).value = (parameter.property as KMutableProperty1<Any, Int>).get(labeledObject.obj).toDouble()
            }
            ParameterType.Text -> {
                (control as Textfield).value = (parameter.property as KMutableProperty1<Any, String>).get(labeledObject.obj)
            }
            ParameterType.Color -> {
                (control as ColorpickerButton).color = (parameter.property as KMutableProperty1<Any, ColorRGBa>).get(labeledObject.obj)
            }
            ParameterType.Boolean -> {
                (control as Slider).value = if ((parameter.property as KMutableProperty1<Any, Boolean>).get(labeledObject.obj)) 1.0 else 0.0
            }
            ParameterType.Action -> {
                // intentionally do nothing
            }
        }
    }

    fun randomize() {
        for ((labeledObject, binding) in trackedObjects) {
            // -- only randomize visible parameters
            for (parameter in binding.parameterControls.keys) {
                when (parameter.parameterType) {
                    ParameterType.Double -> {
                        val min = parameter.doubleRange!!.start
                        val max = parameter.doubleRange!!.endInclusive
                        (parameter.property as KMutableProperty1<Any, Double>).set(labeledObject.obj, Math.random() * (max - min) + min)
                    }
                    ParameterType.Int -> {
                        val min = parameter.intRange!!.first
                        val max = parameter.intRange!!.last
                        (parameter.property as KMutableProperty1<Any, Int>).set(labeledObject.obj, (Math.random() * (max - min) + min).toInt())
                    }
                    ParameterType.Boolean -> {
                        (parameter.property as KMutableProperty1<Any, Boolean>).set(labeledObject.obj, (Math.random() < 0.5))
                    }
                    ParameterType.Color -> {
                        val c = ColorRGBa(Math.random(), Math.random(), Math.random())
                        (parameter.property as KMutableProperty1<Any, ColorRGBa>).set(labeledObject.obj, c)
                    }
                    else -> {
                        // do nothing
                    }
                }
            }
        }
        updateControls()
    }


    /**
     * Recursively find a unique label
     * @param label to find an alternate for in case it already exist
     */
    private fun resolveUniqueLabel(label: String): String {
        return trackedObjects.keys.find { it.label == label }?.let { lo ->
            resolveUniqueLabel(Regex("(.*) / ([0-9]+)").matchEntire(lo.label)?.let {
                "${it.groupValues[1]} / ${1 + it.groupValues[2].toInt()}"
            } ?: "$label / 2")
        } ?: label
    }

    /**
     * Add an object to the GUI
     * @param objectWithParameters an object of a class that annotated parameters
     * @param label an optional label that overrides the label supplied in a [Description] annotation
     * @return pass-through of [objectWithParameters]
     */
    fun <T : Any> add(objectWithParameters: T, label: String? = objectWithParameters.title()): T {
        val parameters = objectWithParameters.listParameters()
        val uniqueLabel = resolveUniqueLabel(label ?: "No name")

        if (parameters.isNotEmpty()) {
            val collapseStates = persistentCompartmentStates.getOrPut(Driver.instance.contextID) {
                mutableMapOf()
            }
            collapseStates.getOrPut(uniqueLabel) {
                CompartmentState()
            }
            trackedObjects[LabeledObject(uniqueLabel, objectWithParameters)] = TrackedObjectBinding(parameters)
        }
        return objectWithParameters
    }

    /**
     * Add an object to the GUI using a builder.
     * @param label an optional label that overrides the label supplied in a [Description] annotation
     * @return the built object
     */
    fun <T : Any> add(label: String? = null, builder: () -> T): T {
        val t = builder()
        return add(t, label ?: t.title())
    }
}