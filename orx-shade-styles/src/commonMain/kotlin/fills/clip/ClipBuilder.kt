package org.openrndr.extra.shadestyles.fills.clip

import org.openrndr.draw.ObservableHashmap
import org.openrndr.draw.StyleParameters
import org.openrndr.extra.shaderphrases.sdf.sdEllipsePhrase
import org.openrndr.extra.shaderphrases.sdf.sdStarPhrase
import org.openrndr.extra.shadestyles.fills.FillFit
import org.openrndr.extra.shadestyles.fills.FillUnits
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector4
import org.openrndr.shape.Rectangle

class ClipBuilder : StyleParameters {
    override var parameterTypes: ObservableHashmap<String, String> = ObservableHashmap(mutableMapOf()) {}
    override var parameterValues: MutableMap<String, Any> = mutableMapOf()
    override var textureBaseIndex: Int = 2

    /**
     * Specifies the outer threshold for the clipping effect in rendering.
     *
     * Default value is `0.0`.
     */
    var clipOuter: Double by Parameter("clipOuter", initialValue = 0.0)


    /**
     * Defines the inner boundary threshold for applying a clipping mask.
     *
     * The initial value is set to a very large negative number
     * (-1E12) to enable a wide default range.
     */
    var clipInner: Double by Parameter("clipInner", initialValue = -1E12)

    /**
     * Specifies the coordinate space used for defining clip shapes.
     * The value can be one of the `FillUnits` enum constants, such as:
     * - `BOUNDS`: The coordinate space is based on the local bounds of the object.
     * - `WORLD`: The coordinate space is based on the world position.
     * - `VIEW`: The coordinate space is based on the view or camera position.
     * - `SCREEN`: The coordinate space is based on the screen space.
     *
     * Determines how the clip shapes are positioned and sized relative to the object being clipped.
     * The default value is `BOUNDS`.
     */
    var clipUnits: FillUnits = FillUnits.BOUNDS

    /**
     * Defines the strategy for fitting a clip shape to its bounds.
     *
     * This property determines how the clip shape will be scaled or resized to fit
     * within a bounding area. The possible values are specified by the [FillFit]
     * enumeration:
     *
     * - [FillFit.STRETCH]: Stretches the clip shape to fully cover the bounds, ignoring original aspect ratio.
     * - [FillFit.COVER]: Scales the clip shape to completely cover the bounds while maintaining its aspect ratio.
     * - [FillFit.CONTAIN]: Scales the clip shape to fully fit within the bounds while preserving its aspect ratio.
     *
     * The default fit mode is [FillFit.STRETCH].
     */
    var clipFit: FillFit = FillFit.STRETCH
    /**
     * Represents the GLSL function used to define the clipping mask for a shader.
     *
     * The `clipMaskFunction` defines a GLSL function `clipMask` that takes a `vec2` coordinate as input
     * and outputs a `float` value. The function is intended to determine how the clipping is applied
     * based on the input coordinate. The return value typically indicates the mask's boundaries, such
     * as a signed distance to a shape.
     *
     * This property can be customized to define various shapes for clipping masks by overwriting it
     * with specific GLSL code.
     *
     * The default implementation of the `clipMaskFunction` always returns `-1.0`, meaning no clipping is
     * applied unless overridden.
     */
    var clipMaskFunction = """float clipMask(vec2 coord) { return -1.0; }"""

    /**
     * A variable containing a GLSL function as a string that defines a domain warping operation
     * applied to coordinates. Domain warping is typically used to introduce variations or distortions
     * in patterns by altering the input space over which a procedure operates.
     *
     * This variable is used in the shader preamble and the transformation process to
     * modify the coordinate system. By default, the `clipDomainWarp` function returns the input
     * coordinates unchanged, maintaining an identity operation.
     *
     * Developers can override this string with a custom GLSL function to create
     * specific domain warping effects suited for their application.
     */
    var domainWarpFunction = """vec2 clipDomainWarp(vec2 coord) { return coord; }"""

    /**
     * Specifies the feathering amount applied to the edges of the clipping mask.
     * Feathering softens the transition between clipped and unclipped regions, creating a smoother boundary.
     * The value represents the width of the feathering effect, with `0.0` indicating no feathering.
     *
     * A larger value will result in a broader and more gradual transition.
     */
    var feather: Double by Parameter("clipFeather", 0.0)


    /**
     * Determines whether the current clipping mask should be inverted.
     *
     * When set to `true`, the areas outside of the defined clip mask are rendered instead of the areas inside.
     * This parameter is useful for reversing the functionality of a clipping mask,
     * such as highlighting areas outside a specified region or shape.
     */
    var invert: Boolean by Parameter("clipInvert", false)

    var floor: Double by Parameter("clipFloor", 0.0)
    var blend: Double by Parameter("clipBlend", 1.0)
    var clipTransform: Matrix44 by Parameter("clipTransform", Matrix44.IDENTITY)

    private fun structure(): ClipbaseStructure {
        return ClipbaseStructure(
            clipMaskFunction,
            domainWarpFunction
        )
    }

    fun build(): ClipBase {
        val clipBase = ClipBase(structure())
        clipBase.parameterTypes.putAll(parameterTypes)
        clipBase.parameterValues.putAll(parameterValues)
        clipBase.clipUnits = clipUnits.ordinal
        clipBase.clipFit = clipFit.ordinal
        return clipBase
    }

    /**
     * Adds a circular clipping mask to the current clip context.
     *
     * This method configures a circular clipping region with customizable properties
     * defined within the provided builder. The circle is defined by its radius and center.
     *
     * @param builder a lambda with receiver scope of [CircleClipBuilder] allowing configuration
     *                of the circle's attributes such as radius and center position.
     */
    fun circle(builder: CircleClipBuilder.() -> Unit) {
        CircleClipBuilder(this).apply(builder)
    }

    /**
     * Configures a rectangular clipping mask within the current clip context.
     *
     * This method allows setting up a rectangular clipping region with customizable
     * properties defined inside the provided builder.
     *
     * @param builder a lambda with receiver scope of [RectangleClipBuilder], allowing
     *                configuration of the rectangle's attributes such as position
     *                and dimensions.
     */
    fun rectangle(builder: RectangleClipBuilder.() -> Unit) {
        val b = RectangleClipBuilder(this)
        b.apply(builder)
        b.clipRectangle = Vector4(b.rectangle.x, b.rectangle.y, b.rectangle.width, b.rectangle.height)
    }

    /**
     * Adds a star-shaped clipping mask to the current clip context.
     *
     * This method configures a clipping region in the shape of a star with customizable
     * properties defined within the provided builder. The star is defined by its radius,
     * center, number of sides, and sharpness.
     *
     * @param builder a lambda with receiver scope of [StarClipBuilder], allowing configuration
     *                of the star's attributes such as radius, center, sides, and sharpness.
     */
    fun star(builder: StarClipBuilder.() -> Unit) {
        val b = StarClipBuilder(this)
        b.builder()
    }

    /**
     * Adds a line-shaped clipping mask to the current clip context.
     *
     * This method configures a linear clipping region with customizable properties
     * defined within the provided builder. The line is determined by its direction
     * and center point.
     *
     * @param builder a lambda with receiver scope of [LineClipBuilder] allowing
     *                configuration of the line's attributes such as direction and center position.
     */
    fun line(builder: LineClipBuilder.() -> Unit) {
        val b = LineClipBuilder(this)
        b.builder()
    }

    /**
     * Adds an elliptical clipping mask to the current clip context.
     *
     * This method configures an elliptical clipping region with customizable properties
     * defined within the provided builder. The ellipse is characterized by its horizontal
     * and vertical radii, as well as its center position.
     *
     * @param builder a lambda with receiver scope of [EllipseClipBuilder], allowing
     *                configuration of the ellipse's attributes such as radiusX, radiusY,
     *                and center position.
     */
    fun ellipse(builder: EllipseClipBuilder.() -> Unit) {
        val b = EllipseClipBuilder(this)
        b.builder()
    }
}


class CircleClipBuilder(builder: ClipBuilder) {
    /**
     * Defines the radius of the circular clipping mask.
     *
     * The default value is 0.5.
     */
    var radius: Double by builder.Parameter("clipRadius", 0.5)


    /**
     * Specifies the center of the circular clipping mask in normalized coordinates.
     *
     * The center is represented as a [Vector2], where the default value is (0.5, 0.5),
     * positioning the clipping mask at the center of the target area.
     */
    var center: Vector2 by builder.Parameter("clipCenter", Vector2(0.5, 0.5))

    init {
        builder.clipMaskFunction = """
            float clipMask(vec2 coord) {
                float d = distance(coord, p_clipCenter);
                return d - p_clipRadius;
            }
        """.trimIndent()
    }
}

class RectangleClipBuilder(builder: ClipBuilder) {
    /**
     * Represents a rectangular clipping region with configurable properties such as
     * position and dimensions. The rectangle is defined by its coordinates (x, y),
     * width, and height.
     */
    var rectangle = Rectangle(0.0, 0.0, 1.0, 1.0)
    internal var clipRectangle: Vector4 by builder.Parameter("clipRectangle")

    init {
        builder.clipMaskFunction = """
            float clipMask(vec2 coord) {
                vec2 center = p_clipRectangle.xy + p_clipRectangle.zw * 0.5;
                vec2 d2 = abs(coord - center) - p_clipRectangle.zw * 0.5;
                return max(d2.x, d2.y);
            }
        """.trimIndent()
    }
}

class StarClipBuilder(builder: ClipBuilder) {
    /**
     * Defines the radius of the star-shaped clip mask.
     */
    var radius: Double by builder.Parameter("clipRadius", 0.5)

    /**
     * Defines the center point of the star-shaped clip mask as a 2D vector.
     */
    var center: Vector2 by builder.Parameter("clipCenter", Vector2(0.5, 0.5))
    var sides: Int by builder.Parameter("clipSides", 5)
    var sharpness: Double by builder.Parameter("clipSharpness", 0.0)

    init {
        builder.clipMaskFunction = """$sdStarPhrase
            float clipMask(vec2 coord) {
                float d = sdStar(coord - p_clipCenter, p_clipRadius, p_clipSides, p_clipSharpness);
                return d;
            }""".trimIndent()
    }
}

class EllipseClipBuilder(builder: ClipBuilder) {
    /**
     * Defines the horizontal radius of the elliptical clipping region.
     *
     * Defaults to `0.5`.
     */
    var radiusX: Double by builder.Parameter("clipRadiusX", 0.5)


    /**
     * Defines the vertical radius of the elliptical clipping region.
     *
     * Defaults to `0.5`.
     */
    var radiusY: Double by builder.Parameter("clipRadiusY", 0.5)

    /**
     * The center of the clipping ellipse.
     *
     * Defaults to (0.5, 0.5)
     */
    var center: Vector2 by builder.Parameter("clipCenter", Vector2(0.5, 0.5))

    init {
        builder.clipMaskFunction = """$sdEllipsePhrase
            float clipMask(vec2 coord) {
                vec2 d0 = (coord - p_clipCenter);
                float d = sdEllipse(d0, vec2(p_clipRadiusX, p_clipRadiusY));
                return d;
            }
        """.trimIndent()
    }
}


class LineClipBuilder(builder: ClipBuilder) {
    /**
     * Represents the direction vector used for clipping operations in the `LineClipBuilder`.
     * This value determines the directional component for the clipping mask calculation,
     * normalized within the shader implementation.
     *
     * The default value is `Vector2.UNIT_Y`.
     */
    var direction: Vector2 by builder.Parameter("clipDirection", Vector2.UNIT_Y)
    /**
     * Represents the center point used as a reference for clipping operations in the `LineClipBuilder`.
     * This variable defines the central coordinate for the clipping mask relative to the drawable area.
     * It is typically normalized within the range [0.0, 1.0], where `Vector2(0.5, 0.5)` corresponds to
     * the center of the drawable area.
     *
     * The default value is `Vector2(0.5, 0.5)`.
     */
    var center: Vector2 by builder.Parameter("clipCenter", Vector2(0.5, 0.5))

    init {
        builder.clipMaskFunction = """
            float clipMask(vec2 coord) {
                vec2 dir = normalize(p_clipDirection);                
                float distance = dot(dir, coord - p_clipCenter);                
                return distance;
            }
        """.trimIndent()
    }
}

/**
 * Creates and configures a `ClipBase` object using the specified builder.
 *
 * @param builder A lambda function used to define the properties of the `ClipBuilder`.
 * @return A `ClipBase` instance configured with the specified properties.
 */
fun clip(builder: ClipBuilder.() -> Unit): ClipBase {
    return ClipBuilder().apply(builder).build()
}