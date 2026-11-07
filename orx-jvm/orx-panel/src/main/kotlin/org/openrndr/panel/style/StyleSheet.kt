package org.openrndr.panel.style

import org.openrndr.color.ColorRGBa
import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.color.ConvertibleToColorRGBa
import org.openrndr.extra.style.ComplexSelector
import org.openrndr.extra.style.CssDeclaration
import org.openrndr.extra.style.CssKeyword
import org.openrndr.extra.style.Specificity
import org.openrndr.extra.style.parseCssKeyframes
import org.openrndr.extra.style.parseCssRules
import org.openrndr.extra.style.parseCssSelectorList
import org.openrndr.extra.style.CssProperties
import org.openrndr.extra.style.CssProperty
import org.openrndr.extra.style.parseCssDeclarations
import org.openrndr.extra.style.splitCssWords
import org.openrndr.extra.style.TimingFunction
import org.openrndr.panel.style.PropertyInheritance.INHERIT
import org.openrndr.panel.style.PropertyInheritance.RESET
import java.util.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.reflect.KProperty

private val logger = KotlinLogging.logger {}

enum class PropertyInheritance {
    INHERIT,
    RESET
}

/**
 * A declaration of a style sheet: the property [name] with [value], a value of the property or a [CssKeyword] like
 * `inherit`; [important] when it is declared !important, it then wins over declarations that are not
 */
@JvmRecord
data class Property(
    val name: String,
    val value: Any?,
    val important: Boolean = false
)

/** a value of a property */
open class PropertyValue

/**
 * The value of [declaration], which uses var(): it is substituted with the custom properties of the element and parsed
 * when its style is computed. For a shorthand, like `margin: var(--space) 4px`, the value of its [longhand].
 */
class VariableValue(val declaration: CssDeclaration, val longhand: String = declaration.name.trim().lowercase()) {
    override fun toString() = "VariableValue(${declaration.name}: ${declaration.value})"
}

sealed class Color : PropertyValue() {
    class RGBa(val color: ColorRGBa) : Color() {
        override fun toString(): String {
            return "RGBa(color=$color)"
        }
    }

    companion object {
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

sealed class Cursor : PropertyValue() {
    object Default : Cursor()
    object Pointer : Cursor()
    object Text : Cursor()
    object Crosshair : Cursor()
    companion object {
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

sealed class LinearDimension : PropertyValue() {
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

    fun inPixels(parentLengthInPx: Double): Double {
        return when (this) {
            is PX -> value
            is Percent -> parentLengthInPx * (value / 100.0)
            Auto -> error("unresolvable")
            else -> TODO()
        }
    }

    companion object {
        val x = 0.0
        val auto by lazy { Auto }

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

/**
 * The properties of panels, by their CSS names, with their CSS shorthands, see [StyleSheet.css]
 */
object PanelProperties : CssProperties()

/**
 * The properties of panels, all registered: the properties are registered by the delegates of this file, which are
 * initialized when a function of the file is first called
 */
internal fun panelProperties(): CssProperties {
    // the animation properties are registered by the delegates of their own file
    ensureAnimationProperties()
    return PanelProperties
}

/**
 * A property of panels: [name] is its CSS name, its value is inherited by default when [inheritance] is
 * [PropertyInheritance.INHERIT], [initial] is its initial value, [parse] reads a value from CSS text
 */
class PropertyHandler<T>(
    val name: String, val inheritance: PropertyInheritance, val initial: T, parse: (String) -> T?
) {
    init {
        PanelProperties.register(CssProperty(name, inheritance == PropertyInheritance.INHERIT, initial, parse))
    }

    @Suppress("UNCHECKED_CAST")
    operator fun getValue(stylesheet: StyleSheet, property: KProperty<*>): T {
        val value = stylesheet.getProperty(name)?.value
        // keywords like inherit and values with var() are resolved when styles are computed
        return if (value == null || value is CssKeyword || value is VariableValue) initial else value as T
    }

    operator fun setValue(stylesheet: StyleSheet, property: KProperty<*>, value: T?) {
        stylesheet.setProperty(name, value)
    }
}

sealed class Display : PropertyValue() {
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

    /**
     * In the flow, but kept in view while the element that scrolls it scrolls: at [top] from its top and [left] from its
     * left, along the axes it scrolls, within its parent; it is drawn over the elements next to it
     */
    STICKY
}

sealed class FlexDirection : PropertyValue() {
    object Row : FlexDirection()
    object Column : FlexDirection()
    object RowReverse : FlexDirection()
    object ColumnReverse : FlexDirection()
}

/**
 * How the items of a flex container are placed across its main axis, inside their line.
 */
sealed class AlignItems : PropertyValue() {
    object Start : AlignItems()
    object Center : AlignItems()
    object End : AlignItems()

    /** items with an auto cross size fill their line */
    object Stretch : AlignItems()
}

/**
 * How the space left in a line of a flex container is distributed along its main axis.
 */
sealed class JustifyContent : PropertyValue() {
    object Start : JustifyContent()
    object Center : JustifyContent()
    object End : JustifyContent()

    /** the space is put between the items */
    object SpaceBetween : JustifyContent()

    /** every item gets an equal amount of space on both of its sides */
    object SpaceAround : JustifyContent()

    /** the space between the items and the edges is equal */
    object SpaceEvenly : JustifyContent()
}

/**
 * Whether the items of a flex container wrap onto new lines when they do not fit.
 */
sealed class FlexWrap : PropertyValue() {
    object NoWrap : FlexWrap()
    object Wrap : FlexWrap()
}

sealed class Overflow : PropertyValue() {
    object Visible : Overflow()
    object Hidden : Overflow()
    object Scroll : Overflow()
}

/** how an element scrolls: [Smooth] animates scrolling by the wheel, by pages and into view, [Auto] jumps */
sealed class ScrollBehavior : PropertyValue() {
    object Auto : ScrollBehavior()
    object Smooth : ScrollBehavior()
}

/** when scrollbars are shown: [Auto] while scrolling, and fading out after it, or [Always] */
sealed class ScrollbarVisibility : PropertyValue() {
    object Auto : ScrollbarVisibility()
    object Always : ScrollbarVisibility()
}

sealed class ZIndex : PropertyValue() {
    object Auto : ZIndex()
    class Value(val value: Int) : ZIndex()
}

sealed class FlexGrow : PropertyValue() {
    class Ratio(val value: Double) : FlexGrow()

    companion object {
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

sealed class GridTemplate : PropertyValue() {
    class LengthList(val value: List<LinearDimension>) : GridTemplate()
    object None : GridTemplate()

    companion object {
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

sealed class GridPopulation : PropertyValue() {
    class Position(val value: Int) : GridPopulation()
    class Length(val value: Int) : GridPopulation()
    class Span(val value: IntRange) : GridPopulation()
    object Auto : GridPopulation()

    companion object {
        val auto = Auto
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

sealed class TextAlign : PropertyValue() {
    class Value(val value: Double) : TextAlign()

    companion object {
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

    /**
     * The selector of CSS of the sheet, made when the sheet is first matched: [child] and [descendant] relate the
     * selector to the selector of their parent sheet after the sheet is made
     */
    val cssSelector: ComplexSelector by lazy { selector.toCss() }

    /** the specificity of the selector */
    val specificity: Specificity get() = cssSelector.specificity

    fun getProperty(name: String) = properties.get(name)

    /** counts the changes to the properties, so that changes made in place are noticed */
    var version = 0
        private set

    /** true while the properties that are set are important, see [important] */
    private var settingImportant = false

    /** sets the property [name] to [value], a value of the property or a [CssKeyword] */
    fun setProperty(name: String, value: Any?, important: Boolean = settingImportant) {
        version++
        properties[name] = Property(name, value, important)
    }

    /** the properties that [block] sets are !important: they win over the declarations that are not */
    fun important(block: StyleSheet.() -> Unit) {
        val before = settingImportant
        settingImportant = true
        try {
            block()
        } finally {
            settingImportant = before
        }
    }

    /** the properties [names] take the values of the parent, like the CSS keyword inherit */
    fun inherit(vararg names: String) = names.forEach { setProperty(it, CssKeyword.INHERIT) }

    /** the properties [names] take their initial values, like the CSS keyword initial */
    fun initial(vararg names: String) = names.forEach { setProperty(it, CssKeyword.INITIAL) }

    /**
     * Sets the properties of the CSS [declarations], like `width: 100px; background-color: #333 !important`, with
     * shorthands like margin, padding, border and transition, and custom properties like `--accent: #f80` that values
     * use with var(), like `color: var(--accent)`. Declarations of properties that are not known and values that are
     * not valid are left out, as in CSS.
     */
    fun css(declarations: String) {
        for (declaration in parseCssDeclarations(declarations)) {
            val important = declaration.important || settingImportant
            // custom properties are kept as text, values with var() are resolved when styles are computed
            if (declaration.name.startsWith("--")) {
                setProperty(declaration.name, declaration.value, important)
                continue
            }
            if ("var(" in declaration.value) {
                // a shorthand sets its longhands, which are substituted and parsed with the shorthand
                val name = declaration.name.trim().lowercase()
                for (longhand in panelProperties().longhands(name) ?: listOf(name)) {
                    setProperty(longhand, VariableValue(declaration, longhand), important)
                }
                continue
            }
            val values = panelProperties().values(declaration)
            if (values == null) {
                logger.warn { "ignored the declaration '${declaration.name}: ${declaration.value}'" }
                continue
            }
            for ((name, value) in values) setProperty(name, value, declaration.important || settingImportant)
        }
    }

    /** sets the property [name] to the CSS [value], see [css] */
    operator fun set(name: String, value: String) = css("$name: $value")

    /** the value of the property [name] as it is set, null when it is not set */
    operator fun get(name: String): Any? = properties[name]?.value

    fun cascadeOnto(onto: StyleSheet): StyleSheet {
        val cascaded = StyleSheet(dummySelector)

        cascaded.properties.putAll(onto.properties)
        // declarations that are important are not replaced by declarations that are not
        for ((name, property) in properties) {
            if (property.important || cascaded.properties[name]?.important != true) cascaded.properties[name] = property
        }
        return cascaded
    }

    override fun toString(): String {
        return "StyleSheet(properties=$properties)"
    }
}

var StyleSheet.width by PropertyHandler<LinearDimension>("width", RESET, LinearDimension.Auto, ::parseLength)
var StyleSheet.height by PropertyHandler<LinearDimension>("height", RESET, LinearDimension.Auto, ::parseLength)

// a minimum or maximum of auto means no limit
var StyleSheet.minWidth by PropertyHandler<LinearDimension>("min-width", RESET, LinearDimension.Auto, ::parseLength)
var StyleSheet.maxWidth by PropertyHandler<LinearDimension>("max-width", RESET, LinearDimension.Auto, ::parseLength)
var StyleSheet.minHeight by PropertyHandler<LinearDimension>("min-height", RESET, LinearDimension.Auto, ::parseLength)
var StyleSheet.maxHeight by PropertyHandler<LinearDimension>("max-height", RESET, LinearDimension.Auto, ::parseLength)
var StyleSheet.top by PropertyHandler<LinearDimension>("top", RESET, length { 0 }, ::parseLength) // css default is auto
var StyleSheet.left by PropertyHandler<LinearDimension>("left", RESET, length { 0 }, ::parseLength) // css default is auto

var StyleSheet.marginTop by PropertyHandler<LinearDimension>("margin-top", RESET, length { 0 }, ::parseLength)
var StyleSheet.marginBottom by PropertyHandler<LinearDimension>("margin-bottom", RESET, length { 0 }, ::parseLength)
var StyleSheet.marginLeft by PropertyHandler<LinearDimension>("margin-left", RESET, length { 0 }, ::parseLength)
var StyleSheet.marginRight by PropertyHandler<LinearDimension>("margin-right", RESET, length { 0 }, ::parseLength)


var StyleSheet.paddingTop by PropertyHandler<LinearDimension>("padding-top", RESET, 0.px, ::parseLength)
var StyleSheet.paddingBottom by PropertyHandler<LinearDimension>("padding-bottom", RESET, 0.px, ::parseLength)
var StyleSheet.paddingLeft by PropertyHandler<LinearDimension>("padding-left", RESET, 0.px, ::parseLength)
var StyleSheet.paddingRight by PropertyHandler<LinearDimension>("padding-right", RESET, 0.px, ::parseLength)


var StyleSheet.position by PropertyHandler("position", RESET, Position.STATIC, keywords("static" to Position.STATIC, "relative" to Position.RELATIVE, "absolute" to Position.ABSOLUTE, "fixed" to Position.FIXED, "sticky" to Position.STICKY))
var StyleSheet.display by PropertyHandler<Display>("display", RESET, Display.BLOCK, keywords("inline" to Display.INLINE, "block" to Display.BLOCK, "flex" to Display.FLEX, "grid" to Display.GRID, "none" to Display.NONE)) // css default is inline

var StyleSheet.columnGap by PropertyHandler<LinearDimension>("column-gap", PropertyInheritance.RESET, LinearDimension.PX(0.0), ::parseLength)
var StyleSheet.rowGap by PropertyHandler<LinearDimension>("row-gap", PropertyInheritance.RESET, LinearDimension.PX(0.0), ::parseLength)

var StyleSheet.gridTemplateColumns by PropertyHandler<GridTemplate>("grid-template-columns", RESET, GridTemplate.None, ::parseGridTemplate)
var StyleSheet.gridTemplateRows by PropertyHandler<GridTemplate>("grid-template-rows", RESET, GridTemplate.None, ::parseGridTemplate)
var StyleSheet.gridColumn by PropertyHandler<GridPopulation>("grid-column", RESET, GridPopulation.Auto, ::parseGridPopulation)
var StyleSheet.gridRow by PropertyHandler<GridPopulation>("grid-row", RESET, GridPopulation.Auto, ::parseGridPopulation)

var StyleSheet.flexDirection by PropertyHandler<FlexDirection>("flex-direction", RESET, FlexDirection.Row, keywords("row" to FlexDirection.Row, "column" to FlexDirection.Column, "row-reverse" to FlexDirection.RowReverse, "column-reverse" to FlexDirection.ColumnReverse))
var StyleSheet.flexGrow by PropertyHandler<FlexGrow>("flex-grow", RESET, FlexGrow.Ratio(0.0), ::parseFlex)
var StyleSheet.flexShrink by PropertyHandler<FlexGrow>("flex-shrink", RESET, FlexGrow.Ratio(1.0), ::parseFlex)
var StyleSheet.flexWrap by PropertyHandler<FlexWrap>("flex-wrap", RESET, FlexWrap.NoWrap, keywords("nowrap" to FlexWrap.NoWrap, "wrap" to FlexWrap.Wrap))
var StyleSheet.alignItems by PropertyHandler<AlignItems>("align-items", RESET, AlignItems.Start, keywords("start" to AlignItems.Start, "flex-start" to AlignItems.Start, "center" to AlignItems.Center, "end" to AlignItems.End, "flex-end" to AlignItems.End, "stretch" to AlignItems.Stretch)) // css default is stretch
var StyleSheet.justifyContent by PropertyHandler<JustifyContent>("justify-content", RESET, JustifyContent.Start, keywords("start" to JustifyContent.Start, "flex-start" to JustifyContent.Start, "center" to JustifyContent.Center, "end" to JustifyContent.End, "flex-end" to JustifyContent.End, "space-between" to JustifyContent.SpaceBetween, "space-around" to JustifyContent.SpaceAround, "space-evenly" to JustifyContent.SpaceEvenly))

var StyleSheet.borderWidth by PropertyHandler<LinearDimension>("border-width", RESET, 0.px, ::parseLength)
var StyleSheet.borderColor by PropertyHandler<Color>("border-color", INHERIT, Color.RGBa(ColorRGBa.TRANSPARENT), ::parseColor)

/** the opacity of the element and its descendants, from 0 for invisible to 1 for opaque */
var StyleSheet.opacity by PropertyHandler<Double>("opacity", RESET, 1.0, { parseNumber(it, percentage = true)?.coerceIn(0.0, 1.0) })

/**
 * The time in seconds it takes the colors and the opacity of the element to change to new values, for example when
 * it is hovered; 0 changes them at once
 */
var StyleSheet.transitionDuration by PropertyHandler<Double>("transition-duration", RESET, 0.0, ::parseTime)

/** the time in seconds before the colors and the opacity of the element start to change, see [transitionDuration] */
var StyleSheet.transitionDelay by PropertyHandler<Double>("transition-delay", RESET, 0.0, ::parseTime)

/** how the colors and the opacity of the element change over time, see [transitionDuration] */
var StyleSheet.transitionTimingFunction by PropertyHandler<TimingFunction>("transition-timing-function", RESET, TimingFunction.EASE, ::parseTimingFunction)

/**
 * The properties that transition, see [transitionDuration]: of background-color, color, border-color, outline-color
 * and opacity, `all` for all of them
 */
var StyleSheet.transitionProperty by PropertyHandler<List<String>>("transition-property", RESET, listOf("all"), ::parseNames)

/** the radius of the corners of the box, in pixels or as a percentage of its shorter side */
var StyleSheet.borderRadius by PropertyHandler<LinearDimension>("border-radius", RESET, 0.px, ::parseLength)

/** the width of the outline drawn around the box, outside its border, for example to show the keyboard focus */
var StyleSheet.outlineWidth by PropertyHandler<LinearDimension>("outline-width", RESET, 0.px, ::parseLength)

/** the color of the outline */
var StyleSheet.outlineColor by PropertyHandler<Color>("outline-color", RESET, Color.RGBa(ColorRGBa.fromHex("#5ab0ff")), ::parseColor)

/** the space between the box and its outline */
var StyleSheet.outlineOffset by PropertyHandler<LinearDimension>("outline-offset", RESET, 1.px, ::parseLength)

/** the image of the background, a gradient, drawn under the element; `background: linear-gradient(…)` sets it */
var StyleSheet.backgroundImage by PropertyHandler<BackgroundImage>("background-image", RESET, BackgroundImage.None, ::parseBackgroundImage)

/**
 * The transform of the element and its descendants, like `scale(1.05) rotate(2deg)`, around [transformOrigin]; it does
 * not change the layout
 */
var StyleSheet.transform by PropertyHandler<Transform>("transform", RESET, Transform.NONE, ::parseTransform)

/** the point that the element is transformed around, in fractions of its size, the center by default */
var StyleSheet.transformOrigin by PropertyHandler<org.openrndr.math.Vector2>("transform-origin", RESET, org.openrndr.math.Vector2(0.5, 0.5), ::parseTransformOrigin)

/** the shadows of the box, drawn under the element, outside its box */
var StyleSheet.boxShadow by PropertyHandler<BoxShadow>("box-shadow", RESET, BoxShadow.NONE, ::parseBoxShadow)

var StyleSheet.background by PropertyHandler<Color>("background-color", RESET, Color.RGBa(ColorRGBa.BLACK.opacify(0.0)), ::parseColor)
val StyleSheet.effectiveBackground: ColorRGBa?
    get() = (background as? Color.RGBa)?.color

/** the thickness of the scrollbars of elements that scroll, in pixels; 0 hides them */
var StyleSheet.scrollbarWidth by PropertyHandler<LinearDimension>("scrollbar-width", INHERIT, 6.px, ::parseLength)

/** the color of the thumbs of scrollbars */
var StyleSheet.scrollbarColor by PropertyHandler<Color>("scrollbar-color", INHERIT, Color.RGBa(ColorRGBa.WHITE.opacify(0.35)), ::parseColor)

/**
 * When scrollbars are shown: [ScrollbarVisibility.Auto] shows them while the element scrolls, while they are hovered
 * and dragged, and fades them out after that; [ScrollbarVisibility.Always] always shows them
 */
var StyleSheet.scrollbarVisibility by PropertyHandler<ScrollbarVisibility>("scrollbar-visibility", INHERIT, ScrollbarVisibility.Auto, keywords("auto" to ScrollbarVisibility.Auto, "always" to ScrollbarVisibility.Always))

/** how the element scrolls its content, smoothly by default */
var StyleSheet.scrollBehavior by PropertyHandler<ScrollBehavior>("scroll-behavior", RESET, ScrollBehavior.Smooth, keywords("auto" to ScrollBehavior.Auto, "smooth" to ScrollBehavior.Smooth))

/** the cursor over scrollbars, and while a scrollbar is dragged */
var StyleSheet.scrollbarCursor by PropertyHandler<Cursor>("scrollbar-cursor", INHERIT, Cursor.Pointer, parseCursor)

/** the color of the tracks of scrollbars */
var StyleSheet.scrollbarTrackColor by PropertyHandler<Color>("scrollbar-track-color", INHERIT, Color.RGBa(ColorRGBa.TRANSPARENT), ::parseColor)

var StyleSheet.color by PropertyHandler<Color>("color", INHERIT, Color.RGBa(ColorRGBa.WHITE), ::parseColor)
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


var StyleSheet.fontSize by PropertyHandler<LinearDimension>("font-size", INHERIT, 14.px, ::parseLength)
/**
 * The font families, separated by commas, the first that is registered is used: in the font manager of the control
 * manager, then in [org.openrndr.extra.textwriter.Fonts], the fonts of all programs
 */
var StyleSheet.fontFamily by PropertyHandler("font-family", INHERIT, "default", { it.trim().takeIf { v -> v.isNotEmpty() } })

/** the weight of the font, from 100 to 900, 400 is regular and 700 bold */
var StyleSheet.fontWeight by PropertyHandler("font-weight", INHERIT, 400, ::parseFontWeight)

/** the style of the font: upright or italic */
enum class FontStyle { NORMAL, ITALIC }

var StyleSheet.fontStyle by PropertyHandler("font-style", INHERIT, FontStyle.NORMAL, keywords("normal" to FontStyle.NORMAL, "italic" to FontStyle.ITALIC, "oblique" to FontStyle.ITALIC))
var StyleSheet.overflow by PropertyHandler<Overflow>("overflow", RESET, Overflow.Visible, parseOverflow)

/** how the content overflows horizontally, as [overflow] when it is not set */
var StyleSheet.overflowX by PropertyHandler<Overflow>("overflow-x", RESET, Overflow.Visible, parseOverflow)

/** how the content overflows vertically, as [overflow] when it is not set */
var StyleSheet.overflowY by PropertyHandler<Overflow>("overflow-y", RESET, Overflow.Visible, parseOverflow)

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
var StyleSheet.zIndex by PropertyHandler<ZIndex>("z-index", RESET, ZIndex.Auto, ::parseZIndex)

var StyleSheet.textVerticalAlign by PropertyHandler<TextAlign>("text-vertical-align", RESET, TextAlign.Value(0.0), ::parseTextAlign)

var StyleSheet.textHorizontalAlign by PropertyHandler<TextAlign>("text-horizontal-align", RESET, TextAlign.Value(0.0), ::parseTextAlign)


// the shorthands of CSS and the CSS names of properties of panels, registered with the properties
private val shorthands = PanelProperties.apply {
    val sides = listOf("top", "right", "bottom", "left")
    shorthand("margin", sides.map { "margin-$it" }, boxShorthand("margin"))
    shorthand("padding", sides.map { "padding-$it" }, boxShorthand("padding"))
    shorthand("gap", listOf("row-gap", "column-gap")) { value ->
        val words = splitCssWords(value)
        when (words.size) {
            1 -> listOf("row-gap" to words[0], "column-gap" to words[0])
            2 -> listOf("row-gap" to words[0], "column-gap" to words[1])
            else -> null
        }
    }
    shorthand("border", listOf("border-width", "border-color"), lineShorthand("border"))
    shorthand("outline", listOf("outline-width", "outline-color"), lineShorthand("outline"))
    // a color, a gradient, or both; the color that is not given is transparent
    shorthand("background", listOf("background-color", "background-image")) { value ->
        val parts = splitCssWords(value)
        val image = parts.filter { it.contains("gradient(") }
        val color = parts.filter { !it.contains("gradient(") }
        if (image.size > 1 || color.size > 1 || (color.isNotEmpty() && parseColor(color[0]) == null)) null
        else listOfNotNull(color.firstOrNull()?.let { "background-color" to it } ?: ("background-color" to "transparent"),
            image.firstOrNull()?.let { "background-image" to it })
    }
    shorthand("text-align", listOf("text-horizontal-align")) { listOf("text-horizontal-align" to it) }
    shorthand("vertical-align", listOf("text-vertical-align")) { listOf("text-vertical-align" to it) }
    // flex: none, auto, or a grow factor and a shrink factor; a basis is not supported
    shorthand("flex", listOf("flex-grow", "flex-shrink")) { value ->
        val words = splitCssWords(value)
        when {
            value.trim() == "none" -> listOf("flex-grow" to "0", "flex-shrink" to "0")
            value.trim() == "auto" -> listOf("flex-grow" to "1", "flex-shrink" to "1")
            words.size in 1..2 && words.all { parseNumber(it) != null } ->
                listOf("flex-grow" to words[0], "flex-shrink" to (words.getOrNull(1) ?: "1"))
            else -> null
        }
    }
    // the properties of all parts of a transition, with the timing of the first: the timing of panels is the same
    // for all properties
    shorthand("transition", listOf("transition-property", "transition-duration", "transition-delay", "transition-timing-function")) { value ->
        val parts = org.openrndr.extra.style.splitCssList(value).map { splitCssWords(it.trim()) }
        if (parts.isEmpty() || parts.any { it.isEmpty() }) return@shorthand null
        val names = parts.map { words -> words.firstOrNull { parseTime(it) == null && parseTimingFunction(it) == null } ?: "all" }
        val first = parts.first()
        val times = first.filter { parseTime(it) != null }
        val timing = first.firstOrNull { parseTimingFunction(it) != null } ?: "ease"
        listOf(
            "transition-property" to names.joinToString(", "),
            "transition-duration" to (times.getOrNull(0) ?: "0s"),
            "transition-delay" to (times.getOrNull(1) ?: "0s"),
            "transition-timing-function" to timing
        )
    }
}

val Number.px: LinearDimension.PX get() = LinearDimension.PX(this.toDouble())
val Number.percent: LinearDimension.Percent get() = LinearDimension.Percent(this.toDouble())

var StyleSheet.cursor by PropertyHandler<Cursor>("cursor", RESET, Cursor.Default, parseCursor)
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

/**
 * A style sheet for the CSS selector [selector], like `.toolbar > button:hover`, with the properties that [init] sets
 */
fun styleSheet(selector: String, init: StyleSheet.() -> Unit): StyleSheet =
    styleSheet(CompoundSelector.parse(selector) ?: error("invalid or unsupported selector '$selector'"), init)

/**
 * The style sheets of the CSS [css]: a sheet for every selector of every rule, with the declarations of the rule, see
 * [StyleSheet.css]. Rules with selectors that are not valid or not supported are left out, as in CSS; @keyframes rules
 * are added to [PanelKeyframes], other at-rules are left out.
 */
fun styleSheets(css: String): List<StyleSheet> = run {
    // the @keyframes rules of the CSS are added to those of panels
    for ((name, frames) in parseCssKeyframes(css)) PanelKeyframes[name] = frames
    parseCssRules(css)
}.flatMap { rule ->
    val selectors = parseCssSelectorList(rule.selectors.joinToString(", "))
    if (selectors == null) {
        logger.warn { "ignored the rule for '${rule.selectors.joinToString(", ")}', its selector is not valid or not supported" }
        emptyList()
    } else selectors.map { selector -> StyleSheet(CompoundSelector.of(selector)).apply { css(rule.block) } }
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
        is Color.RGBa -> a.color == (b as Color.RGBa).color
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
    "opacity", "cursor", "background-image", "box-shadow", "transform", "transform-origin", "transition-duration", "transition-delay", "transition-timing-function", "transition-property",
    "scrollbar-color", "scrollbar-track-color", "scrollbar-cursor",
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
