package org.openrndr.extra.parameters

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.color.ColorRGBa
import org.openrndr.events.Event
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import org.openrndr.math.mix
import java.io.File
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1

private val logger = KotlinLogging.logger { }

/** the name of [this], the name of its property or its function, see [ParameterBindings.Binding.parameter] */
val Parameter.name: String get() = property?.name ?: function?.name ?: label

/**
 * The parameters of objects, bound by labels: their values are read, set, randomized, saved and loaded by the label
 * of an object and the name of a parameter. This is what a GUI shows, without a GUI: programs that run without one,
 * and other front ends, like remote controls or MIDI controllers, use it in the same way.
 *
 * Objects are added with [add], under a label that is unique. The objects held by properties with a
 * [NestedParameter] annotation are bound too, labeled `<label of the parent>/<label of the nested parameter>`; when
 * such a property holds another object, [rebindNested] binds that object.
 *
 * ```
 * val bindings = ParameterBindings()
 * val settings = bindings.add(Settings(), "settings")
 * bindings.load(File("parameters.json"))
 * bindings["settings", "radius"] = 20.0
 * ```
 */
class ParameterBindings {
    /**
     * The binding of [obj], an object with [parameters], under [label]
     * @property nested the bindings of the objects held by the properties of its [ParameterType.Nested] parameters
     */
    class Binding internal constructor(
        val label: String,
        val obj: Any,
        val parameters: List<Parameter>,
        val nested: Map<Parameter, NestedBinding>
    ) {
        /** the parameter with [name], the name of its property or function */
        fun parameter(name: String): Parameter? = parameters.find { it.parameterType != ParameterType.Nested && it.name == name }

        /** the parameters that have values or actions, all but the nested parameters */
        val valueParameters: List<Parameter> get() = parameters.filter { it.parameterType != ParameterType.Nested }

        /** the value of [parameter] */
        fun get(parameter: Parameter): Any? {
            @Suppress("UNCHECKED_CAST")
            return (parameter.property as? KMutableProperty1<Any, Any?>)?.get(obj)
        }

        /** the value of the parameter with [name] */
        operator fun get(name: String): Any? = parameter(name)?.let { get(it) }
    }

    /**
     * The binding of the object held by the property of a [ParameterType.Nested] parameter of [parent]
     * @property label the label of the nested object, `<label of the parent>/<label of the nested parameter>`
     */
    class NestedBinding internal constructor(val label: String, val parent: Any, val parameter: Parameter) {
        @Suppress("UNCHECKED_CAST")
        val property: KProperty1<Any, Any?> = parameter.nestedProperty as KProperty1<Any, Any?>

        /** the bound object, null when the property held null */
        var obj: Any? = null
            internal set

        /** the binding of [obj] */
        var binding: Binding? = null
            internal set

        /** the object the property currently holds */
        fun current(): Any? = property.get(parent)
    }

    /** a change of the value of [parameter] of [binding], or of the object of a nested parameter */
    class Change(val binding: Binding, val parameter: Parameter, val value: Any?)

    private val bound = mutableListOf<Binding>()

    /** the bindings of the objects that are added, in the order they are added */
    val bindings: List<Binding> get() = bound

    /** an object was added */
    val added = Event<Binding>("parameter-bindings-added")

    /** an object was removed */
    val removed = Event<Binding>("parameter-bindings-removed")

    /** the property of a nested parameter holds another object, which is bound, see [rebindNested] */
    val rebound = Event<NestedBinding>("parameter-bindings-rebound")

    /**
     * Values were changed through the bindings: by [set], [invoke], [randomize], [fromObject] or [load]; for nested
     * parameters by [rebindNested]. Front ends trigger it for the changes they make, like the GUI of orx-gui for its
     * controls. Changes made to the objects directly are not noticed.
     */
    val changed = Event<Change>("parameter-bindings-changed")

    /** the values were loaded, by [fromObject] or [load] */
    val loaded = Event<ParameterBindings>("parameter-bindings-loaded")

    /** the bindings of the objects and of the objects nested in them, parents before the objects nested in them */
    fun all(): List<Binding> {
        val result = mutableListOf<Binding>()
        fun visit(binding: Binding) {
            result.add(binding)
            for (nested in binding.nested.values) visit(nested.binding ?: continue)
        }
        bound.forEach(::visit)
        return result
    }

    /** the binding with [label], of an object or a nested object */
    fun binding(label: String): Binding? = all().find { it.label == label }

    /** the binding of [obj] */
    fun binding(obj: Any): Binding? = all().find { it.obj === obj }

    /** [label], or `<label> / 2`, `<label> / 3` and so on when it is taken */
    private fun uniqueLabel(label: String): String =
        bound.find { it.label == label }?.let { taken ->
            uniqueLabel(Regex("(.*) / ([0-9]+)").matchEntire(taken.label)?.let {
                "${it.groupValues[1]} / ${1 + it.groupValues[2].toInt()}"
            } ?: "$label / 2")
        } ?: label

    /**
     * Adds [obj] under [label], or the title of its [Description] annotation; the label is made unique. Objects
     * without parameters are not bound.
     * @return the binding, null when [obj] has no parameters
     */
    fun add(obj: Any, label: String? = obj.title()): Binding? {
        val parameters = obj.listParameters()
        if (parameters.isEmpty()) return null
        val binding = bind(uniqueLabel(label ?: "No name"), obj, parameters)
        bound.add(binding)
        added.trigger(binding)
        return binding
    }

    /** removes [obj], an object that was added */
    fun remove(obj: Any) {
        for (binding in bound.filter { it.obj === obj }) {
            bound.remove(binding)
            removed.trigger(binding)
        }
    }

    /** binds [parameters] of [obj], and the objects nested in it */
    private fun bind(label: String, obj: Any, parameters: List<Parameter>): Binding {
        val nested = parameters.filter { it.parameterType == ParameterType.Nested }.associateWith { parameter ->
            NestedBinding("$label/${parameter.label}", obj, parameter).apply {
                this.obj = current()
                binding = this.obj?.let { bind(this.label, it, parameter.nestedParameters ?: emptyList()) }
            }
        }
        return Binding(label, obj, parameters, nested)
    }

    /**
     * Binds the objects that the properties of nested parameters hold when they hold other objects than the ones
     * that are bound, with the objects nested in them
     * @return the nested bindings that were bound again
     */
    fun rebindNested(): List<NestedBinding> {
        val result = mutableListOf<NestedBinding>()
        fun visit(binding: Binding) {
            for ((parameter, nested) in binding.nested) {
                val current = nested.current()
                if (current !== nested.obj) {
                    nested.obj = current
                    nested.binding = current?.let { bind(nested.label, it, it.listParameters()) }
                    result.add(nested)
                    rebound.trigger(nested)
                    changed.trigger(Change(binding, parameter, current))
                } else {
                    nested.binding?.let(::visit)
                }
            }
        }
        bound.forEach(::visit)
        return result
    }

    /** the value of the parameter with [name] of the object with [label] */
    operator fun get(label: String, name: String): Any? = binding(label)?.get(name)

    /** sets the parameter with [name] of the object with [label] to [value] */
    operator fun set(label: String, name: String, value: Any?) {
        val binding = binding(label) ?: error("no object with label '$label'")
        set(binding, binding.parameter(name) ?: error("no parameter '$name' in '$label'"), value)
    }

    /**
     * Sets [parameter] of [binding] to [value]. For list parameters [value] selects an element: an [Int] is the index
     * of the element, any other value is the element. For mutable list parameters a [List] replaces the elements.
     */
    fun set(binding: Binding, parameter: Parameter, value: Any?) {
        @Suppress("UNCHECKED_CAST")
        val property = parameter.property as? KMutableProperty1<Any, Any?> ?: error("'${parameter.name}' has no value")
        when (parameter.parameterType) {
            ParameterType.List, ParameterType.MutableList -> {
                val list = property.get(binding.obj)
                val selectable = list as? Selectable ?: error("'${parameter.name}' does not hold a Selectable")
                when (value) {
                    is Int -> selectable.selectedIndex = value
                    is List<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        (list as? MutableList<Any?> ?: error("'${parameter.name}' is not mutable")).apply {
                            clear()
                            addAll(value)
                        }
                    }
                    else -> selectable.selectedIndex = (list as List<*>).indexOf(value)
                }
            }
            else -> property.set(binding.obj, value)
        }
        changed.trigger(Change(binding, parameter, binding.get(parameter)))
    }

    /** calls the action with [name] of the object with [label] */
    fun invoke(label: String, name: String) {
        val binding = binding(label) ?: error("no object with label '$label'")
        val parameter = binding.parameter(name) ?: error("no parameter '$name' in '$label'")
        parameter.function?.call(binding.obj) ?: error("'$name' is not an action")
        changed.trigger(Change(binding, parameter, null))
    }

    /**
     * Moves the values of the numeric, vector and color parameters towards random values in their ranges by
     * [strength], 0 keeps the values, 1 sets random values
     * @param filter the parameters that are randomized
     */
    fun randomize(
        strength: Double = 0.05,
        random: Random = Random.Default,
        filter: (Binding, Parameter) -> Boolean = { _, _ -> true }
    ) {
        rebindNested()
        fun uniform(min: Double, max: Double) = if (max > min) random.nextDouble(min, max) else min
        for (binding in all()) {
            for (parameter in binding.valueParameters) {
                if (!filter(binding, parameter)) continue
                val current = binding.get(parameter)
                val value: Any = when (parameter.parameterType) {
                    ParameterType.Double -> {
                        val range = parameter.doubleRange!!
                        mix(current as Double, uniform(range.start, range.endInclusive), strength)
                    }
                    ParameterType.Int -> {
                        val range = parameter.intRange!!
                        mix((current as Int).toDouble(), uniform(range.first.toDouble(), range.last.toDouble()), strength).roundToInt()
                    }
                    ParameterType.Color -> {
                        val color = current as ColorRGBa
                        color.mix(ColorRGBa(random.nextDouble(), random.nextDouble(), random.nextDouble(), color.alpha, color.linearity), strength)
                    }
                    ParameterType.Vector2 -> {
                        val range = parameter.doubleRange!!
                        (current as Vector2).mix(Vector2(uniform(range.start, range.endInclusive), uniform(range.start, range.endInclusive)), strength)
                    }
                    ParameterType.XY -> {
                        val (min, max) = parameter.vectorRange!!
                        (current as Vector2).mix(Vector2(uniform(min.x, max.x), uniform(min.y, max.y)), strength)
                    }
                    ParameterType.Vector3 -> {
                        val range = parameter.doubleRange!!
                        fun u() = uniform(range.start, range.endInclusive)
                        (current as Vector3).mix(Vector3(u(), u(), u()), strength)
                    }
                    ParameterType.Vector4 -> {
                        val range = parameter.doubleRange!!
                        fun u() = uniform(range.start, range.endInclusive)
                        (current as Vector4).mix(Vector4(u(), u(), u(), u()), strength)
                    }
                    else -> continue
                }
                set(binding, parameter, value)
            }
        }
    }

    /**
     * The values of the parameters of all objects, by the label of the object and the name of the parameter, to be
     * saved; complements [fromObject]. This allows the values to be saved together with other data.
     */
    fun toObject(): Map<String, Map<String, ParameterValue>> {
        rebindNested()
        return all().associate { binding ->
            binding.label to binding.valueParameters.associate { parameter -> parameter.name to valueOf(binding, parameter) }
        }
    }

    /** the [ParameterValue] of [parameter] of [binding] */
    private fun valueOf(binding: Binding, parameter: Parameter): ParameterValue {
        val value = binding.get(parameter)
        @Suppress("UNCHECKED_CAST")
        return when (parameter.parameterType) {
            ParameterType.Double -> ParameterValue(
                doubleValue = value as Double,
                minValue = parameter.doubleRange?.start,
                maxValue = parameter.doubleRange?.endInclusive
            )
            ParameterType.Int -> ParameterValue(
                intValue = value as Int,
                minValue = parameter.intRange?.first?.toDouble(),
                maxValue = parameter.intRange?.last?.toDouble()
            )
            ParameterType.Action, ParameterType.Nested -> ParameterValue()
            ParameterType.Color -> ParameterValue(colorValue = value as ColorRGBa)
            ParameterType.Text, ParameterType.Path -> ParameterValue(textValue = value as String)
            ParameterType.Boolean -> ParameterValue(booleanValue = value as Boolean)
            ParameterType.XY -> ParameterValue(vector2Value = value as Vector2)
            ParameterType.DoubleList -> ParameterValue(
                doubleListValue = value as MutableList<Double>,
                minValue = parameter.doubleRange?.start,
                maxValue = parameter.doubleRange?.endInclusive
            )
            ParameterType.Vector2 -> ParameterValue(
                vector2Value = value as Vector2,
                minValue = parameter.doubleRange?.start,
                maxValue = parameter.doubleRange?.endInclusive
            )
            ParameterType.Vector3 -> ParameterValue(
                vector3Value = value as Vector3,
                minValue = parameter.doubleRange?.start,
                maxValue = parameter.doubleRange?.endInclusive
            )
            ParameterType.Vector4 -> ParameterValue(
                vector4Value = value as Vector4,
                minValue = parameter.doubleRange?.start,
                maxValue = parameter.doubleRange?.endInclusive
            )
            ParameterType.Option -> ParameterValue(optionValue = (value as Enum<*>).name)
            ParameterType.List -> {
                val list = value as List<*>
                ParameterValue(selectedIndex = (value as Selectable).selectedIndex, listElementType = elementType(list))
            }
            ParameterType.MutableList -> {
                val list = value as List<*>
                val elementType = elementType(list)
                val elements = list.map { if (it is String || it is Int || it is Double) it else it.toString() }
                ParameterValue(
                    selectedIndex = (value as Selectable).selectedIndex,
                    listElementType = elementType,
                    stringListValue = if (elementType == "String") elements.map { it.toString() }.toMutableList() else null,
                    intListValue = if (elementType == "Int") elements.filterIsInstance<Int>().toMutableList() else null,
                    doubleListValue = if (elementType == "Double") elements.filterIsInstance<Double>().toMutableList() else null
                )
            }
        }
    }

    private fun elementType(list: List<*>) = when (list.firstOrNull()) {
        is Int -> "Int"
        is Double -> "Double"
        else -> "String"
    }

    /**
     * Sets the values of the parameters of the objects to [labeledValues], by the label of the object and the name of
     * the parameter; values of objects or parameters that are not bound are ignored. Complements [toObject].
     */
    fun fromObject(labeledValues: Map<String, Map<String, ParameterValue>>) {
        rebindNested()
        for ((label, values) in labeledValues) {
            val binding = binding(label) ?: continue
            for ((name, value) in values) {
                val parameter = binding.parameter(name) ?: continue
                try {
                    apply(binding, parameter, value)
                } catch (e: Exception) {
                    logger.warn { "could not set '$name' of '$label': ${e.message}" }
                }
            }
        }
        loaded.trigger(this)
    }

    /** sets [parameter] of [binding] to [value] */
    private fun apply(binding: Binding, parameter: Parameter, value: ParameterValue) {
        val v: Any = when (parameter.parameterType) {
            ParameterType.Double -> value.doubleValue
            ParameterType.Int -> value.intValue
            ParameterType.Text, ParameterType.Path -> value.textValue
            ParameterType.Color -> value.colorValue
            ParameterType.XY, ParameterType.Vector2 -> value.vector2Value
            ParameterType.DoubleList -> value.doubleListValue
            ParameterType.Boolean -> value.booleanValue
            ParameterType.Vector3 -> value.vector3Value
            ParameterType.Vector4 -> value.vector4Value
            ParameterType.Option -> value.optionValue?.let { name ->
                val current = binding.get(parameter) as Enum<*>
                current.declaringJavaClass.enumConstants.find { it.name == name } ?: error("cannot map value $name to enum")
            }
            ParameterType.List -> value.selectedIndex?.let { index ->
                index.coerceAtMost((binding.get(parameter) as List<*>).lastIndex)
            }
            ParameterType.MutableList -> {
                val elements = when (value.listElementType) {
                    "Int" -> value.intListValue
                    "Double" -> value.doubleListValue
                    else -> value.stringListValue
                }
                if (elements != null) set(binding, parameter, elements.toList())
                value.selectedIndex?.let { index -> index.coerceAtMost((binding.get(parameter) as List<*>).lastIndex) }
            }
            ParameterType.Action, ParameterType.Nested -> null
        } ?: return
        set(binding, parameter, v)
    }

    /** the values of all parameters as JSON, see [toObject] */
    fun toJson(): String = Gson().toJson(toObject())

    /** sets the values of the parameters to the values in [json], see [fromObject] */
    fun fromJson(json: String) {
        val type = object : TypeToken<Map<String, Map<String, ParameterValue>>>() {}.type
        fromObject(Gson().fromJson(json, type))
    }

    /** saves the values of all parameters to [file] as JSON */
    fun save(file: File) = file.writeText(toJson())

    /** loads the values of the parameters from the JSON in [file] */
    fun load(file: File) = fromJson(file.readText())
}
