package org.openrndr.extra.gui

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.dialogs.*
import org.openrndr.draw.Drawer
import org.openrndr.events.Event
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.parameters.*
import org.openrndr.internal.Driver
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import org.openrndr.math.mix
import org.openrndr.panel.ControlManager
import org.openrndr.panel.collections.SelectableList
import org.openrndr.panel.collections.SelectableMutableList
import org.openrndr.panel.controlManager
import org.openrndr.panel.elements.*
import org.openrndr.panel.style.*
import java.io.File
import kotlin.math.roundToInt
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1

/** Dear contributor, just in case you are here looking to add a new parameter type.
There is a 6-step incantation to add a new parameter type
0) Add your parameter type to orx-parameters, follow the instructions provided there.

1) Set up a control style in GuiStyles.kt, very likely analogous to the styles already in place.
2) Add control creation code.
3) Add value serialization code to ParameterBindings in orx-parameters, may need to update ParameterValue too.
4) Add value deserialization code to ParameterBindings.
5) Add value randomization code to ParameterBindings.
6) Add control update code.

You can use your editor's search functionality to jump to "1)", "2)".
 */
internal class CompartmentState(var collapsed: Boolean, val parameterValues: MutableMap<String, Any> = mutableMapOf())
private class SidebarState(var hidden: Boolean = false, var collapsed: Boolean = false, var scrollTop: Double = 0.0)

private val persistentCompartmentStates = mutableMapOf<Long, MutableMap<String, CompartmentState>>()
private val persistentSidebarStates = mutableMapOf<Long, SidebarState>()

internal fun compartmentState(): MutableMap<String, CompartmentState> =
    persistentCompartmentStates.getOrPut(Driver.instance.contextID) {
        mutableMapOf()
    }

private fun sidebarState(): SidebarState = persistentSidebarStates.getOrPut(Driver.instance.contextID) {
    SidebarState()
}

private fun <T : Any> getPersistedOrDefault(
    compartmentLabel: String,
    property: KMutableProperty1<Any, T>,
    obj: Any
): T {
    val state = compartmentState()[compartmentLabel]
    if (state == null) {
        return property.get(obj)
    } else {
        @Suppress("UNCHECKED_CAST")
        return (state.parameterValues[property.name] as? T?) ?: return property.get(obj)
    }
}

private fun <T : Any> setAndPersist(compartmentLabel: String, property: KMutableProperty1<Any, T>, obj: Any, value: T) {
    property.set(obj, value)
    compartmentState().getOrPut(compartmentLabel) { CompartmentState(false) }.parameterValues[property.name] = value
}

private fun getPersistedSelectedIndex(compartmentLabel: String, property: KMutableProperty1<*, *>, obj: Any): Int? {
    val state = compartmentState()[compartmentLabel] ?: return null
    return state.parameterValues["${property.name}_selectedIndex"] as? Int
}

private fun setAndPersistSelectedIndex(
    compartmentLabel: String,
    property: KMutableProperty1<*, *>,
    obj: Any,
    value: Int
) {
    compartmentState().getOrPut(compartmentLabel) { CompartmentState(false) }.parameterValues["${property.name}_selectedIndex"] = value
}

private fun getPersistedListElements(compartmentLabel: String, property: KMutableProperty1<*, *>, obj: Any): List<*>? {
    val state = compartmentState()[compartmentLabel] ?: return null
    return state.parameterValues["${property.name}_elements"] as? List<*>
}

private fun setAndPersistListElements(
    compartmentLabel: String,
    property: KMutableProperty1<*, *>,
    obj: Any,
    value: List<*>
) {
    compartmentState().getOrPut(compartmentLabel) { CompartmentState(false) }.parameterValues["${property.name}_elements"] = value
}

private val logger = KotlinLogging.logger { }


class GUIAppearance(
    val baseColor: ColorRGBa = ColorRGBa.GRAY.opacify(0.99),
    val barWidth: Int = 200
)

class GUIEvent()

/**
 * A GUI with controls for the parameters of objects, see orx-parameters. The parameters are bound by [bindings],
 * which can be shared with other code, like remote controls; objects added to it directly are shown too.
 */
@Suppress("unused", "UNCHECKED_CAST")
open class GUI(
    val appearance: GUIAppearance = GUIAppearance(),
    val defaultStyles: List<StyleSheet> = defaultStyles(),
    /** the bindings of the parameters of the objects that the GUI shows */
    val bindings: ParameterBindings = ParameterBindings(),
) : Extension {

    /**
     * Represents an event that is triggered when the GUI state has been loaded or restored
     *
     * Example scenarios where this event is triggered include:
     * - After loading parameters from a file via the `loadParameters` function.
     */
    val loaded = Event<GUIEvent>()

    private var onChangeListener: ((name: String, value: Any?) -> Unit)? = null
    override var enabled = true

    var listenToProduceAssetsEvent = true

    var visible = true
        set(value) {
            field = value
            if (field) {
                panel?.body?.classes?.remove(collapsed)
            } else {
                panel?.body?.classes?.add(collapsed)
            }
            sidebarState().hidden = !field
        }

    var compartmentsCollapsedByDefault = true
    var doubleBind = true
    var defaultSaveFolder = "gui-parameters"
    var persistState = true
    var enableSideCanvas = false
    var showToolbar = true

    /**
     * Show a header above every compartment that collapses and expands it when clicked. Must be set before the GUI
     * is installed with `extend()`.
     */
    var showCompartmentHeaders = true

    /**
     * The canvas created by [GUILayoutScope.sideCanvas], see [enableSideCanvas].
     */
    var canvas: Canvas? = null
        private set

    /**
     * The style sheets for the GUI's elements, applied on top of [defaultStyles]. Must be set before the GUI is
     * installed with `extend()`.
     */
    var styles: List<StyleSheet> = guiStyles(appearance)

    /**
     * The layout that builds the GUI's element tree, see [GUILayout]. Must be set before the GUI is installed with
     * `extend()`.
     */
    var layout: GUILayout = defaultGUILayout

    private var panel: ControlManager? = null

    // Randomize button
    private var shiftDown = false
    private var randomizeButton: Button? = null

    fun onChange(listener: (name: String, value: Any?) -> Unit) {
        onChangeListener = listener
    }

    val collapsed = ElementClass("collapsed")

    private var sidebar: Div? = null
    private var compartmentSelector: DropdownButton? = null
    private val compartments = mutableMapOf<String, Div>()
    private val elementsForObject = mutableMapOf<Any, List<Element>>()

    /** the controls of the parameters of the bound objects, and the elements that hold the controls of nested objects */
    private val parameterControls = java.util.IdentityHashMap<ParameterBindings.Binding, MutableMap<Parameter, Element>>()
    private val nestedContents = java.util.IdentityHashMap<ParameterBindings.NestedBinding, Div>()

    init {
        bindings.added.listen { added(it) }
        bindings.removed.listen { removed(it) }
        bindings.rebound.listen { rebound(it) }
    }

    private fun addCollapsible(parent: Element, binding: ParameterBindings.Binding) {
        parent.append {

            val label = binding.label

            val h3Header = if (showCompartmentHeaders) h3 { label } else null
            val collapsible = div("compartment") {
                addControls(binding)
            }
            compartments[label] = collapsible
            elementsForObject[binding.obj] = listOfNotNull(h3Header, collapsible)

            val collapseState = compartmentState().getOrPut(label) { CompartmentState(compartmentsCollapsedByDefault) }
            if (collapseState.collapsed) {
                collapsible.classes.add(collapsed)
            }

            h3Header?.mouse?.pressed?.listen {
                it.cancelPropagation()
            }
            h3Header?.mouse?.clicked?.listen { me ->

                if (KeyModifier.CTRL in me.modifiers) {
                    selectCompartment(label)
                } else {

                    if (collapsed in collapsible.classes) {
                        collapsible.classes.remove(collapsed)
                        collapseState.collapsed = false
                    } else {
                        collapsible.classes.add(collapsed)
                        collapseState.collapsed = true
                    }
                }
            }
        }
    }

    /**
     * Expand the compartment with [label] and collapse all other compartments.
     */
    fun selectCompartment(label: String) {
        // only compartments, the states of nested groups are kept
        for (binding in bindings.bindings) {
            compartmentState()[binding.label]?.collapsed = binding.label != label
        }
        for ((compartmentLabel, compartment) in compartments) {
            if (compartmentLabel == label) {
                compartment.classes.remove(collapsed)
            } else {
                compartment.classes.add(collapsed)
            }
        }
        compartmentSelector?.let { selector ->
            selector.value = selector.items().find { it.label == label }
        }
    }

    override fun setup(program: Program) {
        if (persistState) {
            val guiState = File(defaultSaveFolder, "${program.name}-latest.json")
            if (guiState.exists()) {
                loadParameters(guiState)
            }
        }

        program.produceAssets.listen {
            if (listenToProduceAssetsEvent) {
                val folderFile = File(defaultSaveFolder)
                val targetFile = File(defaultSaveFolder, "${it.assetMetadata.assetBaseName}.json")
                if (folderFile.exists() && folderFile.isDirectory) {
                    logger.info { "Saving parameters to '${targetFile.absolutePath}" }
                    saveParameters(targetFile)
                } else {
                    if (folderFile.mkdirs()) {
                        logger.info { "Saving parameters to '${targetFile.absolutePath}" }
                        saveParameters(targetFile)
                    } else {
                        logger.error { "Could not save parameters because could not create directory ${folderFile.absolutePath}" }
                    }
                }
            }
        }

        program.keyboard.keyDown.listen {
            if (it.key == KEY_F11) {
                visible = !visible
            }

            if (it.key == KEY_LEFT_SHIFT) {
                shiftDown = true
                randomizeButton?.classes?.add(ElementClass("randomize-strong"))
            }
        }

        program.keyboard.keyUp.listen {
            if (it.key == KEY_LEFT_SHIFT) {
                shiftDown = false
                randomizeButton?.classes?.remove(ElementClass("randomize-strong"))
            }
        }

        sidebar = null
        compartmentSelector = null
        compartments.clear()
        panel = program.controlManager(defaultStyles = defaultStyles) {
            styleSheets(styles)
            layout {
                GUILayoutScope(this@GUI, this).layout()
            }
        }
        checkNotNull(sidebar) { "the GUI layout did not create a sidebar, call sidebar() in the layout" }
        // trigger class updates
        visible = visible

        program.extend(panel ?: error("no panel"))
    }

    //<editor-fold desc="Layout building blocks">
    internal fun buildToolbar(parent: Element): Div = parent.div("toolbar") {
        randomizeButton = button {
            label = "Randomize"
            clicked {
                randomize(strength = if (shiftDown) .75 else .05)
            }
        }
        button {
            label = "Load"
            clicked {
                openFileDialog(
                    supportedExtensions = listOf("GUI parameters" to listOf("json")),
                    contextID = "gui.parameters"
                ) {
                    loadParameters(it)
                }
            }
        }
        button {
            label = "Save"
            clicked {
                val defaultPath = getDefaultPathForContext(contextID = "gui.parameters")

                if (defaultPath == null) {
                    val local = File(".")
                    val parameters = File(local, defaultSaveFolder)
                    if (parameters.exists() && parameters.isDirectory) {
                        setDefaultPathForContext(
                            contextID = "gui.parameters",
                            file = parameters
                        )
                    } else {
                        if (parameters.mkdirs()) {
                            setDefaultPathForContext(
                                contextID = "gui.parameters",
                                file = parameters
                            )
                        } else {
                            logger.warn { "Could not create directory ${parameters.absolutePath}" }
                        }
                    }
                }

                saveFileDialog(
                    suggestedFilename = "parameters.json",
                    contextID = "gui.parameters",
                    supportedExtensions = listOf("GUI parameters" to listOf("json"))
                ) {
                    saveParameters(it)
                }
            }
        }
    }

    internal fun buildCollapseBorder(parent: Element): Div {
        val collapseBorder = parent.div("collapse-border") {}
        collapseBorder.mouse.pressed.listen {
            it.cancelPropagation()
        }
        collapseBorder.mouse.clicked.listen {
            sidebar?.let { sidebar ->
                if (collapsed in sidebar.classes) {
                    sidebar.classes.remove(collapsed)
                    sidebarState().collapsed = false
                } else {
                    sidebar.classes.add(collapsed)
                    sidebarState().collapsed = true
                }
            }
            it.cancelPropagation()
        }
        return collapseBorder
    }

    internal fun buildSidebar(parent: Element): Div {
        check(sidebar == null) { "sidebar() can only be called once in a GUI layout" }
        val sidebar = parent.div("sidebar") {
            id = "sidebar"
            scrollTop = sidebarState().scrollTop
            for (binding in bindings.bindings) {
                addCollapsible(this, binding)
            }
        }
        this.sidebar = sidebar
        sidebar.mouse.scrolled.listen {
            sidebarState().scrollTop = sidebar.scrollTop
        }
        if (sidebarState().collapsed) {
            sidebar.classes.add(collapsed)
        }
        sidebar.scrollTop = sidebarState().scrollTop
        return sidebar
    }

    internal fun buildCompartmentSelector(parent: Element): DropdownButton {
        check(compartmentSelector == null) { "compartmentSelector() can only be called once in a GUI layout" }
        val selector = parent.dropdownButton("compartment-selector", label = "") {
            for (binding in bindings.bindings) {
                item {
                    label = binding.label
                }
            }
            events.valueChanged.listen {
                selectCompartment(it.value.label)
            }
        }
        compartmentSelector = selector
        // keep exactly one compartment expanded, preferably the first one that already is
        val selected = bindings.bindings.firstOrNull { compartmentState()[it.label]?.collapsed == false }
            ?: bindings.bindings.firstOrNull()
        selected?.let { selectCompartment(it.label) }
        return selector
    }

    internal fun buildSideCanvas(parent: Element): Canvas {
        val canvas = parent.canvas("full-canvas") {}
        this.canvas = canvas
        return canvas
    }
    //</editor-fold>

    /* 2) control creation. create control, set label, set range, setup event-handler, load values */
    //<editor-fold desc="2) Control creation">
    private fun Div.addControls(binding: ParameterBindings.Binding) {
        val controls = parameterControls.getOrPut(binding) { mutableMapOf() }
        for (parameter in binding.parameters) {
            if (parameter.parameterType == ParameterType.Nested) {
                binding.nested[parameter]?.let { addNested(parameter, it) }
            } else {
                controls[parameter] = addControl(binding, parameter)
            }
        }
    }

    /**
     * Add a group with the controls of a nested object, collapsed and expanded by clicking its header
     */
    private fun Div.addNested(parameter: Parameter, nested: ParameterBindings.NestedBinding): Div =
        div("nested") {
            val header = h3("nested-header") { parameter.label }
            val content = div("nested-content") {
                nested.binding?.let { addControls(it) }
            }
            nestedContents[nested] = content
            val collapseState = compartmentState().getOrPut(nested.label) { CompartmentState(false) }
            if (collapseState.collapsed) {
                content.classes.add(collapsed)
            }
            header.mouse.pressed.listen {
                it.cancelPropagation()
            }
            header.mouse.clicked.listen {
                if (collapsed in content.classes) {
                    content.classes.remove(collapsed)
                    collapseState.collapsed = false
                } else {
                    content.classes.add(collapsed)
                    collapseState.collapsed = true
                }
            }
        }

    /** a control changed [parameter] of [binding] to [value], the listeners of the GUI and of [bindings] hear it */
    private fun changedByControl(binding: ParameterBindings.Binding, parameter: Parameter, value: Any?) {
        onChangeListener?.invoke(parameter.name, value)
        bindings.changed.trigger(ParameterBindings.Change(binding, parameter, binding.get(parameter)))
    }

    private fun Div.addControl(compartment: ParameterBindings.Binding, parameter: Parameter): Element {
        val obj = compartment.obj

        return when (parameter.parameterType) {
            ParameterType.Nested -> error("nested parameters are added by addControls")

            ParameterType.Int -> {
                slider {
                    label = parameter.label
                    range = Range(parameter.intRange!!.first.toDouble(), parameter.intRange!!.last.toDouble())
                    precision = 0
                    events.valueChanged.listen {
                        setAndPersist(
                            compartment.label,
                            parameter.property as KMutableProperty1<Any, Int>,
                            obj,
                            it.newValue.toInt()
                        )
                        (parameter.property as KMutableProperty1<Any, Int>).set(obj, value.toInt())
                        changedByControl(compartment, parameter, it.newValue)
                    }
                    getPersistedOrDefault(
                        compartment.label,
                        parameter.property as KMutableProperty1<Any, Int>,
                        obj
                    ).let {
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
                        setAndPersist(
                            compartment.label,
                            parameter.property as KMutableProperty1<Any, Double>,
                            obj,
                            it.newValue
                        )
                        changedByControl(compartment, parameter, it.newValue)
                    }
                    getPersistedOrDefault(
                        compartment.label,
                        parameter.property as KMutableProperty1<Any, Double>,
                        obj
                    ).let {
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
                        changedByControl(compartment, parameter, null)
                    }
                }
            }

            ParameterType.Boolean -> {
                toggle {
                    label = parameter.label
                    events.valueChanged.listen {
                        value = it.newValue
                        setAndPersist(
                            compartment.label,
                            parameter.property as KMutableProperty1<Any, Boolean>,
                            obj,
                            it.newValue
                        )
                        changedByControl(compartment, parameter, it.newValue)
                    }
                    getPersistedOrDefault(
                        compartment.label,
                        parameter.property as KMutableProperty1<Any, Boolean>,
                        obj
                    ).let {
                        value = it
                        setAndPersist(compartment.label, parameter.property as KMutableProperty1<Any, Boolean>, obj, it)
                    }
                }
            }

            ParameterType.Text -> {
                textfield {
                    label = parameter.label
                    events.valueChanged.listen {
                        setAndPersist(
                            compartment.label,
                            parameter.property as KMutableProperty1<Any, String>,
                            obj,
                            it.newValue
                        )
                        changedByControl(compartment, parameter, it.newValue)
                    }
                    getPersistedOrDefault(
                        compartment.label,
                        parameter.property as KMutableProperty1<Any, String>,
                        obj
                    ).let {
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
                        changedByControl(compartment, parameter, it.color)
                    }
                    getPersistedOrDefault(
                        compartment.label,
                        parameter.property as KMutableProperty1<Any, ColorRGBa>,
                        obj
                    ).let {
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
                    label = parameter.label

                    events.valueChanged.listen {
                        setAndPersist(
                            compartment.label,
                            parameter.property as KMutableProperty1<Any, Vector2>,
                            obj,
                            it.newValue
                        )
                        changedByControl(compartment, parameter, it.newValue)
                    }
                }
            }

            ParameterType.Path -> {
                button {
                    label = "Load ${parameter.label}"
                    clicked {

                        if (parameter.pathIsDirectory == false) {
                            openFileDialog(
                                supportedExtensions = parameter.pathExtensions?.let { listOf("supported extensions" to it.toList()) }
                                    ?: emptyList(),
                                contextID = parameter.pathContext ?: "null"
                            ) {
                                val resolvedPath = if (parameter.absolutePath == true) {
                                    it.absolutePath
                                } else {
                                    it.relativeTo(File(".").absoluteFile).path
                                }
                                setAndPersist(
                                    compartment.label,
                                    parameter.property as KMutableProperty1<Any, String>,
                                    obj,
                                    resolvedPath
                                )
                                changedByControl(compartment, parameter, resolvedPath)
                            }
                        } else {
                            openFolderDialog(contextID = parameter.pathContext ?: "null") {
                                val resolvedPath = if (parameter.absolutePath == true) {
                                    it.absolutePath
                                } else {
                                    it.relativeTo(File(".").absoluteFile).path
                                }
                                setAndPersist(
                                    compartment.label,
                                    parameter.property as KMutableProperty1<Any, String>,
                                    obj,
                                    resolvedPath
                                )
                                changedByControl(compartment, parameter, resolvedPath)
                            }
                        }
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
                        changedByControl(compartment, parameter, it.newValue)
                    }
                    getPersistedOrDefault(
                        compartment.label,
                        parameter.property as KMutableProperty1<Any, MutableList<Double>>,
                        obj
                    ).let {
                        value = it
                        setAndPersist(
                            compartment.label,
                            parameter.property as KMutableProperty1<Any, MutableList<Double>>,
                            obj,
                            it
                        )
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
                            it.newValue
                        )

                        changedByControl(compartment, parameter, it.newValue)
                    }
                    getPersistedOrDefault(
                        compartment.label,
                        parameter.property as KMutableProperty1<Any, Vector2>,
                        obj
                    ).let {
                        value = it
                        setAndPersist(compartment.label, parameter.property as KMutableProperty1<Any, Vector2>, obj, it)
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
                            it.newValue
                        )

                        changedByControl(compartment, parameter, it.newValue)
                    }
                    getPersistedOrDefault(
                        compartment.label,
                        parameter.property as KMutableProperty1<Any, Vector3>,
                        obj
                    ).let {
                        value = it
                        setAndPersist(compartment.label, parameter.property as KMutableProperty1<Any, Vector3>, obj, it)
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
                            it.newValue
                        )

                        changedByControl(compartment, parameter, it.newValue)
                    }
                    getPersistedOrDefault(
                        compartment.label,
                        parameter.property as KMutableProperty1<Any, Vector4>,
                        obj
                    ).let {
                        value = it
                        setAndPersist(compartment.label, parameter.property as KMutableProperty1<Any, Vector4>, obj, it)
                    }
                }
            }

            ParameterType.List -> {
                dropdownButton {
                    label = parameter.label

                    val prop = parameter.property!!

                    @Suppress("UNCHECKED_CAST")
                    val propTyped = prop as KMutableProperty1<Any, Any>
                    val selectableList = propTyped.get(obj) as SelectableList<*>
                    val optionsList = selectableList.toList()

                    getPersistedSelectedIndex(
                        compartment.label,
                        prop,
                        obj
                    )?.let { persistedIndex ->
                        selectableList.selectedIndex = persistedIndex.coerceAtMost(optionsList.lastIndex)
                    }

                    optionsList.forEach { value ->
                        item {
                            label = value.toString()
                            data = value
                        }
                    }
                    events.valueChanged.listen {
                        val currentSelectableList = propTyped.get(obj) as SelectableList<*>
                        val newIndex = optionsList.indexOf(it.value.data)
                        currentSelectableList.selectedIndex = newIndex
                        setAndPersistSelectedIndex(
                            compartment.label,
                            prop,
                            obj,
                            newIndex
                        )
                        changedByControl(compartment, parameter, currentSelectableList.selected)
                    }
                    val selectedItem = selectableList.selected
                    value = items().find { it.data == selectedItem }
                }
            }

            ParameterType.MutableList -> {
                dropdownButton {
                    label = parameter.label

                    val prop = parameter.property!!

                    @Suppress("UNCHECKED_CAST")
                    val propTyped = prop as KMutableProperty1<Any, Any>
                    val selectableMutableList = propTyped.get(obj) as SelectableMutableList<*>

                    getPersistedListElements(
                        compartment.label,
                        prop,
                        obj
                    )?.let { persistedElements ->
                        selectableMutableList.clear()
                        @Suppress("UNCHECKED_CAST")
                        (selectableMutableList as SelectableMutableList<Any>).addAll(persistedElements as Collection<Any>)
                    }

                    getPersistedSelectedIndex(
                        compartment.label,
                        prop,
                        obj
                    )?.let { persistedIndex ->
                        selectableMutableList.selectedIndex =
                            persistedIndex.coerceAtMost(selectableMutableList.lastIndex)
                    }

                    selectableMutableList.forEach { value ->
                        item {
                            label = value.toString()
                            data = value
                        }
                    }
                    fun rebuildItems() {
                        children.filterIsInstance<Item>().forEach { remove(it) }
                        selectableMutableList.forEach { value ->
                            item {
                                label = value.toString()
                                data = value
                            }
                        }
                        val selectedItem = selectableMutableList.selected
                        value = items().find { it.data == selectedItem }
                    }

                    selectableMutableList.changed.listen {
                        rebuildItems()
                        draw.dirty = true
                        setAndPersistListElements(
                            compartment.label, prop, obj, selectableMutableList.toList()
                        )
                    }

                    events.valueChanged.listen {
                        val currentSelectableMutableList = propTyped.get(obj) as SelectableMutableList<*>
                        val newIndex = selectableMutableList.indexOf(it.value.data)
                        currentSelectableMutableList.selectedIndex = newIndex
                        setAndPersistSelectedIndex(
                            compartment.label, prop, obj, newIndex
                        )
                        changedByControl(compartment, parameter, currentSelectableMutableList.selected)
                    }

                    val selectedItem = selectableMutableList.selected
                    value = items().find { it.data == selectedItem }
                }
            }

            ParameterType.Option -> {
                dropdownButton {
                    val enumProperty = parameter.property as KMutableProperty1<Any, Enum<*>>
                    val value = enumProperty.get(obj)
                    label = parameter.label
                    // -- this is dirty, but it is the only way to get the constants for arbitrary enums
                    // -- (that I know of, at least)
                    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN") val jEnum = value as java.lang.Enum<*>
                    // -- we don't use the property syntax here because that leads to compilation errors
                    val constants = jEnum.getDeclaringClass().getEnumConstants()
                    constants.forEach {
                        item {
                            label = it.name
                            data = it
                        }
                    }
                    events.valueChanged.listen {
                        setAndPersist(
                            compartment.label,
                            parameter.property as KMutableProperty1<Any, Enum<*>>,
                            obj,
                            it.value.data as? Enum<*> ?: error("no data")
                        )

                        changedByControl(compartment, parameter, it.value.data as? Enum<*> ?: error("no data")
                        )
                    }
                    getPersistedOrDefault(
                        compartment.label,
                        parameter.property as KMutableProperty1<Any, Enum<*>>,
                        obj
                    ).let { enum ->
                        (this@dropdownButton).value = items().find { item -> item.data == enum }
                            ?: error("no matching item found")
                        setAndPersist(
                            compartment.label,
                            parameter.property as KMutableProperty1<Any, Enum<*>>,
                            obj,
                            enum
                        )
                    }
                }
            }
        }
    }
    //</editor-fold>

    private fun updateControls() {
        for (binding in bindings.all()) {
            for ((parameter, control) in parameterControls[binding] ?: continue) {
                updateControl(binding, parameter, control)
            }
        }
    }

    /** the values of parameters as they are saved, see [ParameterBindings.toObject] */
    typealias ParameterValue = org.openrndr.extra.parameters.ParameterValue

    /**
     * Can be called by the user to obtain an object to be serialized
     * externally. This allows the user to combine custom data with gui
     * state and save it all to one file. Complements `.fromObject()`.
     */
    fun toObject(): Map<String, Map<String, ParameterValue>> = bindings.toObject()

    fun saveParameters(file: File) {
        bindings.save(file)
    }

    /**
     * Can be called by the user to update the gui using an object
     * deserialized externally. Allows the user to load a larger json object,
     * deserialize it, and use part of it to update the GUI.
     * Complements `.toObject()`.
     */
    fun fromObject(labeledValues: Map<String, Map<String, ParameterValue>>) {
        bindings.fromObject(labeledValues)
        updateControls()
        persistValues()
    }

    fun loadParameters(file: File) {
        bindings.load(file)
        updateControls()
        persistValues()
        loaded.trigger(GUIEvent())
    }

    /**
     * Keeps the values of the parameters for when the GUI is created again in the same context, after values were
     * set by other means than the controls
     */
    private fun persistValues() {
        if (!persistState) return
        for (binding in bindings.all()) {
            val values = compartmentState().getOrPut(binding.label) { CompartmentState(false) }.parameterValues
            for (parameter in binding.valueParameters) {
                val property = parameter.property ?: continue
                val value = binding.get(parameter) ?: continue
                when (parameter.parameterType) {
                    ParameterType.List -> values["${property.name}_selectedIndex"] = (value as Selectable).selectedIndex
                    ParameterType.MutableList -> {
                        values["${property.name}_selectedIndex"] = (value as Selectable).selectedIndex
                        values["${property.name}_elements"] = (value as List<*>).toList()
                    }
                    else -> values[property.name] = value
                }
            }
        }
    }

    private fun updateControl(labeledObject: ParameterBindings.Binding, parameter: Parameter, control: Element) {
        when (parameter.parameterType) {
            /* 5) Update control from property value */
            ParameterType.Double -> {
                (control as Slider).value =
                    (parameter.property as KMutableProperty1<Any, Double>).get(labeledObject.obj)
            }

            ParameterType.Int -> {
                (control as Slider).value =
                    (parameter.property as KMutableProperty1<Any, Int>).get(labeledObject.obj).toDouble()
            }

            ParameterType.Text -> {
                (control as Textfield).value =
                    (parameter.property as KMutableProperty1<Any, String>).get(labeledObject.obj)
            }

            ParameterType.Color -> {
                (control as ColorpickerButton).color =
                    (parameter.property as KMutableProperty1<Any, ColorRGBa>).get(labeledObject.obj)
            }

            ParameterType.XY -> {
                (control as XYPad).value =
                    (parameter.property as KMutableProperty1<Any, Vector2>).get(labeledObject.obj)
            }

            ParameterType.DoubleList -> {
                (control as SequenceEditor).value =
                    (parameter.property as KMutableProperty1<Any, MutableList<Double>>).get(labeledObject.obj)
            }

            ParameterType.Boolean -> {
                (control as Toggle).value =
                    (parameter.property as KMutableProperty1<Any, Boolean>).get(labeledObject.obj)
            }

            ParameterType.Vector2 -> {
                (control as SlidersVector2).value =
                    (parameter.property as KMutableProperty1<Any, Vector2>).get(labeledObject.obj)
            }

            ParameterType.Vector3 -> {
                (control as SlidersVector3).value =
                    (parameter.property as KMutableProperty1<Any, Vector3>).get(labeledObject.obj)
            }

            ParameterType.Vector4 -> {
                (control as SlidersVector4).value =
                    (parameter.property as KMutableProperty1<Any, Vector4>).get(labeledObject.obj)
            }

            ParameterType.Option -> {
                val ddb = control as DropdownButton
                ddb.value = ddb.items().find { item ->
                    item.data == (parameter.property as KMutableProperty1<Any, Enum<*>>).get(labeledObject.obj)
                } ?: error("could not find item")
            }

            ParameterType.Path -> {

            }

            ParameterType.List -> {
                val ddb = control as DropdownButton
                val prop = parameter.property!!

                @Suppress("UNCHECKED_CAST")
                val propTyped = prop as KMutableProperty1<Any, Any>
                val selectableList = propTyped.get(labeledObject.obj) as SelectableList<*>?
                val selectedItem = selectableList?.selected
                ddb.value = ddb.items().find { item -> item.data == selectedItem }
            }

            ParameterType.MutableList -> {
                val ddb = control as DropdownButton
                val prop = parameter.property!!

                @Suppress("UNCHECKED_CAST")
                val propTyped = prop as KMutableProperty1<Any, Any>
                val selectableMutableList = propTyped.get(labeledObject.obj) as SelectableMutableList<*>?
                val selectedItem = selectableMutableList?.selected
                ddb.value = ddb.items().find { item -> item.data == selectedItem }
            }

            ParameterType.Action, ParameterType.Nested -> {
                // intentionally do nothing
            }
        }
    }

    /**
     * Moves the values of the parameters towards random values by [strength], see [ParameterBindings.randomize]
     */
    fun randomize(strength: Double = 0.05) {
        bindings.randomize(strength)
        updateControls()
        persistValues()
    }

    /**
     * Add an object to the GUI
     * @param objectWithParameters an object of a class that annotated parameters
     * @param label an optional label that overrides the label supplied in a [Description] annotation
     * @return pass-through of [objectWithParameters]
     */
    fun <T : Any> add(objectWithParameters: T, label: String? = objectWithParameters.title()): T {
        bindings.add(objectWithParameters, label)
        return objectWithParameters
    }

    /** builds the compartment of an object that was added to [bindings] */
    private fun added(binding: ParameterBindings.Binding) {
        compartmentState().getOrPut(binding.label) { CompartmentState(compartmentsCollapsedByDefault) }
        compartmentSelector?.let { selector ->
            selector.item {
                this.label = binding.label
            }
            // only the selected compartment is expanded, the new compartment is selected if there is none
            if (selector.value == null) {
                selectCompartment(binding.label)
            } else {
                compartmentState()[binding.label]?.collapsed = true
            }
        }
        sidebar?.let {
            addCollapsible(it, binding)
        }
    }

    /**
     * Rebuilds the controls of a nested object that is bound again. The values persisted for the replaced object and
     * the objects nested in it are dropped, a new object keeps its own values.
     */
    private fun rebound(nested: ParameterBindings.NestedBinding) {
        compartmentState().forEach { (label, state) ->
            if (label == nested.label || label.startsWith("${nested.label}/")) {
                state.parameterValues.clear()
            }
        }
        nestedContents[nested]?.let { content ->
            for (child in content.children.toList()) {
                content.remove(child)?.close()
            }
            nested.binding?.let { content.addControls(it) }
            content.requestRedraw()
        }
        onChangeListener?.invoke(nested.property.name, nested.obj)
    }

    /**
     * Remove an object from the GUI.
     *
     * @param objectWithParameters the original object added to the GUI using the `add()` method.
     */
    fun <T : Any> remove(objectWithParameters: T) {
        bindings.remove(objectWithParameters)
    }

    /** removes the compartment of an object that was removed from [bindings] */
    private fun removed(binding: ParameterBindings.Binding) {
        compartmentState().keys.removeAll { it == binding.label || it.startsWith("${binding.label}/") }
        compartments.remove(binding.label)
        compartmentSelector?.let { selector ->
            selector.items().find { it.label == binding.label }?.let { item ->
                selector.remove(item)
                if (selector.value == item) {
                    selector.value = null
                    bindings.bindings.firstOrNull()?.let { selectCompartment(it.label) }
                }
            }
        }
        elementsForObject.remove(binding.obj)?.forEach {
            it.parent?.remove(it)?.close()
        }
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
        bindings.rebindNested()
        if (doubleBind) {
            updateControls()
        }
    }

    override fun shutdown(program: Program) {
        if (persistState) {
            val folderFile = File(defaultSaveFolder)
            if (folderFile.exists() && folderFile.isDirectory) {
                saveParameters(File(defaultSaveFolder, "${program.name}-latest.json"))
            } else {
                if (folderFile.mkdirs()) {
                    saveParameters(File(defaultSaveFolder, "${program.name}-latest.json"))
                } else {
                    logger.error { "Could not persist GUI state because could not create directory ${folderFile.absolutePath}" }
                }
            }
        }
        loaded.close()
    }
}

@JvmName("addToGui")
fun <T : Any> T.addTo(gui: GUI, label: String? = this.title()): T {
    gui.add(this, label)
    return this
}
