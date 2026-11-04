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
    OBJECT
}

/**
 * A paint of a [PaintStyle]: [source] read at coordinates in [units], transformed by the inverse of [transform]
 */
class PaintLayer(
    val source: SourcePreset,
    val units: PaintUnits = PaintUnits.BOUNDS,
    val transform: Matrix44 = Matrix44.IDENTITY
)

/**
 * A shade style that paints the fills and strokes of shapes with sources, such as gradients: the color of the source
 * replaces the color of the fill or stroke, their alpha multiplies. A fill or stroke without a paint keeps its
 * color. [PaintUnits.OBJECT] reads the position of the shape and works for shapes and contours.
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
                    PaintUnits.BOUNDS -> "c_boundsPosition.xy"
                    PaintUnits.OBJECT -> "v_objectPosition.xy"
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

    /** paints fills with [source] at coordinates in [units], transformed by [transform] */
    fun fill(source: SourcePreset, units: PaintUnits = PaintUnits.BOUNDS, transform: Matrix44 = Matrix44.IDENTITY) {
        fill = PaintLayer(source, units, transform)
    }

    /** paints strokes with [source] at coordinates in [units], transformed by [transform] */
    fun stroke(source: SourcePreset, units: PaintUnits = PaintUnits.BOUNDS, transform: Matrix44 = Matrix44.IDENTITY) {
        stroke = PaintLayer(source, units, transform)
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
