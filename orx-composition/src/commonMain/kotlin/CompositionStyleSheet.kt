@file:Suppress("RemoveExplicitTypeArguments")

package org.openrndr.extra.composition
import org.openrndr.color.*
import org.openrndr.draw.*
import org.openrndr.extra.shadestyles.presets.ColorStop
import org.openrndr.extra.shadestyles.presets.GradientSpread
import org.openrndr.extra.shadestyles.presets.GradientInterpolation
import org.openrndr.extra.shadestyles.presets.LinearGradientPaint
import org.openrndr.extra.shadestyles.presets.PaintUnits
import org.openrndr.extra.shadestyles.presets.RadialGradientPaint
import org.openrndr.extra.shadestyles.presets.SourcePreset
import org.openrndr.extra.shadestyles.presets.TexturePaint
import org.openrndr.extra.composition.AttributeOrPropertyKey.*
import org.openrndr.extra.composition.Inheritance.*
import org.openrndr.math.*
import org.openrndr.math.transforms.buildTransform
import org.openrndr.shape.Rectangle
import kotlin.jvm.JvmRecord
import kotlin.reflect.*

enum class Inheritance {
    INHERIT,
    RESET
}

sealed interface AttributeOrPropertyValue {
    val value: Any?
    override fun toString(): String
}

sealed interface Paint : AttributeOrPropertyValue {
    override val value: ColorRGBa?

    class RGB(override val value: ColorRGBa) : Paint {
        override fun toString(): String {
            val hexs = listOf(value.r, value.g, value.b).map {
                (it.coerceIn(0.0, 1.0) * 255.0).toInt().toString(16).padStart(2, '0')
            }
            return hexs.joinToString(prefix = "#", separator = "")
        }
    }

    /** the value of the color property, like SVG's currentColor; it has no color of its own */
    object CurrentColor : Paint {
        override val value: ColorRGBa? = null

        override fun toString(): String = "currentcolor"
    }

    object None : Paint {
        override val value: ColorRGBa? = null
        override fun toString(): String = "none"
    }

    /**
     * A paint that is drawn with a shade style: its [source] read at coordinates in [units], transformed by the
     * inverse of [transform]. [value] is a single color that stands in for it where it can not be drawn.
     */
    sealed interface Server : Paint {
        val units: PaintUnits
        val transform: Matrix44

        /** the preset that generates the colors of this paint */
        fun source(): SourcePreset
    }

    /**
     * A gradient with color [stops], like the gradients of SVG. [id] is the id of the gradient in SVG documents.
     */
    sealed interface Gradient : Server {
        val stops: List<ColorStop>
        val spread: GradientSpread
        val id: String

        /** the color space the colors are interpolated in, sRGB like SVG by default */
        val interpolation: GradientInterpolation

        /** the id of the gradient in SVG documents: [id], or an id derived from the gradient when it has none */
        val svgId: String get() = id.ifEmpty { "gradient-" + hashCode().toUInt().toString(16) }

        override val value: ColorRGBa?
            get() = if (stops.isEmpty()) null else ColorRGBa(
                stops.sumOf { it.color.r } / stops.size,
                stops.sumOf { it.color.g } / stops.size,
                stops.sumOf { it.color.b } / stops.size,
                1.0,
                stops.first().color.linearity
            )
    }

    /**
     * A linear gradient from [start] to [end], like SVG's linearGradient
     */
    data class LinearGradient(
        val start: Vector2,
        val end: Vector2,
        override val stops: List<ColorStop>,
        override val units: PaintUnits = PaintUnits.BOUNDS,
        override val spread: GradientSpread = GradientSpread.PAD,
        override val transform: Matrix44 = Matrix44.IDENTITY,
        override val id: String = "",
        override val interpolation: GradientInterpolation = GradientInterpolation.SRGB
    ) : Gradient {
        override fun source(): SourcePreset =
            LinearGradientPaint(start, end, stops, spread).also { it.interpolation = interpolation }
        override fun toString(): String = "url(#$svgId)"
    }

    /**
     * A radial gradient from the circle at [focal] with [focalRadius] to the circle at [center] with [radius], like
     * SVG's radialGradient
     */
    data class RadialGradient(
        val center: Vector2,
        val radius: Double,
        val focal: Vector2 = center,
        val focalRadius: Double = 0.0,
        override val stops: List<ColorStop>,
        override val units: PaintUnits = PaintUnits.BOUNDS,
        override val spread: GradientSpread = GradientSpread.PAD,
        override val transform: Matrix44 = Matrix44.IDENTITY,
        override val id: String = "",
        override val interpolation: GradientInterpolation = GradientInterpolation.SRGB
    ) : Gradient {
        override fun source(): SourcePreset =
            RadialGradientPaint(center, radius, focal, focalRadius, stops, spread).also { it.interpolation = interpolation }
        override fun toString(): String = "url(#$svgId)"
    }

    /**
     * A pattern, like SVG's pattern: [content] drawn in a tile that is repeated in both directions.
     *
     * The tile is [tile] in [units]: the bounds of the painted shape for [PaintUnits.BOUNDS], so 0..1 covers the
     * shape, or its coordinates for [PaintUnits.OBJECT]. [transform] transforms the tiles, like patternTransform.
     * The content is drawn with its origin at the corner of the tile; [viewBox] and [aspectRatio] fit the content
     * into the tile when a view box is given, otherwise [contentUnits] decide whether its coordinates are in the
     * coordinates of the shape or relative to its bounds.
     *
     * The tile is drawn to a texture once for every size it is drawn at, changes to [content] after it is drawn are
     * not seen. [fallback] is the color that stands in for the pattern where it can not be drawn.
     */
    class Pattern(
        val content: GroupNode,
        val tile: Rectangle,
        override val units: PaintUnits = PaintUnits.BOUNDS,
        val contentUnits: PaintUnits = PaintUnits.OBJECT,
        val viewBox: Rectangle? = null,
        val aspectRatio: AspectRatio = AspectRatio.DEFAULT,
        override val transform: Matrix44 = Matrix44.IDENTITY,
        val id: String = "",
        val fallback: ColorRGBa? = null
    ) : Server {
        override val value: ColorRGBa? get() = fallback

        /** the id of the pattern in SVG documents: [id], or an id derived from the pattern when it has none */
        val svgId: String get() = id.ifEmpty { "pattern-" + hashCode().toUInt().toString(16) }

        // the tile textures are set on this paint before a shape is drawn
        private var texturePaint: TexturePaint? = null

        override fun source(): SourcePreset = texturePaint ?: error("the texture of the pattern is not prepared")

        /** the paint that reads the tile textures, created with [texture] */
        internal fun texturePaint(texture: ColorBuffer): TexturePaint =
            texturePaint?.also { it.texture = texture } ?: TexturePaint(texture).also { texturePaint = it }

        override fun toString(): String = "url(#$svgId)"
    }

    /**
     * A paint of any [preset] that generates colors, such as noise or checkers. It is not written to SVG documents,
     * [fallback] is written instead.
     */
    class Preset(
        val preset: SourcePreset,
        override val units: PaintUnits = PaintUnits.BOUNDS,
        override val transform: Matrix44 = Matrix44.IDENTITY,
        val fallback: ColorRGBa? = null
    ) : Server {
        override val value: ColorRGBa? get() = fallback
        override fun source(): SourcePreset = preset
        override fun toString(): String = fallback?.let { RGB(it).toString() } ?: "none"
    }
}

sealed interface Shade : AttributeOrPropertyValue {
    override val value: ShadeStyle

    class Value(override val value: ShadeStyle) : Shade {
        override fun toString(): String = ""
    }
}

sealed interface Length : AttributeOrPropertyValue {
    override val value: Double

    class Pixels(override val value: Double) : Length {
        companion object {
            fun fromInches(value: Double) = Pixels(value * 96.0)
            fun fromPicas(value: Double) = Pixels(value * 16.0)
            fun fromPoints(value: Double) = Pixels(value * (4.0 / 3.0))
            fun fromCentimeters(value: Double) = Pixels(value * (96.0 / 2.54))
            fun fromMillimeters(value: Double) = Pixels(value * (96.0 / 25.4))
            fun fromQuarterMillimeters(value: Double) = Pixels(value * (96.0 / 101.6))
        }

        override fun toString(): String = "$value"
    }

    class Percent(override val value: Double) : Length {
        override fun toString(): String {
            return "${value}%"
        }
    }

    enum class UnitIdentifier {
        IN,
        PC,
        PT,
        PX,
        CM,
        MM,
        Q
    }
}

inline val Double.pixels: Length.Pixels
    get() = Length.Pixels(this)
inline val Double.percent: Length.Percent
    get() = Length.Percent(this)

sealed interface Numeric : AttributeOrPropertyValue {
    override val value: Double

    class Rational(override val value: Double) : Numeric {
        override fun toString(): String = "$value"
    }
}

sealed interface Transform : AttributeOrPropertyValue {
    override val value: Matrix44

    class Matrix(override val value: Matrix44) : Transform {
        override fun toString(): String {
            return if (value == Matrix44.IDENTITY) {
                ""
            } else {
                "matrix(${value.c0r0} ${value.c0r1} " +
                    "${value.c1r0} ${value.c1r1} " +
                    "${value.c3r0} ${value.c3r1})"
            }
        }
    }

    object None : Transform {
        override val value = Matrix44.IDENTITY
        override fun toString(): String = ""
    }
}

sealed interface Visibility : AttributeOrPropertyValue {
    override val value: Boolean

    object Visible : Visibility {
        override val value = true
        override fun toString() = "visible"
    }

    object Hidden : Visibility {
        override val value = false
        override fun toString() = "hidden"
    }

    // This exists because the spec specifies so,
    // it is effectively Hidden.
    object Collapse : Visibility {
        override val value = false
        override fun toString() = "collapse"
    }
}

/**
 * Effects on the rendering of shapes, like the vector-effect property of SVG
 */
sealed interface VectorEffect : AttributeOrPropertyValue {
    override val value: Boolean

    object None : VectorEffect {
        override val value = false
        override fun toString() = "none"
    }

    /** the stroke weight is not scaled by the transforms of the shape and its groups */
    object NonScalingStroke : VectorEffect {
        override val value = true
        override fun toString() = "non-scaling-stroke"
    }
}

/**
 * The dash pattern of strokes, like the stroke-dasharray property of SVG
 */
sealed interface DashArray : AttributeOrPropertyValue {
    override val value: List<Double>

    /** solid strokes */
    object None : DashArray {
        override val value = emptyList<Double>()
        override fun toString() = "none"
    }

    /** strokes with alternating dashes and gaps of [value] */
    class Lengths(override val value: List<Double>) : DashArray {
        override fun toString() = value.joinToString(" ")
        override fun equals(other: Any?) = other is Lengths && other.value == value
        override fun hashCode() = value.hashCode()
    }
}

/**
 * The order in which the fill and the stroke of shapes and text are painted, like the paint-order property of SVG
 */
sealed interface PaintOrder : AttributeOrPropertyValue {
    override val value: org.openrndr.draw.PaintOrder

    /** the fill, then the stroke */
    object Normal : PaintOrder {
        override val value = org.openrndr.draw.PaintOrder.FILL_STROKE
        override fun toString() = "normal"
    }

    /** the stroke, then the fill, which covers the inner half of the stroke */
    object StrokeFirst : PaintOrder {
        override val value = org.openrndr.draw.PaintOrder.STROKE_FILL
        override fun toString() = "stroke"
    }
}

/**
 * The clip path of a node, like the clip-path property of SVG: the node is drawn where the shapes of the clip path
 * are, with anti-aliased edges
 */
sealed interface ClipPath : AttributeOrPropertyValue {
    object None : ClipPath {
        override val value: Any? = null
        override fun toString() = "none"
    }

    /**
     * The shapes in [content] clip, in [units]: the coordinates of the node, or 0..1 over its bounds. The shapes are
     * filled with the clip rule ([clipRule]) of their styles, their fills and strokes are ignored.
     */
    class Shapes(val content: GroupNode, val units: PaintUnits = PaintUnits.OBJECT, val id: String = "") : ClipPath {
        override val value: GroupNode get() = content

        /** the id of the clip path in SVG documents: [id], or an id derived from the clip path when it has none */
        val svgId: String get() = id.ifEmpty { "clip-" + hashCode().toUInt().toString(16) }

        override fun toString() = "url(#$svgId)"
    }
}

/**
 * How a [Marker] is turned at a vertex
 */
sealed interface MarkerOrient {
    /** along the direction of the path, the bisector of the directions in and out of the vertex */
    object Auto : MarkerOrient

    /** like [Auto], the marker at the start of the path is turned around */
    object AutoStartReverse : MarkerOrient

    /** by a fixed angle in [degrees] */
    class Angle(val degrees: Double) : MarkerOrient
}

/**
 * A marker, like SVG's marker: [content] drawn at the vertices of paths. The content is in the coordinates of
 * [viewBox], fitted into [size] by [aspectRatio], or in the coordinates of [size] when there is no view box; the
 * point [reference] of the content is placed on the vertex. The size is multiplied by the stroke weight when
 * [strokeWidthUnits] is true. Clipping the content to the viewport, as SVG does by default, is part of the content.
 */
class Marker(
    val content: GroupNode,
    val viewBox: Rectangle? = null,
    val aspectRatio: AspectRatio = AspectRatio.DEFAULT,
    val reference: Vector2 = Vector2.ZERO,
    val size: Vector2 = Vector2(3.0, 3.0),
    val strokeWidthUnits: Boolean = true,
    val orient: MarkerOrient = MarkerOrient.Angle(0.0),
    val id: String = ""
) {
    /** the id of the marker in SVG documents: [id], or an id derived from the marker when it has none */
    val svgId: String get() = id.ifEmpty { "marker-" + hashCode().toUInt().toString(16) }

    /** the transform from the coordinates of the content to the viewport, with the reference point at the origin */
    val contentTransform: Matrix44
        get() {
            val fit = viewBox?.let { viewportTransform(it, Rectangle(0.0, 0.0, size.x, size.y), aspectRatio) }
                ?: Matrix44.IDENTITY
            val reference = (fit * this.reference.xy01).xy
            return buildTransform { translate(-reference.x, -reference.y, 0.0) } * fit
        }
}

/**
 * The marker at vertices of paths, like the marker-start, marker-mid and marker-end properties of SVG
 */
sealed interface MarkerReference : AttributeOrPropertyValue {
    override val value: Marker?

    object None : MarkerReference {
        override val value: Marker? = null
        override fun toString() = "none"
    }

    class Value(override val value: Marker) : MarkerReference {
        override fun toString() = "url(#${value.svgId})"
    }
}

/** how the value of a [Mask] is taken from its content, like the mask-type property of SVG */
enum class MaskType {
    /** the luminance of the content, multiplied by its alpha */
    LUMINANCE,

    /** the alpha of the content */
    ALPHA
}

/**
 * A mask, like SVG's mask: the alpha of what it is applied to is multiplied by the value of [content], its luminance
 * or its alpha by [type]. The content is in the coordinates of the node it is applied to, or in the bounds of that
 * node when [units] is [PaintUnits.BOUNDS]; it is drawn in [region], in the coordinates of [regionUnits], or
 * everywhere when the region is null.
 */
class Mask(
    val content: GroupNode,
    val units: PaintUnits = PaintUnits.OBJECT,
    val region: Rectangle? = Rectangle(-0.1, -0.1, 1.2, 1.2),
    val regionUnits: PaintUnits = PaintUnits.BOUNDS,
    val type: MaskType = MaskType.LUMINANCE,
    val id: String = ""
) {
    /** the id of the mask in SVG documents: [id], or an id derived from the mask when it has none */
    val svgId: String get() = id.ifEmpty { "mask-" + hashCode().toUInt().toString(16) }
}

/**
 * The mask of a node, like the mask property of SVG
 */
sealed interface MaskReference : AttributeOrPropertyValue {
    override val value: Mask?

    object None : MaskReference {
        override val value: Mask? = null
        override fun toString() = "none"
    }

    class Value(override val value: Mask) : MaskReference {
        override fun toString() = "url(#${value.svgId})"
    }
}

/**
 * How a node is blended with what is drawn under it, like the mix-blend-mode property of CSS. The modes are those of
 * the W3C compositing and blending specification.
 */
enum class MixBlendMode(private val css: String) : AttributeOrPropertyValue {
    NORMAL("normal"),
    MULTIPLY("multiply"),
    SCREEN("screen"),
    OVERLAY("overlay"),
    DARKEN("darken"),
    LIGHTEN("lighten"),
    COLOR_DODGE("color-dodge"),
    COLOR_BURN("color-burn"),
    HARD_LIGHT("hard-light"),
    SOFT_LIGHT("soft-light"),
    DIFFERENCE("difference"),
    EXCLUSION("exclusion"),
    HUE("hue"),
    SATURATION("saturation"),
    COLOR("color"),
    LUMINOSITY("luminosity"),
    PLUS_LIGHTER("plus-lighter");

    override val value: MixBlendMode get() = this
    override fun toString(): String = css

    companion object {
        /** the mode named [name] in CSS, null for names that are not known */
        fun of(name: String): MixBlendMode? = entries.firstOrNull { it.css == name.trim() }
    }
}

/**
 * Whether a group is drawn on its own before it is blended with what is under it, like the isolation property of
 * CSS. Groups with an opacity, a mask or a blend mode are always isolated.
 */
enum class Isolation(private val css: String) : AttributeOrPropertyValue {
    AUTO("auto"),
    ISOLATE("isolate");

    override val value: Isolation get() = this
    override fun toString(): String = css
}

sealed interface Display : AttributeOrPropertyValue {
    override val value: Boolean

    object Inline : Display {
        override val value = true
        override fun toString() = "inline"
    }

    object Block : Display {
        override val value = true
        override fun toString() = "block"
    }

    object None : Display {
        override val value = false
        override fun toString() = "none"
    }
}

sealed interface LineCap : AttributeOrPropertyValue {
    override val value: org.openrndr.draw.LineCap

    object Round : LineCap {
        override val value = org.openrndr.draw.LineCap.ROUND
        override fun toString() = "round"
    }

    object Butt : LineCap {
        override val value = org.openrndr.draw.LineCap.BUTT
        override fun toString() = "butt"
    }

    object Square : LineCap {
        override val value = org.openrndr.draw.LineCap.SQUARE
        override fun toString() = "square"
    }
}

/**
 * The rule that decides which parts of a shape with overlapping or self-intersecting contours are filled, like the
 * fill-rule of SVG
 */
sealed interface FillRule : AttributeOrPropertyValue {
    override val value: org.openrndr.shape.FillRule

    /** filled where the contours wind around a point a number of times other than zero */
    object NonZero : FillRule {
        override val value = org.openrndr.shape.FillRule.NON_ZERO
        override fun toString() = "nonzero"
    }

    /** filled where an odd number of contours are around a point */
    object EvenOdd : FillRule {
        override val value = org.openrndr.shape.FillRule.EVEN_ODD
        override fun toString() = "evenodd"
    }
}

sealed interface LineJoin : AttributeOrPropertyValue {
    override val value: org.openrndr.draw.LineJoin

    object Miter : LineJoin {
        override val value = org.openrndr.draw.LineJoin.MITER
        override fun toString() = "miter"
    }

    object Bevel : LineJoin {
        override val value = org.openrndr.draw.LineJoin.BEVEL
        override fun toString() = "bevel"
    }

    object Round : LineJoin {
        override val value = org.openrndr.draw.LineJoin.ROUND
        override fun toString() = "round"
    }
}

enum class Align {
    NONE,
    X_MIN_Y_MIN,
    X_MID_Y_MIN,
    X_MAX_Y_MIN,
    X_MIN_Y_MID,
    X_MID_Y_MID,
    X_MAX_Y_MID,
    X_MIN_Y_MAX,
    X_MID_Y_MAX,
    X_MAX_Y_MAX
}

enum class MeetOrSlice {
    MEET,
    SLICE
}

@JvmRecord
data class AspectRatio(val align: Align, val meetOrSlice: MeetOrSlice) : AttributeOrPropertyValue {
    override val value: AspectRatio
        get() = this

    companion object {
        val DEFAULT = AspectRatio(Align.X_MID_Y_MID, MeetOrSlice.MEET)
    }

    override fun toString(): String {
        if (this == DEFAULT) {
            return ""
        }

        val a = when (align) {
            Align.NONE -> "none"
            Align.X_MIN_Y_MIN -> "xMinYMin"
            Align.X_MID_Y_MIN -> "xMidYMin"
            Align.X_MAX_Y_MIN -> "xMaxYMin"
            Align.X_MIN_Y_MID -> "xMinYMid"
            Align.X_MID_Y_MID -> "xMidYMid"
            Align.X_MAX_Y_MID -> "xMaxYMid"
            Align.X_MIN_Y_MAX -> "xMinYMax"
            Align.X_MID_Y_MAX -> "xMidYMax"
            Align.X_MAX_Y_MAX -> "xMaxYMax"
        }
        val m = when (meetOrSlice) {
            MeetOrSlice.MEET -> "meet"
            MeetOrSlice.SLICE -> "slice"
        }

        return "$a $m"
    }
}

/**
 * The font families of text in order of preference, like CSS font-family
 */
class FontFamily(override val value: List<String>) : AttributeOrPropertyValue {
    override fun toString(): String = value.joinToString(", ") { if (it.contains(' ')) "'$it'" else it }
}

/**
 * The weight of a font, from 100 (thin) to 900 (black), 400 is normal and 700 is bold
 */
class FontWeight(override val value: Int) : AttributeOrPropertyValue {
    override fun toString(): String = value.toString()
}

enum class FontStyle(override val value: Boolean) : AttributeOrPropertyValue {
    NORMAL(false),
    ITALIC(true),
    OBLIQUE(true);

    override fun toString(): String = name.lowercase()
}

/**
 * Where text is placed relative to its position: the fraction of its width that is left of the position
 */
enum class TextAnchor(override val value: Double) : AttributeOrPropertyValue {
    START(0.0),
    MIDDLE(0.5),
    END(1.0);

    override fun toString(): String = name.lowercase()
}

/**
 * The baseline of text that its position is on, like the dominant-baseline property of SVG. [AUTO] is the alphabetic
 * baseline.
 */
enum class DominantBaseline(private val css: String) : AttributeOrPropertyValue {
    AUTO("auto"),
    ALPHABETIC("alphabetic"),
    IDEOGRAPHIC("ideographic"),
    MIDDLE("middle"),
    CENTRAL("central"),
    MATHEMATICAL("mathematical"),
    HANGING("hanging"),
    TEXT_BEFORE_EDGE("text-before-edge"),
    TEXT_AFTER_EDGE("text-after-edge");

    override val value: DominantBaseline get() = this
    override fun toString(): String = css

    /**
     * The distance from this baseline down to the alphabetic baseline, for a face with [ascent], [descent] and
     * [xHeight], all positive
     */
    fun offset(ascent: Double, descent: Double, xHeight: Double): Double = when (this) {
        AUTO, ALPHABETIC -> 0.0
        IDEOGRAPHIC, TEXT_AFTER_EDGE -> -descent
        MIDDLE -> xHeight / 2.0
        CENTRAL -> (ascent - descent) / 2.0
        MATHEMATICAL -> ascent / 2.0
        HANGING -> ascent * 0.8
        TEXT_BEFORE_EDGE -> ascent
    }

    companion object {
        /** the baseline named [name] in SVG or CSS, null for names that are not known */
        fun of(name: String): DominantBaseline? = when (val n = name.trim()) {
            "text-top" -> TEXT_BEFORE_EDGE
            "text-bottom" -> TEXT_AFTER_EDGE
            "use-script", "no-change", "reset-size" -> AUTO
            else -> entries.firstOrNull { it.css == n }
        }
    }
}

/**
 * The direction that lines of text run in, like the writing-mode property: horizontally, or vertically from top to
 * bottom; vertical lines follow each other to the left ([VERTICAL_RL]) or to the right ([VERTICAL_LR])
 */
enum class WritingMode(private val css: String, val vertical: Boolean) : AttributeOrPropertyValue {
    HORIZONTAL_TB("horizontal-tb", false),
    VERTICAL_RL("vertical-rl", true),
    VERTICAL_LR("vertical-lr", true);

    override val value: WritingMode get() = this
    override fun toString(): String = css

    companion object {
        /** the mode named [name] in CSS, or in SVG 1.1 */
        fun of(name: String): WritingMode? = when (name.trim()) {
            "horizontal-tb", "lr", "lr-tb", "rl", "rl-tb" -> HORIZONTAL_TB
            "vertical-rl", "tb", "tb-rl", "sideways-rl" -> VERTICAL_RL
            "vertical-lr", "sideways-lr" -> VERTICAL_LR
            else -> null
        }
    }
}

/**
 * How the characters of vertical text are oriented, like the text-orientation property: [MIXED] sets the characters
 * of scripts that are written vertically, like CJK, upright and turns others sideways, [UPRIGHT] sets all upright,
 * [SIDEWAYS] turns all 90 degrees clockwise
 */
enum class TextOrientation(private val css: String) : AttributeOrPropertyValue {
    MIXED("mixed"),
    UPRIGHT("upright"),
    SIDEWAYS("sideways");

    override val value: TextOrientation get() = this
    override fun toString(): String = css

    companion object {
        fun of(name: String): TextOrientation? = when (name.trim()) {
            "sideways-right" -> SIDEWAYS
            else -> entries.firstOrNull { it.css == name.trim() }
        }
    }
}

/**
 * Which parts of a node are hit by the pointer, like the pointer-events property of SVG: the fill and the stroke,
 * when they are painted or always, and only when the node is visible or always. [NONE] is never hit.
 */
enum class PointerEvents(private val css: String, val fill: Boolean, val stroke: Boolean, val painted: Boolean, val visible: Boolean) :
    AttributeOrPropertyValue {
    VISIBLE_PAINTED("visiblePainted", true, true, true, true),
    VISIBLE_FILL("visibleFill", true, false, false, true),
    VISIBLE_STROKE("visibleStroke", false, true, false, true),
    VISIBLE("visible", true, true, false, true),
    PAINTED("painted", true, true, true, false),
    FILL("fill", true, false, false, false),
    STROKE("stroke", false, true, false, false),
    ALL("all", true, true, false, false),
    NONE("none", false, false, false, false);

    override val value: PointerEvents get() = this
    override fun toString(): String = css

    companion object {
        /** the value named [name] in SVG or CSS, auto is visiblePainted */
        fun of(name: String): PointerEvents? = if (name.trim() == "auto") VISIBLE_PAINTED else entries.firstOrNull { it.css == name.trim() }
    }
}

/**
 * How the white space of text is handled, like the white-space property of CSS. Text in compositions is already
 * collapsed or not, the value is kept so that it is written with the text; line feeds are line breaks of text that
 * wraps, see [inlineSize].
 */
enum class WhiteSpace(private val css: String, val preservesSpaces: Boolean) : AttributeOrPropertyValue {
    NORMAL("normal", false),
    NOWRAP("nowrap", false),
    PRE("pre", true),
    PRE_WRAP("pre-wrap", true),
    PRE_LINE("pre-line", false),
    BREAK_SPACES("break-spaces", true);

    override val value: WhiteSpace get() = this
    override fun toString(): String = css

    companion object {
        fun of(name: String): WhiteSpace? = entries.firstOrNull { it.css == name.trim() }
    }
}

/** how text is adjusted to its [textLength], like the lengthAdjust attribute of SVG */
enum class LengthAdjust(private val svg: String) : AttributeOrPropertyValue {
    /** the space between the characters is adjusted */
    SPACING("spacing"),

    /** the characters are stretched or squeezed along with the space between them */
    SPACING_AND_GLYPHS("spacingAndGlyphs");

    override val value: LengthAdjust get() = this
    override fun toString(): String = svg
}

/** an optional length: a [Length], or none, like the textLength attribute and the inline-size property */
class OptionalLength(override val value: Length?) : AttributeOrPropertyValue {
    override fun toString(): String = when (val v = value) {
        null -> ""
        is Length.Pixels -> v.value.toString()
        is Length.Percent -> "${v.value}%"
    }

    companion object {
        val NONE = OptionalLength(null)
    }
}

/** the height of lines of text: normal, a multiple of the font size, or a length, like line-height */
sealed interface LineHeight : AttributeOrPropertyValue {
    object Normal : LineHeight {
        override val value: Double? = null
        override fun toString() = "normal"
    }

    class Multiple(override val value: Double) : LineHeight {
        override fun toString() = value.toString()
    }

    class Absolute(override val value: Double) : LineHeight {
        override fun toString() = "${value}px"
    }
}

/**
 * Settings of the font, like font-feature-settings (OpenType features and their values) and font-variation-settings
 * (axes of variable fonts and their values)
 */
class FontSettings(override val value: Map<String, Double>) : AttributeOrPropertyValue {
    override fun toString(): String = if (value.isEmpty()) "normal" else
        value.entries.joinToString(", ") { (tag, v) -> "'$tag' ${if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()}" }

    companion object {
        val NORMAL = FontSettings(emptyMap())
    }
}

/**
 * How far text is moved up from the baseline of its parent, like the baseline-shift property of SVG
 */
sealed interface BaselineShift : AttributeOrPropertyValue {
    /** moved down by half the height of the font of the text, like Chrome and Firefox do */
    object Sub : BaselineShift {
        override val value: BaselineShift get() = this
        override fun toString() = "sub"
    }

    /** moved up by half the height of the font of the text */
    object Super : BaselineShift {
        override val value: BaselineShift get() = this
        override fun toString() = "super"
    }

    /** moved up by a length, a percentage is one of the font size */
    class Value(override val value: Length) : BaselineShift {
        override fun toString() = value.toString()
    }

    companion object {
        val NONE = Value(0.0.pixels)
    }
}

/**
 * The lines drawn along text, like the text-decoration property of CSS
 */
class TextDecoration(
    val underline: Boolean = false,
    val overline: Boolean = false,
    val lineThrough: Boolean = false
) : AttributeOrPropertyValue {
    override val value: TextDecoration get() = this

    override fun toString(): String = listOfNotNull(
        "underline".takeIf { underline }, "overline".takeIf { overline }, "line-through".takeIf { lineThrough }
    ).joinToString(" ").ifEmpty { "none" }

    companion object {
        val NONE = TextDecoration()
    }
}

/**
 * How shapes are rendered, like the shape-rendering property of SVG: shapes are not anti-aliased with [CRISP_EDGES]
 * and [OPTIMIZE_SPEED]
 */
enum class ShapeRendering(private val css: String, override val value: Boolean) : AttributeOrPropertyValue {
    AUTO("auto", true),
    OPTIMIZE_SPEED("optimizeSpeed", false),
    CRISP_EDGES("crispEdges", false),
    GEOMETRIC_PRECISION("geometricPrecision", true);

    override fun toString(): String = css
}

/**
 * The length that the author gives a path, like the pathLength attribute of SVG: lengths along the path, like
 * those of dashes, are scaled by the actual length of the path divided by [value]. Null for the actual length.
 */
class PathLength(override val value: Double?) : AttributeOrPropertyValue {
    override fun toString(): String = value?.toString() ?: ""

    companion object {
        val NONE = PathLength(null)
    }
}

sealed interface ViewBox : AttributeOrPropertyValue {
    override val value: Rectangle?

    class Value(override val value: Rectangle) : ViewBox {
        override fun toString(): String =
            "${value.x.toInt()} ${value.y.toInt()} ${value.width.toInt()} ${value.height.toInt()}"
    }

    /**
     * The viewBox has not been defined,
     * **not** that it doesn't exist.
     */
    object None : ViewBox {
        override val value: Rectangle? = null
        override fun toString(): String = ""
    }
}

@JvmRecord
private data class PropertyBehavior(val inherit: Inheritance, val initial: AttributeOrPropertyValue)

private object PropertyBehaviors {
    val behaviors = HashMap<AttributeOrPropertyKey, PropertyBehavior>()
}

private class PropertyDelegate<T : AttributeOrPropertyValue>(
    val name: AttributeOrPropertyKey,
    inheritance: Inheritance,
    val initial: T
) {
    init {
        PropertyBehaviors.behaviors[name] = PropertyBehavior(inheritance, initial)
    }

    @Suppress("UNCHECKED_CAST")
    operator fun getValue(style: Styleable, property: KProperty<*>): T {
        return (style[name] ?: PropertyBehaviors.behaviors[name]!!.initial) as T
    }

    operator fun setValue(style: Styleable, property: KProperty<*>, value: T?) {
        style[name] = value
    }
}

sealed class Styleable {
    val properties = HashMap<AttributeOrPropertyKey, AttributeOrPropertyValue?>()

    operator fun get(name: AttributeOrPropertyKey) = properties[name]

    operator fun set(name: AttributeOrPropertyKey, value: AttributeOrPropertyValue?) {
        properties[name] = value
    }

    infix fun inherit(from: Style): Style {
        return Style().also {
            from.properties.forEach { (name, value) ->
                if (PropertyBehaviors.behaviors[name]?.inherit == INHERIT) {
                    it.properties[name] = value
                }
            }
            it.properties.putAll(properties)
        }
    }

    // Because AttributeOrPropertyValue has a toString override,
    // we can abuse it for equality checks.
    fun isInherited(from: Styleable, attributeKey: AttributeOrPropertyKey): Boolean =
        when (this.properties[attributeKey].toString()) {
            from.properties[attributeKey].toString() -> true
            PropertyBehaviors.behaviors[attributeKey]?.initial.toString() -> true
            else -> false
        }
}

/** the initial value of the property [name], the value it has when it is not set and not inherited */
fun initialValue(name: AttributeOrPropertyKey): AttributeOrPropertyValue? = PropertyBehaviors.behaviors[name]?.initial

/** true when the property [name] is inherited by the contents of groups */
fun isInheritedProperty(name: AttributeOrPropertyKey): Boolean = PropertyBehaviors.behaviors[name]?.inherit == INHERIT

class DocumentStyle : Styleable()
class Style : Styleable()

var DocumentStyle.viewBox by PropertyDelegate<ViewBox>(VIEW_BOX, RESET, ViewBox.None)
var DocumentStyle.preserveAspectRatio by PropertyDelegate<AspectRatio>(
    PRESERVE_ASPECT_RATIO,
    RESET, AspectRatio.DEFAULT
)

var Style.stroke by PropertyDelegate<Paint>(STROKE, INHERIT, Paint.None)
var Style.strokeOpacity by PropertyDelegate<Numeric>(STROKE_OPACITY, INHERIT, Numeric.Rational(1.0))
var Style.strokeWeight by PropertyDelegate<Length>(STROKE_WIDTH, INHERIT, 1.0.pixels)
var Style.miterLimit by PropertyDelegate<Numeric>(STROKE_MITERLIMIT, INHERIT, Numeric.Rational(4.0))
var Style.lineCap by PropertyDelegate<LineCap>(STROKE_LINECAP, INHERIT, LineCap.Butt)
var Style.lineJoin by PropertyDelegate<LineJoin>(STROKE_LINEJOIN, INHERIT, LineJoin.Miter)

var Style.fill by PropertyDelegate<Paint>(FILL, INHERIT, Paint.RGB(ColorRGBa.BLACK))
var Style.fillOpacity by PropertyDelegate<Numeric>(FILL_OPACITY, INHERIT, Numeric.Rational(1.0))
var Style.fillRule by PropertyDelegate<FillRule>(FILL_RULE, INHERIT, FillRule.NonZero)

/** the color that [Paint.CurrentColor] refers to, like the color property of SVG */
var Style.color by PropertyDelegate<Paint>(COLOR, INHERIT, Paint.RGB(ColorRGBa.BLACK))
var Style.vectorEffect by PropertyDelegate<VectorEffect>(VECTOR_EFFECT, RESET, VectorEffect.None)
var Style.clipPath by PropertyDelegate<ClipPath>(CLIP_PATH, RESET, ClipPath.None)
var Style.mask by PropertyDelegate<MaskReference>(MASK, RESET, MaskReference.None)
var Style.filter by PropertyDelegate<FilterReference>(FILTER, RESET, FilterReference.None)
var Style.mixBlendMode by PropertyDelegate<MixBlendMode>(MIX_BLEND_MODE, RESET, MixBlendMode.NORMAL)
var Style.isolation by PropertyDelegate<Isolation>(ISOLATION, RESET, Isolation.AUTO)
var Style.clipRule by PropertyDelegate<FillRule>(CLIP_RULE, INHERIT, FillRule.NonZero)
var Style.markerStart by PropertyDelegate<MarkerReference>(MARKER_START, INHERIT, MarkerReference.None)
var Style.markerMid by PropertyDelegate<MarkerReference>(MARKER_MID, INHERIT, MarkerReference.None)
var Style.markerEnd by PropertyDelegate<MarkerReference>(MARKER_END, INHERIT, MarkerReference.None)
var Style.paintOrder by PropertyDelegate<PaintOrder>(PAINT_ORDER, INHERIT, PaintOrder.Normal)
var Style.strokeDashArray by PropertyDelegate<DashArray>(STROKE_DASHARRAY, INHERIT, DashArray.None)
var Style.strokeDashOffset by PropertyDelegate<Length>(STROKE_DASHOFFSET, INHERIT, 0.0.pixels)

var Style.transform by PropertyDelegate<Transform>(TRANSFORM, RESET, Transform.None)

// Okay so the spec says `display` isn't inheritable, but effectively acts so
// when the element and its children are excluded from the rendering tree.
var Style.display by PropertyDelegate<Display>(DISPLAY, RESET, Display.Inline)
var Style.opacity by PropertyDelegate<Numeric>(OPACITY, RESET, Numeric.Rational(1.0))
var Style.visibility by PropertyDelegate<Visibility>(VISIBILITY, INHERIT, Visibility.Visible)

var Style.x by PropertyDelegate<Length>(X, RESET, 0.0.pixels)
var Style.y by PropertyDelegate<Length>(Y, RESET, 0.0.pixels)
var Style.width by PropertyDelegate<Length>(WIDTH, RESET, 768.0.pixels)
var Style.height by PropertyDelegate<Length>(HEIGHT, RESET, 576.0.pixels)

var Style.shadeStyle by PropertyDelegate<Shade>(SHADESTYLE, INHERIT, Shade.Value(ShadeStyle()))

var Style.fontFamily by PropertyDelegate<FontFamily>(FONT_FAMILY, INHERIT, FontFamily(listOf("sans-serif")))
var Style.fontSize by PropertyDelegate<Length>(FONT_SIZE, INHERIT, 16.0.pixels)
var Style.fontWeight by PropertyDelegate<FontWeight>(FONT_WEIGHT, INHERIT, FontWeight(400))
var Style.fontStyle by PropertyDelegate<FontStyle>(FONT_STYLE, INHERIT, FontStyle.NORMAL)
var Style.textAnchor by PropertyDelegate<TextAnchor>(TEXT_ANCHOR, INHERIT, TextAnchor.START)

/** space added after every character of text, like letter-spacing */
var Style.letterSpacing by PropertyDelegate<Length>(LETTER_SPACING, INHERIT, 0.0.pixels)

/** space added to every space between words, like word-spacing */
var Style.wordSpacing by PropertyDelegate<Length>(WORD_SPACING, INHERIT, 0.0.pixels)
var Style.dominantBaseline by PropertyDelegate<DominantBaseline>(DOMINANT_BASELINE, INHERIT, DominantBaseline.AUTO)

/** how far text is moved up from the baseline of its parent, like baseline-shift; the shifts of groups add up */
var Style.baselineShift by PropertyDelegate<BaselineShift>(BASELINE_SHIFT, RESET, BaselineShift.NONE)

/**
 * The lines drawn along text. Unlike CSS, where decorations are drawn in the color of the element that declares
 * them, the lines are drawn in the fill and stroke of the text they are drawn along.
 */
var Style.textDecoration by PropertyDelegate<TextDecoration>(TEXT_DECORATION, INHERIT, TextDecoration.NONE)
var Style.shapeRendering by PropertyDelegate<ShapeRendering>(SHAPE_RENDERING, INHERIT, ShapeRendering.AUTO)
var Style.whiteSpace by PropertyDelegate<WhiteSpace>(WHITE_SPACE, INHERIT, WhiteSpace.NORMAL)
var Style.pointerEvents by PropertyDelegate<PointerEvents>(POINTER_EVENTS, INHERIT, PointerEvents.VISIBLE_PAINTED)
var Style.writingMode by PropertyDelegate<WritingMode>(WRITING_MODE, INHERIT, WritingMode.HORIZONTAL_TB)
var Style.textOrientation by PropertyDelegate<TextOrientation>(TEXT_ORIENTATION, INHERIT, TextOrientation.MIXED)

/** the length that the text of a text element or a tspan is adjusted to, see [lengthAdjust] */
var Style.textLength by PropertyDelegate<OptionalLength>(TEXT_LENGTH, RESET, OptionalLength.NONE)
var Style.lengthAdjust by PropertyDelegate<LengthAdjust>(LENGTH_ADJUST, RESET, LengthAdjust.SPACING)

/** the width that the text of a text element wraps to, like the inline-size property of SVG 2 */
var Style.inlineSize by PropertyDelegate<OptionalLength>(INLINE_SIZE, RESET, OptionalLength.NONE)
var Style.lineHeight by PropertyDelegate<LineHeight>(LINE_HEIGHT, INHERIT, LineHeight.Normal)

/** OpenType features of text, like font-feature-settings; font-variant is read into it */
var Style.fontFeatureSettings by PropertyDelegate<FontSettings>(FONT_FEATURE_SETTINGS, INHERIT, FontSettings.NORMAL)

/** values of the axes of variable fonts, like font-variation-settings */
var Style.fontVariationSettings by PropertyDelegate<FontSettings>(FONT_VARIATION_SETTINGS, INHERIT, FontSettings.NORMAL)
var Style.pathLength by PropertyDelegate<PathLength>(PATH_LENGTH, RESET, PathLength.NONE)

enum class AttributeOrPropertyKey {
    // @formatter:off
    // Attributes
    BASE_PROFILE { override fun toString() = "baseProfile" },
    CLASS { override fun toString() = "class" },
    CX { override fun toString() = "cx" },
    CY { override fun toString() = "cy" },
    D { override fun toString() = "d" },
    DX { override fun toString() = "dx" },
    DY { override fun toString() = "dy" },
    GRADIENT_UNITS { override fun toString() = "gradientUnits" },
    HEIGHT { override fun toString() = "height" },
    ID { override fun toString() = "id" },
    OFFSET { override fun toString() = "offset" },
    PATH_LENGTH { override fun toString() = "pathLength" },
    POINTS { override fun toString() = "points" },
    PRESERVE_ASPECT_RATIO { override fun toString() = "preserveAspectRatio" },
    R { override fun toString() = "r" },
    ROTATE { override fun toString() = "rotate" },
    RX { override fun toString() = "rx" },
    RY { override fun toString() = "ry" },
    SPACE { override fun toString() = "xml:space" },
    STYLE { override fun toString() = "style" },
    TRANSFORM { override fun toString() = "transform" },
    VERSION { override fun toString() = "version" },
    VIEW_BOX { override fun toString() = "viewBox" },
    WIDTH { override fun toString() = "width" },
    X { override fun toString() = "x" },
    X1 { override fun toString() = "x1" },
    X2 { override fun toString() = "x2" },
    Y { override fun toString() = "y" },
    Y1 { override fun toString() = "y1" },
    Y2 { override fun toString() = "y2" },

    // Properties
    COLOR { override fun toString() = "color" },
    DIRECTION { override fun toString() = "direction" },
    DISPLAY { override fun toString() = "display" },
    DISPLAY_ALIGN { override fun toString() = "display-align" },
    FILL { override fun toString() = "fill" },
    FILL_OPACITY { override fun toString() = "fill-opacity" },
    FILL_RULE { override fun toString() = "fill-rule" },
    FONT_FAMILY { override fun toString() = "font-family" },
    FONT_SIZE { override fun toString() = "font-size" },
    FONT_STYLE { override fun toString() = "font-style" },
    FONT_VARIANT { override fun toString() = "font-variant" },
    FONT_WEIGHT { override fun toString() = "font-weight" },
    OPACITY { override fun toString() = "opacity" },
    PAINT_ORDER { override fun toString() = "paint-order" },
    CLIP_PATH { override fun toString() = "clip-path" },
    CLIP_RULE { override fun toString() = "clip-rule" },
    MASK { override fun toString() = "mask" },
    FILTER { override fun toString() = "filter" },
    MIX_BLEND_MODE { override fun toString() = "mix-blend-mode" },
    ISOLATION { override fun toString() = "isolation" },
    MARKER_START { override fun toString() = "marker-start" },
    MARKER_MID { override fun toString() = "marker-mid" },
    MARKER_END { override fun toString() = "marker-end" },
    LETTER_SPACING { override fun toString() = "letter-spacing" },
    WORD_SPACING { override fun toString() = "word-spacing" },
    DOMINANT_BASELINE { override fun toString() = "dominant-baseline" },
    BASELINE_SHIFT { override fun toString() = "baseline-shift" },
    TEXT_DECORATION { override fun toString() = "text-decoration" },
    SHAPE_RENDERING { override fun toString() = "shape-rendering" },
    WHITE_SPACE { override fun toString() = "white-space" },
    POINTER_EVENTS { override fun toString() = "pointer-events" },
    WRITING_MODE { override fun toString() = "writing-mode" },
    TEXT_ORIENTATION { override fun toString() = "text-orientation" },
    TEXT_LENGTH { override fun toString() = "textLength" },
    LENGTH_ADJUST { override fun toString() = "lengthAdjust" },
    INLINE_SIZE { override fun toString() = "inline-size" },
    LINE_HEIGHT { override fun toString() = "line-height" },
    FONT_FEATURE_SETTINGS { override fun toString() = "font-feature-settings" },
    FONT_VARIATION_SETTINGS { override fun toString() = "font-variation-settings" },
    STOP_COLOR { override fun toString() = "stop-color" },
    STOP_OPACITY { override fun toString() = "stop-opacity" },
    STROKE { override fun toString() = "stroke" },
    STROKE_DASHARRAY { override fun toString() = "stroke-dasharray" },
    STROKE_DASHOFFSET { override fun toString() = "stroke-dashoffset" },
    STROKE_LINECAP { override fun toString() = "stroke-linecap" },
    STROKE_LINEJOIN { override fun toString() = "stroke-linejoin" },
    STROKE_MITERLIMIT { override fun toString() = "stroke-miterlimit" },
    STROKE_OPACITY { override fun toString() = "stroke-opacity" },
    STROKE_WIDTH { override fun toString() = "stroke-width" },
    TEXT_ALIGN { override fun toString() = "text-align" },
    TEXT_ANCHOR { override fun toString() = "text-anchor" },
    UNICODE_BIDI { override fun toString() = "unicode-bidi" },
    VECTOR_EFFECT { override fun toString() = "vector-effect" },
    VISIBILITY { override fun toString() = "visibility" },

    // Made-up properties
    // because "Compositions aren't SVGs and yadda yadda"
    // this one's for you, edwin
    SHADESTYLE { override fun toString() = "" };

    abstract override fun toString(): String
    // @formatter:on
}