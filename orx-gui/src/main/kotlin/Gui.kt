package org.openrndr.extra.gui

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.openrndr.Extension
import org.openrndr.KEY_F11
import org.openrndr.KEY_LEFT_SHIFT
import org.openrndr.KeyModifier
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.dialogs.openFileDialog
import org.openrndr.dialogs.saveFileDialog
import org.openrndr.draw.Drawer
import org.openrndr.extra.parameters.*
import org.openrndr.internal.Driver
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import org.openrndr.panel.ControlManager
import org.openrndr.panel.controlManager
import org.openrndr.panel.elements.*
import org.openrndr.panel.style.*

import java.io.File
import kotlin.math.roundToInt
import kotlin.reflect.KMutableProperty1

/** Dear contributor, just in case you are here looking to add a new parameter type.
There is a 6-step incantation to add a new parameter type
0) Add your parameter type to orx-parameters, follow the instructions provided there.

1) Setup a control style, very likely analogous to the styles already in place.
2) Add control creation code.
3) Add value serialization code, may need to update ParameterValue too.
4) Add value deserialization code.
5) Add value randomization code.
6) Add control update code.

You can use your editor's search functionality to jump to "1)", "2)".
 */
private data class LabeledObject(val label: String, val obj: Any)

private class CompartmentState(var collapsed: Boolean, val parameterValues: MutableMap<String, Any> = mutableMapOf())
private class SidebarState(var hidden: Boolean = false, var collapsed: Boolean = false, var scrollTop: Double = 0.0)
private class TrackedObjectBinding(
        val parameters: List<Parameter>,
        val parameterControls: MutableMap<Parameter, Element> = mutableMapOf()
)

private val persistentCompartmentStates = mutableMapOf<Long, MutableMap<String, CompartmentState>>()
private val persistentSidebarStates = mutableMapOf<Long, SidebarState>()

private fun sidebarState(): SidebarState = persistentSidebarStates.getOrPut(Driver.instance.contextID) {
    SidebarState()
}

private fun <T : Any> getPersistedOrDefault(compartmentLabel: String, property: KMutableProperty1<Any, T>, obj: Any): T? {
    val state = persistentCompartmentStates[Driver.instance.contextID]!![compartmentLabel]
    if (state == null) {
        return property.get(obj)
    } else {
        @Suppress("UNCHECKED_CAST")
        return (state.parameterValues[property.name] as? T?) ?: return property.get(obj)
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

    var compartmentsCollapsedByDefault = true
    var doubleBind = false

    private lateinit var panel: ControlManager

    // Randomize button
    private var shiftDown = false
    private var randomizeButton: Button? = null // FIXME should this be null or is there a better way?

    fun onChange(listener: (name: String, value: Any?) -> Unit) {
        onChangeListener = listener
    }

    override fun setup(program: Program) {

        program.keyboard.keyDown.listen {
            if (it.key == KEY_F11) {
                enabled = !enabled
                panel.enabled = enabled
                sidebarState().hidden = !enabled
            }

            if (it.key == KEY_LEFT_SHIFT) {
                shiftDown = true
                randomizeButton!!.classes.add(ElementClass("randomize-strong"))
            }
        }

        program.keyboard.keyUp.listen {
            if (it.key == KEY_LEFT_SHIFT) {
                shiftDown = false
                randomizeButton!!.classes.remove(ElementClass("randomize-strong"))
            }
        }

        panel = program.controlManager {
            styleSheet(has class_ "container") {
                this.display = Display.FLEX
                this.flexDirection = FlexDirection.Column
                this.width = 200.px
                this.height = 100.percent
            }

            styleSheet(has class_ "collapse-border") {
                this.display = Display.FLEX
                this.flexDirection = FlexDirection.Column
                this.height = 5.px
                this.width = 100.percent
                this.background = Color.RGBa(ColorRGBa.GRAY.shade(0.9))

                and(has state "hover") {
                    this.background = Color.RGBa(ColorRGBa.GRAY.shade(1.1))
                }
            }

            styleSheet(has class_ "toolbar") {
                this.height = 42.px
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

                //<editor-fold desc="1) setup control style">
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

                descendant(has type "toggle") {
                    this.width = 175.px
                }

                descendant(has type "xy-pad") {
                    this.width = 175.px
                    this.height = 175.px
                }

                descendant(has type "sequence-editor") {
                    this.width = 175.px
                    this.height = 100.px
                }
                descendant(has type "sliders-vector2") {
                    this.width = 175.px
                    this.height = 100.px
                }
                descendant(has type "sliders-vector3") {
                    this.width = 175.px
                    this.height = 100.px
                }
                descendant(has type "sliders-vector4") {
                    this.width = 175.px
                    this.height = 100.px
                }

                //</editor-fold>
            }

            styleSheet(has class_ "randomize-strong") {
                color = Color.RGBa(ColorRGBa.PINK)

                and(has state "hover") {
                    color = Color.RGBa(ColorRGBa.BLACK)
                    background = Color.RGBa(ColorRGBa.PINK)
                }
            }

            styleSheet(has type "dropdown-button") {
                this.width = 175.px
            }


            layout {
                div("container") {
                    id = "container"
                    val header = div("toolbar") {
                        randomizeButton = button {
                            label = "Randomize"
                            clicked {
                                randomize(strength = if (shiftDown) .75 else .05)
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
                    val collapseBorder = div("collapse-border") {

                    }

                    val collapsibles = mutableSetOf<Div>()
                    val sidebar = div("sidebar") {
                        id = "sidebar"
                        scrollTop = sidebarState().scrollTop
                        for ((labeledObject, binding) in trackedObjects) {
                            val (label, _) = labeledObject

                            val header = h3 { label }
                            val collapsible = div("compartment") {
                                for (parameter in binding.parameters) {
                                    val element = addControl(labeledObject, parameter)
                                    binding.parameterControls[parameter] = element
                                }
                            }
                            collapsibles.add(collapsible)
                            val collapseClass = ElementClass("collapsed")

                            /* this is guaranteed to be in the dictionary after insertion through add() */
                            val collapseState = persistentCompartmentStates[Driver.instance.contextID]!![label]!!
                            if (collapseState.collapsed) {
                                collapsible.classes.add(collapseClass)
                            }

                            header.mouse.pressed.listen {
                                it.cancelPropagation()
                            }
                            header.mouse.clicked.listen {

                                if (KeyModifier.CTRL in it.modifiers) {
                                    collapsible.classes.remove(collapseClass)
                                    persistentCompartmentStates[Driver.instance.contextID]!!.forEach {
                                        it.value.collapsed = true
                                    }
                                    collapseState.collapsed = false

                                    (collapsibles - collapsible).forEach {
                                        it.classes.add(collapseClass)
                                    }
                                } else {

                                    if (collapseClass in collapsible.classes) {
                                        collapsible.classes.remove(collapseClass)
                                        collapseState.collapsed = false
                                    } else {
                                        collapsible.classes.add(collapseClass)
                                        collapseState.collapsed = true
                                    }
                                }
                            }
                        }
                    }
                    collapseBorder.mouse.pressed.listen {
                        it.cancelPropagation()
                    }

                    collapseBorder.mouse.clicked.listen {
                        val collapsed = ElementClass("collapsed")
                        if (collapsed in sidebar.classes) {
                            sidebar.classes.remove(collapsed)
                            sidebarState().collapsed = false
                        } else {
                            sidebar.classes.add(collapsed)
                            sidebarState().collapsed = true
                        }
                        it.cancelPropagation()
                    }
                    sidebar.mouse.scrolled.listen {
                        sidebarState().scrollTop = sidebar.scrollTop
                    }
                    if (sidebarState().collapsed) {
                        sidebar.classes.add(ElementClass("collapsed"))
                    }
                    sidebar.scrollTop = sidebarState().scrollTop
                }
            }
        }

        if (sidebarState().hidden) {
            enabled = false
            panel.enabled = false
        } else {
            enabled = true
            panel.enabled = true
        }

        program.extend(panel)
    }

    /* 2) control creation. create control, set label, set range, setup event-handler, load values */
    //<editor-fold desc="2) Control creation">
    private fun Div.addControl(compartment: LabeledObject, parameter: Parameter): Element {
        val obj = compartment.obj

        return when (parameter.parameterType) {

            ParameterType.Int -> {
                slider {
                    label = parameter.label
                    range = Range(parameter.intRange!!.first.toDouble(), parameter.intRange!!.last.toDouble())
                    precision = 0
                    events.valueChanged.listen {
                        setAndPersist(compartment.label, parameter.property as KMutableProperty1<Any, Int>, obj, it.newValue.toInt())
                        (parameter.property as KMutableProperty1<Any, Int>).set(obj, value.toInt())
                        onChangeListener?.invoke(parameter.property!!.name, it.newValue)
                    }
                    getPersistedOrDefault(compartment.label, parameter.property as KMutableProperty1<Any, Int>, obj)?.let {
                        value = it.toDouble()
                        setAndPersist(compartment.label, parameter.property as KMutableProperty1<Any, Int>, obj, it)
                    }
                }
            }
            ParameterType.Double -> {
                slider {
                    label = parameter.label
                    range = Range(parameter.doubleRange!!.start, parameter.doubleRange!!.endInclusive)
                    precision = parameter.precision!!
                    events.valueChanged.listen {
                        setAndPersist(compartment.label, parameter.property as KMutableProperty1<Any, Double>, obj, it.newValue)
                        onChangeListener?.invoke(parameter.property!!.name, it.newValue)
                    }
                    getPersistedOrDefault(compartment.label, parameter.property as KMutableProperty1<Any, Double>, obj)?.let {
                        value = it
                        /*  this is generally not needed, but when the persisted value is equal to the slider default
                            it will not emit the newly set value */
                        setAndPersist(compartment.label, parameter.property as KMutableProperty1<Any, Double>, obj, it)
                    }
                }
            }
            ParameterType.Action -> {
                button {
                    label = parameter.label
                    events.clicked.listen {
                        /* the `obj` we pass in here is the receiver */
                        parameter.function!!.call(obj)
                        onChangeListener?.invoke(parameter.function!!.name, null)
                    }
                }
            }
            ParameterType.Boolean -> {
                toggle {
                    label = parameter.label
                    events.valueChanged.listen {
                        value = it.newValue
                        setAndPersist(compartment.label, parameter.property as KMutableProperty1<Any, Boolean>, obj, it.newValue)
                        onChangeListener?.invoke(parameter.property!!.name, it.newValue)
                    }
                    getPersistedOrDefault(compartment.label, parameter.property as KMutableProperty1<Any, Boolean>, obj)?.let {
                        value = it
                        setAndPersist(compartment.label, parameter.property as KMutableProperty1<Any, Boolean>, obj, it)
                    }
                }
            }
            ParameterType.Text -> {
                textfield {
                    label = parameter.label
                    events.valueChanged.listen {
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
                    events.valueChanged.listen {
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

            ParameterType.XY -> {
                xyPad {
                    minX = parameter.vectorRange!!.first.x
                    minY = parameter.vectorRange!!.first.y
                    maxX = parameter.vectorRange!!.second.x
                    maxY = parameter.vectorRange!!.second.y
                    precision = parameter.precision!!
                    showVector = parameter.showVector!!
                    invertY = parameter.invertY!!

                    events.valueChanged.listen {
                        setAndPersist(
                                compartment.label,
                                parameter.property as KMutableProperty1<Any, Vector2>,
                                obj,
                                it.newValue
                        )
                        onChangeListener?.invoke(parameter.property!!.name, it.newValue)
                    }
                }
            }

            ParameterType.DoubleList -> {
                sequenceEditor {
                    range = parameter.doubleRange!!
                    label = parameter.label
                    minimumSequenceLength = parameter.sizeRange!!.start
                    maximumSequenceLength = parameter.sizeRange!!.endInclusive
                    precision = parameter.precision!!

                    events.valueChanged.listen {
                        setAndPersist(
                                compartment.label,
                                parameter.property as KMutableProperty1<Any, MutableList<Double>>,
                                obj,
                                it.newValue.toMutableList()
                        )
                        onChangeListener?.invoke(parameter.property!!.name, it.newValue)
                    }
                    getPersistedOrDefault(
                            compartment.label,
                            parameter.property as KMutableProperty1<Any, MutableList<Double>>,
                            obj
                    )?.let {
                        value = it
                    }
                }
            }

            ParameterType.Vector2 -> {
                slidersVector2 {
                    range = parameter.doubleRange!!
                    label = parameter.label
                    precision = parameter.precision!!

                    events.valueChanged.listen {
                        setAndPersist(
                                compartment.label,
                                parameter.property as KMutableProperty1<Any, Vector2>,
                                obj,
                                it.newValue)

                        onChangeListener?.invoke(parameter.property!!.name, it.newValue)
                    }
                    getPersistedOrDefault(
                            compartment.label,
                            parameter.property as KMutableProperty1<Any, Vector2>,
                            obj
                    )?.let {
                        value = it
                    }
                }
            }

            ParameterType.Vector3 -> {
                slidersVector3 {
                    range = parameter.doubleRange!!
                    label = parameter.label
                    precision = parameter.precision!!

                    events.valueChanged.listen {
                        setAndPersist(
                                compartment.label,
                                parameter.property as KMutableProperty1<Any, Vector3>,
                                obj,
                                it.newValue)

                        onChangeListener?.invoke(parameter.property!!.name, it.newValue)
                    }
                    getPersistedOrDefault(
                            compartment.label,
                            parameter.property as KMutableProperty1<Any, Vector3>,
                            obj
                    )?.let {
                        value = it
                    }
                }
            }

            ParameterType.Vector4 -> {
                slidersVector4 {
                    range = parameter.doubleRange!!
                    label = parameter.label
                    precision = parameter.precision!!

                    events.valueChanged.listen {
                        setAndPersist(
                                compartment.label,
                                parameter.property as KMutableProperty1<Any, Vector4>,
                                obj,
                                it.newValue)

                        onChangeListener?.invoke(parameter.property!!.name, it.newValue)
                    }
                    getPersistedOrDefault(
                            compartment.label,
                            parameter.property as KMutableProperty1<Any, Vector4>,
                            obj
                    )?.let {
                        value = it
                    }
                }
            }


        }
    }
    //</editor-fold>

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
                                 var vector2Value: Vector2? = null,
                                 var vector3Value: Vector3? = null,
                                 var vector4Value: Vector4? = null,
                                 var doubleListValue: MutableList<Double>? = null,
                                 var textValue: String? = null)


    fun saveParameters(file: File) {
        fun <T> KMutableProperty1<out Any, Any?>?.qget(obj: Any): T {
            return (this as KMutableProperty1<Any, T>).get(obj)
        }

        val toSave =
                trackedObjects.entries.associate { (lo, b) ->
                    Pair(lo.label, b.parameterControls.keys.associate { k ->
                        Pair(k.property?.name ?: k.function?.name ?: error("no name"), when (k.parameterType) {
                            /* 3) setup serializers */
                            ParameterType.Double -> ParameterValue(doubleValue = k.property.qget(lo.obj) as Double)
                            ParameterType.Int -> ParameterValue(intValue = k.property.qget(lo.obj) as Int)
                            ParameterType.Action -> ParameterValue()
                            ParameterType.Color -> ParameterValue(colorValue = k.property.qget(lo.obj) as ColorRGBa)
                            ParameterType.Text -> ParameterValue(textValue = k.property.qget(lo.obj) as String)
                            ParameterType.Boolean -> ParameterValue(booleanValue = k.property.qget(lo.obj) as Boolean)
                            ParameterType.XY -> ParameterValue(vector2Value = k.property.qget(lo.obj) as Vector2)
                            ParameterType.DoubleList -> ParameterValue(doubleListValue = k.property.qget(lo.obj) as MutableList<Double>)
                            ParameterType.Vector2 -> ParameterValue(vector2Value = k.property.qget(lo.obj) as Vector2)
                            ParameterType.Vector3 -> ParameterValue(vector3Value = k.property.qget(lo.obj) as Vector3)
                            ParameterType.Vector4 -> ParameterValue(vector4Value = k.property.qget(lo.obj) as Vector4)
                        })
                    })
                }
        file.writeText(Gson().toJson(toSave))
    }

    fun loadParameters(file: File) {
        fun <T> KMutableProperty1<out Any, Any?>?.qset(obj: Any, value: T) {
            return (this as KMutableProperty1<Any, T>).set(obj, value)
        }

        val json = file.readText()
        val typeToken = object : TypeToken<Map<String, Map<String, ParameterValue>>>() {}
        val labeledValues: Map<String, Map<String, ParameterValue>> = Gson().fromJson(json, typeToken.type)

        labeledValues.forEach { (label, ps) ->
            trackedObjects.keys.find { it.label == label }?.let { lo ->
                val binding = trackedObjects[lo]!!
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
            /* 5) Update control from property value */
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
            ParameterType.XY -> {
                (control as XYPad).value = (parameter.property as KMutableProperty1<Any, Vector2>).get(labeledObject.obj)
            }
            ParameterType.DoubleList -> {
                (control as SequenceEditor).value = (parameter.property as KMutableProperty1<Any, MutableList<Double>>).get(labeledObject.obj)
            }
            ParameterType.Boolean -> {
                (control as Toggle).value = (parameter.property as KMutableProperty1<Any, Boolean>).get(labeledObject.obj)
            }
            ParameterType.Vector2 -> {
                (control as SlidersVector2).value = (parameter.property as KMutableProperty1<Any, Vector2>).get(labeledObject.obj)
            }
            ParameterType.Vector3 -> {
                (control as SlidersVector3).value = (parameter.property as KMutableProperty1<Any, Vector3>).get(labeledObject.obj)
            }
            ParameterType.Vector4 -> {
                (control as SlidersVector4).value = (parameter.property as KMutableProperty1<Any, Vector4>).get(labeledObject.obj)
            }
            ParameterType.Action -> {
                // intentionally do nothing
            }
        }
    }

    fun randomize(strength: Double = 0.05) {
        for ((labeledObject, binding) in trackedObjects) {
            // -- only randomize visible parameters
            for (parameter in binding.parameterControls.keys) {
                when (parameter.parameterType) {
                    /* 6) Set up value randomizers */
                    ParameterType.Double -> {
                        val min = parameter.doubleRange!!.start
                        val max = parameter.doubleRange!!.endInclusive
                        val currentValue = (parameter.property as KMutableProperty1<Any, Double>).get(labeledObject.obj)
                        val randomValue = Math.random() * (max - min) + min
                        val newValue = (1.0 - strength) * currentValue + randomValue * strength
                        (parameter.property as KMutableProperty1<Any, Double>).set(labeledObject.obj, newValue)
                    }
                    ParameterType.Int -> {
                        val min = parameter.intRange!!.first
                        val max = parameter.intRange!!.last
                        val currentValue = (parameter.property as KMutableProperty1<Any, Int>).get(labeledObject.obj)
                        val randomValue = Math.random() * (max - min) + min
                        val newValue = ((1.0 - strength) * currentValue + randomValue * strength).roundToInt()
                        (parameter.property as KMutableProperty1<Any, Int>).set(labeledObject.obj, newValue)
                    }
                    ParameterType.Boolean -> {
                        //I am not sure about randomizing boolean values here
                        //(parameter.property as KMutableProperty1<Any, Boolean>).set(labeledObject.obj, (Math.random() < 0.5))
                    }
                    ParameterType.Color -> {
                        val currentValue = (parameter.property as KMutableProperty1<Any, ColorRGBa>).get(labeledObject.obj)
                        val randomValue = ColorRGBa(Math.random(), Math.random(), Math.random(), currentValue.a)
                        val newValue = ColorRGBa((1.0 - strength) * currentValue.r + randomValue.r * strength,
                                (1.0 - strength) * currentValue.g + randomValue.g * strength,
                                (1.0 - strength) * currentValue.b + randomValue.b * strength)

                        (parameter.property as KMutableProperty1<Any, ColorRGBa>).set(labeledObject.obj, newValue)
                    }
                    else -> {
                        // intentionally do nothing
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
                CompartmentState(compartmentsCollapsedByDefault)
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

    override fun afterDraw(drawer: Drawer, program: Program) {
        if (doubleBind) {
            updateControls()
        }
    }
}

@JvmName("addToGui")
fun <T : Any> T.addTo(gui: GUI, label: String? = this.title()): T {
    gui.add(this, label)
    return this
}
