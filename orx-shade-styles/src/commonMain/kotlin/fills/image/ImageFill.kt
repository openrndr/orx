package org.openrndr.extra.shadestyles.fills.image

import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.ObservableHashmap
import org.openrndr.draw.ShadeStyle
import org.openrndr.draw.StyleParameters
import org.openrndr.extra.shadestyles.fills.FillFit
import org.openrndr.extra.shadestyles.fills.FillUnits
import org.openrndr.extra.shadestyles.fills.SpreadMethod
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2

class ImageFillBuilder: StyleParameters {
    override var parameterTypes: ObservableHashmap<String, String> = ObservableHashmap(mutableMapOf()) {}
    override var parameterValues: MutableMap<String, Any> = mutableMapOf()
    override var textureBaseIndex: Int = 2


    /**
     * Specifies the units in which the image fill is defined.
     *
     * The `fillUnits` property determines how the dimensions of the fill area
     * are interpreted when applying an image to a shape or area. It supports
     * several predefined units of measure, as defined in the `FillUnits` enumeration:
     * - `BOUNDS`: Interprets the fill area in the context of the bounding box of the shape.
     * - `WORLD`: Uses world coordinates to define the fill area.
     * - `VIEW`: Aligns the fill area to the view space.
     * - `SCREEN`: Defines the fill area based on screen coordinates.
     *
     * The default value is `FillUnits.BOUNDS`.
     */
    var fillUnits = FillUnits.BOUNDS


    /**
     * Specifies how the image should fit within the bounds of the target shape or area.
     *
     * This property determines the scaling and alignment behavior when applying an image as
     * a fill. The options available are defined in the `FillFit` enum:
     *
     * - `STRETCH`: Stretches the image to completely fill the bounds, potentially distorting
     *   its aspect ratio.
     * - `COVER`: Scales the image to cover the entire bounds while maintaining its
     *   aspect ratio. Portions of the image that exceed the bounds are cropped.
     * - `CONTAIN`: Scales the image to fit entirely within the bounds while maintaining
     *   its aspect ratio, leaving empty space if the aspect ratio differs.
     *
     * The default value for this property is `FillFit.COVER`.
     */
    var fillFit = FillFit.COVER

    /**
     * Specifies the method used to determine how the edges of the image fill are handled along the x-axis.
     * The value is of type [SpreadMethod], and it defines how pixel values are repeated or reflected
     * when they exceed the bounds of the original image.
     * By default, it is set to [SpreadMethod.PAD], which extends the edge pixels of the image.
     */
    var spreadMethodX = SpreadMethod.PAD

    /**
     * Specifies the method used to determine how the edges of the image fill are handled along the y-axis.
     * The value is of type [SpreadMethod], allowing for pixel values to be extended, reflected, or repeated
     * when exceeding the bounds of the image. By default, it is set to [SpreadMethod.PAD], which extends the
     * edge pixels of the image.
     */
    var spreadMethodY = SpreadMethod.PAD


    /**
     * Represents an optional `ColorBuffer` used as an input image for image-based fill operations.
     * This property is used in conjunction with various fill attributes (e.g., `fillUnits`, `fillFit`,
     * `spreadMethodX`, `spreadMethodY`, `fillTransform`) to define how the image is applied to a shape.
     *
     * If this property is not set, an error will be thrown during the build process of the `ImageFill`.
     *
     * The image may be modified via parameters like flipping vertically (`flipV`), scaling,
     * or transforming. It plays a central role in defining visual aspects of a gradient or
     * custom fill style.
     */
    var image: ColorBuffer? = null


    /**
     * A transformation matrix applied to the image fill. This property allows modifying the spatial
     * characteristics of the image used in the fill, such as translation, rotation, or scaling.
     * Using this matrix, the image can be adjusted to achieve specific visual effects or alignment
     * within the fill bounds. By default, it is set to the identity matrix, which means no transformation
     * is applied.
     */
    var fillTransform: Matrix44 = Matrix44.IDENTITY


    /**
     * Customizable GLSL function used to perform domain warping on a given vector.
     * This property allows defining a function written in GLSL that alters the texture coordinate `p`
     * for advanced procedural texturing techniques or creative transformations.
     *
     * The default value is a no-op function that directly returns the input vector:
     * `vec2 if_domainWarp(vec2 p) { return p; }`.
     *
     * This property can be dynamically updated to specify a custom domain warp, such as displacements
     * based on time, noise, or other parameters.
     *
     * To apply domain warping, update this property with a valid GLSL function that operates on `vec2` input
     * and returns a transformed `vec2` coordinate.
     */
    var domainWarpFunction: String = """vec2 if_domainWarp(vec2 p) { return p; }"""


    var scale = 1.0
    fun build(): ImageFill {
        val imageFill = ImageFill(domainWarpFunction)
        imageFill.parameterTypes.putAll(parameterTypes)
        imageFill.parameterValues.putAll(parameterValues)
        imageFill.if_image = image ?: error("image not set")
        imageFill.if_fillUnits = fillUnits.ordinal
        imageFill.if_fillFit = fillFit.ordinal
        imageFill.if_spreadMethodX = spreadMethodX.ordinal
        imageFill.if_spreadMethodY = spreadMethodY.ordinal
        imageFill.if_flipV = image?.flipV ?: false
        imageFill.if_scale = scale
        imageFill.if_fillTransform = fillTransform

        return imageFill
    }
}

/**
 * Provides shaded rendering using an image as the fill for a shape or geometry.
 *
 * The `ImageFill` class enables customization of how an image is applied as a fill,
 * providing properties to control image source, scaling, positioning, and orientation.
 * This is achieved through shader-based rendering, with configurable parameters for
 * precise control over the resulting visual representation.
 *
 * Image behavior is determined by the following properties:
 * - `image`: The image to be used as the fill, represented as a `ColorBuffer`.
 * - `flipV`: A flag to vertically flip the image. This is useful when the vertical
 *   orientation of the original image needs correction.
 * - `position`: A `Vector2` specifying the offset positioning for the image within
 *   the bounds of the shape.
 * - `useScreenBounds`: When enabled, adjusts the image mapping to use the screen
 *   space bounds rather than the shape's bounds.
 *
 * The rendering logic dynamically adjusts the image's aspect ratio and alignment,
 * maintaining proportional scaling between the image and target bounds. This ensures
 * correct rendering for shapes of varying sizes or orientations.
 */
class ImageFill(domainWarpFunction: String = """vec2 if_domainWarp(vec2 p) { return p; }""") : ShadeStyle() {
    var if_image: ColorBuffer by Parameter()
    var if_flipV: Boolean by Parameter()
    var if_position: Vector2 by Parameter()
    var if_fillFit: Int by Parameter()
    var if_fillUnits: Int by Parameter()
    var if_spreadMethodX: Int by Parameter()
    var if_spreadMethodY: Int by Parameter()
    var if_scale: Double by Parameter()
    var if_fillTransform: Matrix44 by Parameter()

    init {
        if_fillTransform = Matrix44.IDENTITY
        if_fillFit = FillFit.COVER.ordinal
        if_fillUnits = FillUnits.BOUNDS.ordinal
        if_spreadMethodX = SpreadMethod.PAD.ordinal
        if_spreadMethodY = SpreadMethod.PAD.ordinal
        if_position = Vector2.ZERO
        if_scale = 1.0
        fragmentPreamble = """$domainWarpFunction"""
        fragmentTransform = """
    | vec2 ts = vec2(textureSize(p_if_image, 0));
    | vec2 uv; // = c_boundsPosition.xy;
    | vec2 boundsSize; // = c_boundsSize.xy;
    | 
    | if (p_if_fillUnits == 0) { // BOUNDS
    |   uv = c_boundsPosition.xy;
    |   boundsSize = c_boundsSize.xy;
    | } else if (p_if_fillUnits == 1) { // WORLD
    |   boundsSize = vec2(textureSize(p_if_image, 0));
    |   uv = v_worldPosition.xy / boundsSize;
    | }
    | 
    | float boundsAR = boundsSize.x / boundsSize.y;
    | vec2 shift = (p_if_position + vec2(1.0, 1.0)) / 2.0;
    | 
    | if (boundsSize.x >boundsSize.y) {
    |   uv.y -= shift.y; 
    |   uv.y /= boundsAR;
    |   uv.y += shift.y;
    | } else {
    |   uv.x -= shift.x;
    |   uv.x *= boundsAR;
    |   uv.x += shift.x;
    | }
    | uv = if_domainWarp(uv);
    | uv = (p_if_fillTransform * vec4(uv, 0.0, 1.0)).xy;
    | uv -= vec2(0.5);
    | uv /= p_if_scale; 
    | uv += vec2(0.5);
    | float textureAR = ts.x / ts.y;
    | if (ts.x > ts.y) {
    |   uv.x -= 0.5;
    |   uv.x /= textureAR;
    |   uv.x += 0.5;
    | } else {
    |   uv.y -= 0.5;
    |   uv.y *= textureAR;
    
    |   uv.y += 0.5;
    | }
    | float alphaMask = 1.0;
    | if (p_if_spreadMethodX == 0) { // PAD
    |   alphaMask *= uv.x >= 0.0 && uv.x < 1.0 ? 1.0 : 0.0;
    |   uv.x = clamp(uv.x, 0.0, 1.0);
    | } else if (p_if_spreadMethodX == 1) { // REFLECT
    |   uv.x = 2.0 * abs(uv.x / 2.0 - floor(uv.x / 2.0 + 0.5));
    | } else if (p_if_spreadMethodX == 2) { // REPEAT
    |   uv.x = mod(uv.x, 1.0);
    | }
    | if (p_if_spreadMethodY == 0) { // PAD
    |   alphaMask *= uv.y >= 0.0 && uv.y < 1.0 ? 1.0 : 0.0;
    |   uv.y = clamp(uv.y, 0.0, 1.0);
    | } else if (p_if_spreadMethodY == 1) { // REFLECT
    |   uv.y = 2.0 * abs(uv.y / 2.0 - floor(uv.y / 2.0 + 0.5));
    | } else if (p_if_spreadMethodY == 2) { // REPEAT
    |   uv.y = mod(uv.y, 1.0);
    | }
    | 
    | if (!p_if_flipV) {
    |   uv.y = 1.0 - uv.y;
    | }
    | 
    | vec4 img = texture(p_if_image, uv);
    | img.a *= alphaMask;
    | x_fill *= img;
    | """.trimMargin()
    }
}


/**
 * Creates an ImageFill instance using the provided configuration.
 *
 * The method allows for the configuration of image fill options such as scaling,
 * transformations, spread methods, and more by applying the specified configuration
 * to an internal ImageFillBuilder.
 *
 * @param builder A lambda that configures the properties of the ImageFillBuilder
 *                instance used to construct the ImageFill object.
 * @return An ImageFill object configured based on the input builder settings.
 */
fun imageFill(builder: ImageFillBuilder.() -> Unit): ImageFill {
    val im = ImageFillBuilder().apply(builder).build()
    return im
}