package org.openrndr.panel.style

import org.openrndr.color.ColorRGBa
import org.openrndr.color.ConvertibleToColorRGBa
import org.openrndr.panel.style.PropertyInheritance.INHERIT
import org.openrndr.panel.style.PropertyInheritance.RESET
import java.util.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.reflect.KProperty

enum class PropertyInheritance {
    INHERIT,
    RESET
}

@JvmRecord
data class Property(
    val name: String,
    val value: Any?
)

open class PropertyValue(val inherit: Boolean = false)

sealed class Color(inherit: Boolean = false) : PropertyValue(inherit) {
    class RGBa(val color: ColorRGBa) : Color() {
        override fun toString(): String {
            return "RGBa(color=$color)"
        }
    }

    object Inherit : Color(inherit = true)

    companion object {
        val inherit = Inherit

        operator fun invoke(f: Companion.() -> Any): Color {
            return when (val r = f()) {
                is ConvertibleToColorRGBa -> RGBa(r.toRGBa())
                is Color -> r
                else -> error("Can't resolve Color from '$r'")
            }
        }
    }
}
typealias color = Color


class CalculateContext(val elementWidth: Double?, val elementHeight: Double?)

sealed class LinearDimension(inherit: Boolean = false) : PropertyValue(inherit) {
    class PX(val value: Double) : LinearDimension() {
        override fun toString(): String {
            return "PX(value=$value)"
        }
    }

    class Fraction(val value: Double) : LinearDimension()

    class Percent(val value: Double) : LinearDimension()
    class Calculate(val function: (CalculateContext) -> Double) : LinearDimension()

    class MinMax(val min: LinearDimension, val max: LinearDimension) : LinearDimension()

    object Auto : LinearDimension()
    object Inherit : LinearDimension(inherit = true)

    fun inPixels(parentLengthInPx: Double): Double {
        return when (this) {
            is PX -> value
            is Percent -> parentLengthInPx * (value / 100.0)
            Auto, Inherit -> error("unresolvable")
            else -> TODO()
        }
    }

    companion object {
        val x = 0.0
        val auto by lazy { Auto }
        val inherit by lazy { Inherit }

        val Number.px: PX
            get() {
                return PX(this.toDouble())
            }

        val Number.fr: Fraction
            get() {
                return Fraction(this.toDouble())
            }

        fun minmax(min: LinearDimension, max: LinearDimension) = MinMax(min, max)


        operator fun invoke(f: Companion.() -> Any): LinearDimension {
            return when (val r = f()) {
                is Number -> PX(r.toDouble())
                is LinearDimension -> r
                else -> error("Can't resolve LinearDimension from '$r'")
            }
        }
    }
}

typealias length = LinearDimension.Companion


@JvmRecord
data class PropertyBehaviour(val inheritance: PropertyInheritance, val intitial: Any)

object PropertyBehaviours {

    val behaviours = HashMap<String, PropertyBehaviour>()
}

class PropertyHandler<T>(
    val name: String, val inheritance: PropertyInheritance, val initial: T
) {

    init {
        PropertyBehaviours.behaviours[name] = PropertyBehaviour(inheritance, initial as Any)
    }

    @Suppress("USELESS_CAST", "UNCHECKED_CAST")
    operator fun getValue(stylesheet: StyleSheet, property: KProperty<*>): T {
        val value: T? = stylesheet.getProperty(name)?.value as T?
        return value ?: PropertyBehaviours.behaviours[name]!!.intitial as T

    }

    operator fun setValue(stylesheet: StyleSheet, property: KProperty<*>, value: T?) {
        stylesheet.setProperty(name, value)
    }
}

enum class Display {
    INLINE,
    BLOCK,
    FLEX,
    GRID,
    NONE
}

enum class Position {
    STATIC,
    ABSOLUTE,
    RELATIVE,
    FIXED,
    INHERIT
}

sealed class FlexDirection(inherit: Boolean = false) : PropertyValue(inherit) {
    object Row : FlexDirection()
    object Column : FlexDirection()
    object RowReverse : FlexDirection()
    object ColumnReverse : FlexDirection()
    object Inherit : FlexDirection(inherit = true)
}

sealed class Overflow(inherit: Boolean = false) : PropertyValue(inherit) {
    object Visible : Overflow()
    object Hidden : Overflow()
    object Scroll : Overflow()
    object Inherit : Overflow(inherit = true)
}

sealed class ZIndex(inherit: Boolean = false) : PropertyValue(inherit) {
    object Auto : ZIndex()
    class Value(val value: Int) : ZIndex()
    object Inherit : ZIndex(inherit = true)
}

sealed class FlexGrow(inherit: Boolean = false) : PropertyValue(inherit) {
    class Ratio(val value: Double) : FlexGrow()
    object Inherit : FlexGrow(inherit = true)

    companion object {
        val inherit = Inherit
        operator fun invoke(f: FlexGrow.Companion.() -> Any): FlexGrow {
            return when (val r = f()) {
                is Number -> Ratio(r.toDouble())
                is FlexGrow -> r
                else -> error("Can't resolve FlexGrow from '$r'")
            }
        }
    }
}
typealias flex = FlexGrow

sealed class GridTemplate(inherit: Boolean = false) : PropertyValue(inherit) {
    class LengthList(val value: List<LinearDimension>) : GridTemplate()
    object Inherit : GridTemplate(inherit = true)
    object None : GridTemplate()

    companion object {
        val inherit = Inherit
        val none = None
        operator fun invoke(f: Companion.() -> Any): GridTemplate {
            return when (val r = f()) {
                is LinearDimension -> LengthList(listOf(r))
                is List<*> -> LengthList(r.map { it as LinearDimension })
                is GridTemplate -> r
                else -> error("Can't resolve GridTemplate from '$r'")
            }
        }
    }
}

typealias gridTemplate = GridTemplate

sealed class GridPopulation(inherit: Boolean = false) : PropertyValue(inherit) {
    class Position(val value: Int) : GridPopulation()
    class Length(val value: Int) : GridPopulation()
    class Span(val value: IntRange) : GridPopulation()
    object Auto : GridPopulation()
    object Inherit : GridPopulation(inherit = true)

    companion object {
        val auto = Auto
        val inherit = Inherit
        fun length(value: Int) = Length(value)
        val Int.rows: Length get() = length(this)
        val Int.columns: Length get() = length(this)
        operator fun invoke(f: Companion.() -> Any): GridPopulation {
            return when (val r = f()) {
                is Int -> Position(r)
                is IntRange -> Span(r)
                is GridPopulation -> r
                else -> error("Can't resolve GridPosition from '$r'")
            }
        }
    }
}

typealias gridPopulation = GridPopulation

sealed class TextAlign(inherit: Boolean = false) : PropertyValue(inherit) {
    class Value(val value: Double) : TextAlign()
    object Inherit : TextAlign(inherit = false)

    companion object {
        val inherit = Inherit
        val left = Value(0.0)
        val right = Value(1.0)
        val center = Value(0.5)
        val top = Value(0.0)
        val bottom = Value(1.0)

        operator fun invoke(f: Companion.() -> Any): TextAlign {
            return when (val r = f()) {
                is Number -> Value(r.toDouble())
                is TextAlign -> r
                else -> error("Can't resolve TextAlign from '$r'")
            }
        }
    }
}
typealias textAlign = TextAlign


private val dummySelector = CompoundSelector()

class StyleSheet(val selector: CompoundSelector = CompoundSelector.DUMMY) {
    val children = mutableListOf<StyleSheet>()
    val properties = HashMap<String, Property>()

    val precedence by lazy {
        selector.precedence()
    }

    fun getProperty(name: String) = properties.get(name)

    fun setProperty(name: String, value: Any?) {
        properties[name] = Property(name, value)
    }

    fun cascadeOnto(onto: StyleSheet): StyleSheet {
        val cascaded = StyleSheet(dummySelector)

        cascaded.properties.putAll(onto.properties)
        cascaded.properties.putAll(properties)
        return cascaded
    }

    override fun toString(): String {
        return "StyleSheet(properties=$properties)"
    }
}

var StyleSheet.width by PropertyHandler<LinearDimension>("width", RESET, LinearDimension.Auto)
var StyleSheet.height by PropertyHandler<LinearDimension>("height", RESET, LinearDimension.Auto)
var StyleSheet.top by PropertyHandler<LinearDimension>("top", RESET, 0.px) // css default is auto
var StyleSheet.left by PropertyHandler<LinearDimension>("left", RESET, 0.px) // css default is auto

var StyleSheet.marginTop by PropertyHandler<LinearDimension>("margin-top", RESET, 0.px)
var StyleSheet.marginBottom by PropertyHandler<LinearDimension>("margin-bottom", RESET, 0.px)
var StyleSheet.marginLeft by PropertyHandler<LinearDimension>("margin-left", RESET, 0.px)
var StyleSheet.marginRight by PropertyHandler<LinearDimension>("margin-right", RESET, 0.px)


var StyleSheet.paddingTop by PropertyHandler<LinearDimension>("padding-top", RESET, 0.px)
var StyleSheet.paddingBottom by PropertyHandler<LinearDimension>("padding-bottom", RESET, 0.px)
var StyleSheet.paddingLeft by PropertyHandler<LinearDimension>("padding-left", RESET, 0.px)
var StyleSheet.paddingRight by PropertyHandler<LinearDimension>("padding-right", RESET, 0.px)


var StyleSheet.position by PropertyHandler("position", RESET, Position.STATIC)
var StyleSheet.display by PropertyHandler("display", RESET, Display.BLOCK) // css default is inline

var StyleSheet.columnGap by PropertyHandler<LinearDimension>(
    "column-gap",
    PropertyInheritance.RESET,
    LinearDimension.PX(0.0)
)
var StyleSheet.rowGap by PropertyHandler<LinearDimension>("row-gap", PropertyInheritance.RESET, LinearDimension.PX(0.0))

var StyleSheet.gridTemplateColumns by PropertyHandler<GridTemplate>("grid-template-columns", RESET, GridTemplate.None)
var StyleSheet.gridTemplateRows by PropertyHandler<GridTemplate>("grid-template-rows", RESET, GridTemplate.None)
var StyleSheet.gridColumn by PropertyHandler<GridPopulation>("grid-column", RESET, GridPopulation.Auto)
var StyleSheet.gridRow by PropertyHandler<GridPopulation>("grid-row", RESET, GridPopulation.Auto)

var StyleSheet.flexDirection by PropertyHandler<FlexDirection>("flex-direction", RESET, FlexDirection.Row)
var StyleSheet.flexGrow by PropertyHandler<FlexGrow>("flex-grow", RESET, FlexGrow.Ratio(0.0))
var StyleSheet.flexShrink by PropertyHandler<FlexGrow>("flex-shrink", RESET, FlexGrow.Ratio(1.0))

var StyleSheet.borderWidth by PropertyHandler<LinearDimension>("border-width", RESET, 0.px)
var StyleSheet.borderColor by PropertyHandler<Color>("border-color", INHERIT, Color.RGBa(ColorRGBa.TRANSPARENT))

var StyleSheet.background by PropertyHandler<Color>("background-color", RESET, Color.RGBa(ColorRGBa.BLACK.opacify(0.0)))
val StyleSheet.effectiveBackground: ColorRGBa?
    get() = (background as? Color.RGBa)?.color

var StyleSheet.color by PropertyHandler<Color>("color", INHERIT, Color.RGBa(ColorRGBa.WHITE))
val StyleSheet.effectiveColor: ColorRGBa?
    get() = (color as? Color.RGBa)?.color


val StyleSheet.effectivePaddingLeft: Double
    get() = (paddingLeft as? LinearDimension.PX)?.value ?: 0.0

val StyleSheet.effectivePaddingRight: Double
    get() = (paddingRight as? LinearDimension.PX)?.value ?: 0.0

val StyleSheet.effectivePaddingTop: Double
    get() = (paddingTop as? LinearDimension.PX)?.value ?: 0.0

val StyleSheet.effectivePaddingBottom: Double
    get() = (paddingBottom as? LinearDimension.PX)?.value ?: 0.0


val StyleSheet.effectivePaddingHeight: Double
    get() = effectivePaddingBottom + effectivePaddingTop

val StyleSheet.effectivePaddingWidth: Double
    get() = effectivePaddingLeft + effectivePaddingRight


val StyleSheet.effectiveBorderWidth: Double
    get() = (borderWidth as? LinearDimension.PX)?.value ?: 0.0

val StyleSheet.effectiveBorderColor: ColorRGBa?
    get() = (borderColor as? Color.RGBa)?.color


val StyleSheet.computedTextVerticalAlign: Double
    get() = (textVerticalAlign as? TextAlign.Value)?.value ?: 0.0

val StyleSheet.computedTextHorizontalAlign: Double
    get() = (textHorizontalAlign as? TextAlign.Value)?.value ?: 0.0


var StyleSheet.fontSize by PropertyHandler<LinearDimension>("font-size", INHERIT, 14.px)
var StyleSheet.fontFamily by PropertyHandler("font-family", INHERIT, "default")
var StyleSheet.overflow by PropertyHandler<Overflow>("overflow", RESET, Overflow.Visible)
var StyleSheet.zIndex by PropertyHandler<ZIndex>("z-index", RESET, ZIndex.Auto)

var StyleSheet.textVerticalAlign by PropertyHandler<TextAlign>(
    "text-vertical-align", PropertyInheritance.RESET,
    TextAlign.Value(0.0)
)

var StyleSheet.textHorizontalAlign by PropertyHandler<TextAlign>(
    "text-horizontal-align", PropertyInheritance.RESET,
    TextAlign.Value(0.0)
)


val Number.px: LinearDimension.PX get() = LinearDimension.PX(this.toDouble())
val Number.percent: LinearDimension.Percent get() = LinearDimension.Percent(this.toDouble())


fun StyleSheet.child(selector: CompoundSelector, init: StyleSheet.() -> Unit) {
    val stylesheet = StyleSheet(selector).apply(init)
    stylesheet.selector.previous = Pair(Combinator.CHILD, this.selector)
    children.add(stylesheet)
}

fun StyleSheet.descendant(selector: CompoundSelector, init: StyleSheet.() -> Unit) {
    val stylesheet = StyleSheet(selector).apply(init)
    stylesheet.selector.previous = Pair(Combinator.DESCENDANT, this.selector)
    children.add(stylesheet)
}

fun StyleSheet.and(selector: CompoundSelector, init: StyleSheet.() -> Unit) {
    val stylesheet = StyleSheet(this.selector and selector).apply(init)
    this.children.add(stylesheet)
}

fun StyleSheet.flatten(): List<StyleSheet> {
    return listOf(this) + children.flatMap { it.flatten() }
}

@OptIn(ExperimentalContracts::class)
fun styleSheet(selector: CompoundSelector = CompoundSelector.DUMMY, init: StyleSheet.() -> Unit): StyleSheet {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return StyleSheet(selector).apply {
        init()
    }
}
