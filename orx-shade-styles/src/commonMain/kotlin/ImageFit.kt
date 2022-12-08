package org.openrndr.extra.shadestyles

import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.ShadeStyle
import org.openrndr.math.Vector2

class ImageFit : ShadeStyle() {

    var image: ColorBuffer by Parameter()
    var flipV: Boolean by Parameter()
    var position: Vector2 by Parameter()

    init {
        position = Vector2.ZERO
        fragmentTransform = """
    | vec2 uv = c_boundsPosition.xy;
    | vec2 ts = textureSize(p_image, 0);
    | float boundsAR = c_boundsSize.x / c_boundsSize.y;
    | vec2 shift = (p_position + vec2(1.0, 1.0)) / 2.0;
    | 
    | if (c_boundsSize.x > c_boundsSize.y) {
    |   uv.y -= shift.y; 
    |   uv.y /= boundsAR;
    |   uv.y += shift.y;
    | } else {
    |   uv.x -= shift.x;
    |   uv.x *= boundsAR;
    |   uv.x += shift.x;
    | }
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
    | 
    | 
    | if (p_flipV) {
    |   uv.y = 1.0 - uv.y;
    | }
    | #ifndef OR_GL_TEXTURE2D
    | vec4 img = texture(p_image, uv);
    | #else
    | vec4 img = texture2D(p_image, uv);
    | #endif
    | x_fill = img;
    | """.trimMargin()
    }
}

fun imageFit(image: ColorBuffer, position: Vector2 = Vector2.ZERO) : ImageFit {
    val im = ImageFit()
    im.image = image
    im.flipV = true
    im.position = position
    return im
}
