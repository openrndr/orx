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

open class PropertyValue(val inherit: Boolean = false, val important: Boolean = false)

sealed class Color(inherit: Boolean = false,  important: Boolean = false) : PropertyValue(inherit, important) {
    class RGBa(val color: ColorRGBa, important: Boolean = false) : Color(important = important) {
        override fun toString(): String {
            return "RGBa(color=$color)"
        }
    }

    object Inherit : Color(inherit = true)

    companion object {
        val inherit = Inherit

        operator fun invoke(important: Boolean = false, f: Companion.() -> Any): Color {
            return when (val r = f()) {
                is ConvertibleToColorRGBa -> RGBa(r.toRGBa(), important)
                is Color -> r
                else -> error("Can't resolve Color from '$r'")
            }
        }
    }
}
typealias color = Color


class CalculateContext(val elementWidth: Double?, val elementHeight: Double?)

sealed class Cursor(inherit: Boolean = false) : PropertyValue(inherit) {
    object Default : Cursor()
    object Pointer : Cursor()
    object Text : Cursor()
    object Crosshair : Cursor()
    object Inherit : Cursor(inherit = true)
    companion object {
        val inherit = Inherit
        val default = Default
        val pointer = Pointer
        val text = Text
        val crosshair = Crosshair

        operator fun invoke(f: Companion.() -> Any): Cursor {
            return when (val r = f()) {
                is Cursor -> r
                else -> error("Can't resolve Cursor from '$r'")
            }
        }
    }
}

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

sealed class Display : PropertyValue(false) {
    object INLINE : Display()
    object BLOCK : Display()
    object FLEX : Display()
    object GRID : Display()
    object NONE : Display()
}

enum class Position {
    STATIC,
    ABSOLUTE,
    RELATIVE,
    FIXED,
    INHERIT,

    /**
     * In the flow, but kept in view while the element that scrolls it scrolls: at [top] from its top and [left] from its
     * left, along the axes it scrolls, within its parent; it is drawn over the elements next to it
     */
    STICKY
}

sealed class FlexDirection(inherit: Boolean = false) : PropertyValue(inherit) {
    object Row : FlexDirection()
    object Column : FlexDirection()
    object RowReverse : FlexDirection()
    object ColumnReverse : FlexDirection()
    object Inherit : FlexDirection(inherit = true)
}

/**
 * How the items of a flex container are placed across its main axis, inside their line.
 */
sealed class AlignItems(inherit: Boolean = false) : PropertyValue(inherit) {
    object Start : AlignItems()
    object Center : AlignItems()
    object End : AlignItems()

    /** items with an auto cross size fill their line */
    object Stretch : AlignItems()
    object Inherit : AlignItems(inherit = true)
}

/**
 * How the space left in a line of a flex container is distributed along its main axis.
 */
sealed class JustifyContent(inherit: Boolean = false) : PropertyValue(inherit) {
    object Start : JustifyContent()
    object Center : JustifyContent()
    object End : JustifyContent()

    /** the space is put between the items */
    object SpaceBetween : JustifyContent()

    /** every item gets an equal amount of space on both of its sides */
    object SpaceAround : JustifyContent()

    /** the space between the items and the edges is equal */
    object SpaceEvenly : JustifyContent()
    object Inherit : JustifyContent(inherit = true)
}

/**
 * Whether the items of a flex container wrap onto new lines when they do not fit.
 */
sealed class FlexWrap(inherit: Boolean = false) : PropertyValue(inherit) {
    object NoWrap : FlexWrap()
    object Wrap : FlexWrap()
    object Inherit : FlexWrap(inherit = true)
}

sealed class Overflow(inherit: Boolean = false) : PropertyValue(inherit) {
    object Visible : Overflow()
    object Hidden : Overflow()
    object Scroll : Overflow()
    object Inherit : Overflow(inherit = true)
}

/** how an element scrolls: [Smooth] animates scrolling by the wheel, by pages and into view, [Auto] jumps */
sealed class ScrollBehavior(inherit: Boolean = false) : PropertyValue(inherit) {
    object Auto : ScrollBehavior()
    object Smooth : ScrollBehavior()
    object Inherit : ScrollBehavior(inherit = true)
}

/** when scrollbars are shown: [Auto] while scrolling, and fading out after it, or [Always] */
sealed class ScrollbarVisibility(inherit: Boolean = false) : PropertyValue(inherit) {
    object Auto : ScrollbarVisibility()
    object Always : ScrollbarVisibility()
    object Inherit : ScrollbarVisibility(inherit = true)
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
        operator fun invoke(f: Companion.() -> Any): FlexGrow {
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

    /** counts the changes to the properties, so that changes made in place are noticed */
    var version = 0
        private set

    fun setProperty(name: String, value: Any?) {
        version++
        properties[name] = Property(name, value)
    }

    fun cascadeOnto(onto: StyleSheet): StyleSheet {
        val cascaded = StyleSheet(dummySelector)

        cascaded.properties.putAll(onto.properties)
        cascaded.properties.putAll(properties)

        // re-apply properties with important flag set
        val important = onto.properties.filterValues { (it.value as? PropertyValue)?.important == true }
        cascaded.properties.putAll(important)
        return cascaded
    }

    override fun toString(): String {
        return "StyleSheet(properties=$properties)"
    }
}

var StyleSheet.width by PropertyHandler<LinearDimension>("width", RESET, LinearDimension.Auto)
var StyleSheet.height by PropertyHandler<LinearDimension>("height", RESET, LinearDimension.Auto)

// a minimum or maximum of auto means no limit
var StyleSheet.minWidth by PropertyHandler<LinearDimension>("min-width", RESET, LinearDimension.Auto)
var StyleSheet.maxWidth by PropertyHandler<LinearDimension>("max-width", RESET, LinearDimension.Auto)
var StyleSheet.minHeight by PropertyHandler<LinearDimension>("min-height", RESET, LinearDimension.Auto)
var StyleSheet.maxHeight by PropertyHandler<LinearDimension>("max-height", RESET, LinearDimension.Auto)
var StyleSheet.top by PropertyHandler<LinearDimension>("top", RESET, length { 0 }) // css default is auto
var StyleSheet.left by PropertyHandler<LinearDimension>("left", RESET, length { 0 }) // css default is auto

var StyleSheet.marginTop by PropertyHandler<LinearDimension>("margin-top", RESET, length { 0 })
var StyleSheet.marginBottom by PropertyHandler<LinearDimension>("margin-bottom", RESET, length { 0 })
var StyleSheet.marginLeft by PropertyHandler<LinearDimension>("margin-left", RESET, length { 0 })
var StyleSheet.marginRight by PropertyHandler<LinearDimension>("margin-right", RESET, length { 0 })


var StyleSheet.paddingTop by PropertyHandler<LinearDimension>("padding-top", RESET, 0.px)
var StyleSheet.paddingBottom by PropertyHandler<LinearDimension>("padding-bottom", RESET, 0.px)
var StyleSheet.paddingLeft by PropertyHandler<LinearDimension>("padding-left", RESET, 0.px)
var StyleSheet.paddingRight by PropertyHandler<LinearDimension>("padding-right", RESET, 0.px)


var StyleSheet.position by PropertyHandler("position", RESET, Position.STATIC)
var StyleSheet.display by PropertyHandler<Display>("display", RESET, Display.BLOCK) // css default is inline

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
var StyleSheet.flexWrap by PropertyHandler<FlexWrap>("flex-wrap", RESET, FlexWrap.NoWrap)
var StyleSheet.alignItems by PropertyHandler<AlignItems>("align-items", RESET, AlignItems.Start) // css default is stretch
var StyleSheet.justifyContent by PropertyHandler<JustifyContent>("justify-content", RESET, JustifyContent.Start)

var StyleSheet.borderWidth by PropertyHandler<LinearDimension>("border-width", RESET, 0.px)
var StyleSheet.borderColor by PropertyHandler<Color>("border-color", INHERIT, Color.RGBa(ColorRGBa.TRANSPARENT))

/** the opacity of the element and its descendants, from 0 for invisible to 1 for opaque */
var StyleSheet.opacity by PropertyHandler<Double>("opacity", RESET, 1.0)

/**
 * The time in seconds it takes the colors and the opacity of the element to change to new values, for example when
 * it is hovered; 0 changes them at once
 */
var StyleSheet.transitionDuration by PropertyHandler<Double>("transition-duration", RESET, 0.0)

/** the radius of the corners of the box, in pixels or as a percentage of its shorter side */
var StyleSheet.borderRadius by PropertyHandler<LinearDimension>("border-radius", RESET, 0.px)

/** the width of the outline drawn around the box, outside its border, for example to show the keyboard focus */
var StyleSheet.outlineWidth by PropertyHandler<LinearDimension>("outline-width", RESET, 0.px)

/** the color of the outline */
var StyleSheet.outlineColor by PropertyHandler<Color>("outline-color", RESET, Color.RGBa(ColorRGBa.fromHex("#5ab0ff")))

/** the space between the box and its outline */
var StyleSheet.outlineOffset by PropertyHandler<LinearDimension>("outline-offset", RESET, 1.px)

var StyleSheet.background by PropertyHandler<Color>("background-color", RESET, Color.RGBa(ColorRGBa.BLACK.opacify(0.0)))
val StyleSheet.effectiveBackground: ColorRGBa?
    get() = (background as? Color.RGBa)?.color

/** the thickness of the scrollbars of elements that scroll, in pixels; 0 hides them */
var StyleSheet.scrollbarWidth by PropertyHandler<LinearDimension>("scrollbar-width", INHERIT, 6.px)

/** the color of the thumbs of scrollbars */
var StyleSheet.scrollbarColor by PropertyHandler<Color>("scrollbar-color", INHERIT, Color.RGBa(ColorRGBa.WHITE.opacify(0.35)))

/**
 * When scrollbars are shown: [ScrollbarVisibility.Auto] shows them while the element scrolls, while they are hovered
 * and dragged, and fades them out after that; [ScrollbarVisibility.Always] always shows them
 */
var StyleSheet.scrollbarVisibility by PropertyHandler<ScrollbarVisibility>("scrollbar-visibility", INHERIT, ScrollbarVisibility.Auto)

/** how the element scrolls its content, smoothly by default */
var StyleSheet.scrollBehavior by PropertyHandler<ScrollBehavior>("scroll-behavior", RESET, ScrollBehavior.Smooth)

/** the cursor over scrollbars, and while a scrollbar is dragged */
var StyleSheet.scrollbarCursor by PropertyHandler<Cursor>("scrollbar-cursor", INHERIT, Cursor.Pointer)

/** the color of the tracks of scrollbars */
var StyleSheet.scrollbarTrackColor by PropertyHandler<Color>("scrollbar-track-color", INHERIT, Color.RGBa(ColorRGBa.TRANSPARENT))

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

/** how the content overflows horizontally, as [overflow] when it is not set */
var StyleSheet.overflowX by PropertyHandler<Overflow>("overflow-x", RESET, Overflow.Visible)

/** how the content overflows vertically, as [overflow] when it is not set */
var StyleSheet.overflowY by PropertyHandler<Overflow>("overflow-y", RESET, Overflow.Visible)

/** the horizontal overflow: [overflowX], or [overflow] when it is not set */
val StyleSheet.effectiveOverflowX: Overflow
    get() = getProperty("overflow-x")?.value as? Overflow ?: overflow

/** the vertical overflow: [overflowY], or [overflow] when it is not set */
val StyleSheet.effectiveOverflowY: Overflow
    get() = getProperty("overflow-y")?.value as? Overflow ?: overflow

/** true when the content scrolls horizontally: by the program for [Overflow.Hidden], also by the user for [Overflow.Scroll] */
val StyleSheet.scrollableX: Boolean get() = effectiveOverflowX != Overflow.Visible

/** true when the content scrolls vertically, see [scrollableX] */
val StyleSheet.scrollableY: Boolean get() = effectiveOverflowY != Overflow.Visible

/**
 * True when the element clips its content, when it scrolls along one of its axes; along an axis that is visible the
 * content is clipped too, but does not scroll
 */
val StyleSheet.clipsContent: Boolean get() = scrollableX || scrollableY
var StyleSheet.zIndex by PropertyHandler<ZIndex>("z-index", RESET, ZIndex.Auto)

var StyleSheet.textVerticalAlign by PropertyHandler<TextAlign>(
    "text-vertical-align", RESET,
    TextAlign.Value(0.0)
)

var StyleSheet.textHorizontalAlign by PropertyHandler<TextAlign>(
    "text-horizontal-align", RESET,
    TextAlign.Value(0.0)
)


val Number.px: LinearDimension.PX get() = LinearDimension.PX(this.toDouble())
val Number.percent: LinearDimension.Percent get() = LinearDimension.Percent(this.toDouble())

var StyleSheet.cursor by PropertyHandler<Cursor>("cursor", RESET, Cursor.Default)
fun StyleSheet.margins(all: LinearDimension) {
    marginTop = all
    marginBottom = all
    marginLeft = all
    marginRight = all
}

fun StyleSheet.padding(all: LinearDimension) {
    paddingTop = all
    paddingBottom = all
    paddingLeft = all
    paddingRight = all
}

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

/** true when [a] and [b] are the same value of a property, also when they are different objects */
internal fun sameValue(a: Any?, b: Any?): Boolean {
    if (a === b || a == b) return true
    if (a == null || b == null || a::class != b::class) return false
    return when (a) {
        is Color.RGBa -> a.color == (b as Color.RGBa).color && a.important == b.important
        is LinearDimension.PX -> a.value == (b as LinearDimension.PX).value
        is LinearDimension.Percent -> a.value == (b as LinearDimension.Percent).value
        is LinearDimension.Fraction -> a.value == (b as LinearDimension.Fraction).value
        is LinearDimension.MinMax -> sameValue(a.min, (b as LinearDimension.MinMax).min) && sameValue(a.max, b.max)
        is ZIndex.Value -> a.value == (b as ZIndex.Value).value
        is FlexGrow.Ratio -> a.value == (b as FlexGrow.Ratio).value
        is TextAlign.Value -> a.value == (b as TextAlign.Value).value
        is GridPopulation.Position -> a.value == (b as GridPopulation.Position).value
        is GridPopulation.Length -> a.value == (b as GridPopulation.Length).value
        is GridPopulation.Span -> a.value == (b as GridPopulation.Span).value
        is GridTemplate.LengthList -> (b as GridTemplate.LengthList).value.size == a.value.size &&
                a.value.indices.all { sameValue(a.value[it], b.value[it]) }
        // values that are not known are taken as changed
        else -> false
    }
}

/** the properties that only change how an element is painted, not its size or position */
internal val paintProperties = setOf(
    "background-color", "color", "border-color", "border-radius", "outline-width", "outline-color", "outline-offset",
    "opacity", "cursor", "transition-duration", "scrollbar-color", "scrollbar-track-color", "scrollbar-cursor",
    "scrollbar-visibility", "scroll-behavior"
)

/**
 * How [new] differs from [old], styles of the same element: 0 when it does not, 1 when only properties that paint
 * changed, 2 when properties that lay out changed
 */
internal fun styleDifference(old: StyleSheet, new: StyleSheet): Int {
    var result = 0
    for (name in old.properties.keys + new.properties.keys) {
        if (!sameValue(old.getProperty(name)?.value, new.getProperty(name)?.value)) {
            if (name !in paintProperties) return 2
            result = 1
        }
    }
    return result
}
