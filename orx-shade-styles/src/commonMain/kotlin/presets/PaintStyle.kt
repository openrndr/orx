package org.openrndr.extra.shadestyles.presets

import org.openrndr.draw.ShadeStyle
import org.openrndr.math.Matrix44
import org.openrndr.math.transforms.transform
import org.openrndr.shape.Rectangle

/** the coordinates that a paint is read at */
enum class PaintUnits {
    /** 0 to 1 over the bounds of the shape, like SVG's objectBoundingBox */
    BOUNDS,

    /** the coordinates of the shape itself, before the model transform, like SVG's userSpaceOnUse */
    OBJECT,

    /** the coordinates after the model transform, before the view transform */
    WORLD,

    /** the coordinates after the model and the view transform, before the projection */
    VIEW,

    /** the position on the target in points, from its bottom left corner, with y up */
    SCREEN
}

/** how a paint in [PaintUnits.BOUNDS] units fits bounds that are not square */
enum class PaintFit {
    /** the 0..1 square of the paint stretches over the bounds */
    STRETCH,

    /** the 0..1 square of the paint fits inside the bounds, centered, keeping its aspect ratio */
    FIT,

    /** the 0..1 square of the paint covers the bounds, centered, keeping its aspect ratio */
    FILL
}

/**
 * A paint of a [PaintStyle]: [source] read at coordinates in [units], transformed by the inverse of [transform]. In
 * [PaintUnits.BOUNDS] units [fit] keeps the aspect ratio of the paint in bounds that are not square, before the
 * transform; for other units it does nothing, put it in the transform.
 */
class PaintLayer(
    val source: SourcePreset,
    val units: PaintUnits = PaintUnits.BOUNDS,
    val transform: Matrix44 = Matrix44.IDENTITY,
    val fit: PaintFit = PaintFit.STRETCH
)

/**
 * A shade style that paints the fills and strokes of shapes with sources, such as gradients: the color of the source
 * replaces the color of the fill or stroke, their alpha multiplies. A fill or stroke without a paint keeps its
 * color. All [PaintUnits] work for shapes, contours, circles and rectangles, also drawn in batches.
 *
 * [PaintUnits.BOUNDS] reads `c_boundsPosition`. For shapes, a paint over the bounds of a shape can be drawn in
 * [PaintUnits.OBJECT] units with a transform that maps 0..1 to the bounds, see [boundsTransform].
 */
class PaintStyle(val fill: PaintLayer?, val stroke: PaintLayer?) : ShadeStyle() {
    init {
        fragmentTransform = buildString {
            for ((name, layer) in listOf("fill" to fill, "stroke" to stroke)) {
                if (layer == null) continue
                val coordinate = when (layer.units) {
                    PaintUnits.BOUNDS -> when (layer.fit) {
                        PaintFit.STRETCH -> "c_boundsPosition.xy"
                        PaintFit.FIT -> "((c_boundsPosition.xy - 0.5) * c_boundsSize.xy / max(min(c_boundsSize.x, c_boundsSize.y), 1e-8) + 0.5)"
                        PaintFit.FILL -> "((c_boundsPosition.xy - 0.5) * c_boundsSize.xy / max(max(c_boundsSize.x, c_boundsSize.y), 1e-8) + 0.5)"
                    }
                    PaintUnits.OBJECT -> "v_objectPosition.xy"
                    PaintUnits.WORLD -> "v_worldPosition.xy"
                    PaintUnits.VIEW -> "v_viewPosition.xy"
                    PaintUnits.SCREEN -> "c_screenPosition.xy"
                }
                appendLine("{")
                appendLine("    vec2 p = (p_${name}Transform * vec4($coordinate, 0.0, 1.0)).xy;")
                appendLine("    vec4 c = p_${name}Paint(p);")
                appendLine("    x_$name = vec4(c.rgb, c.a * x_$name.a);")
                appendLine("}")
                parameter("${name}Paint", layer.source)
                parameter("${name}Transform", layer.transform.inversed)
            }
        }
    }

    /**
     * The transform of the fill paint, which replaces the transform of [fill]. Changing it does not compile another
     * shader, shapes with different transforms can share a paint style.
     */
    var fillTransform: Matrix44 = fill?.transform ?: Matrix44.IDENTITY
        set(value) {
            field = value
            if (fill != null) parameter("fillTransform", value.inversed)
        }

    /** the transform of the stroke paint, which replaces the transform of [stroke], see [fillTransform] */
    var strokeTransform: Matrix44 = stroke?.transform ?: Matrix44.IDENTITY
        set(value) {
            field = value
            if (stroke != null) parameter("strokeTransform", value.inversed)
        }
}

/**
 * Builds a [PaintStyle]
 */
class PaintStyleBuilder {
    private var fill: PaintLayer? = null
    private var stroke: PaintLayer? = null

    /** paints fills with [source] at coordinates in [units], transformed by [transform], fitted by [fit] */
    fun fill(source: SourcePreset, units: PaintUnits = PaintUnits.BOUNDS, transform: Matrix44 = Matrix44.IDENTITY, fit: PaintFit = PaintFit.STRETCH) {
        fill = PaintLayer(source, units, transform, fit)
    }

    /** paints strokes with [source] at coordinates in [units], transformed by [transform], fitted by [fit] */
    fun stroke(source: SourcePreset, units: PaintUnits = PaintUnits.BOUNDS, transform: Matrix44 = Matrix44.IDENTITY, fit: PaintFit = PaintFit.STRETCH) {
        stroke = PaintLayer(source, units, transform, fit)
    }

    fun build(): PaintStyle = PaintStyle(fill, stroke)
}

/**
 * A shade style that paints fills and strokes with sources, such as gradients:
 *
 * ```
 * drawer.shadeStyle = paintStyle {
 *     fill(LinearGradientPaint(stops = listOf(ColorStop(0.0, ColorRGBa.PINK), ColorStop(1.0, ColorRGBa.BLUE))))
 *     stroke(NoiseSource(), PaintUnits.OBJECT, transform = Matrix44.scale(100.0, 100.0, 1.0))
 * }
 * ```
 */
fun paintStyle(builder: PaintStyleBuilder.() -> Unit): PaintStyle = PaintStyleBuilder().apply(builder).build()

/**
 * The transform that maps 0..1 to [bounds], which turns a paint in [PaintUnits.BOUNDS] units into one in
 * [PaintUnits.OBJECT] units: `boundsTransform(shape.bounds) * paintTransform`
 */
fun boundsTransform(bounds: Rectangle): Matrix44 = transform {
    translate(bounds.x, bounds.y, 0.0)
    scale(bounds.width, bounds.height, 1.0)
}
