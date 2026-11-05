package org.openrndr.extra.shadestyles.presets

import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.glslFunction
import org.openrndr.math.Vector4

/**
 * The colors of [texture], read at texture coordinates: 0..1 covers the texture once, the wrap mode of the texture
 * decides what lies beyond, set it to repeat to tile the texture. The texture holds premultiplied colors, as render
 * targets that are drawn to do, the colors are read without the premultiplication. Replacing the texture does not
 * compile another shader.
 */
class TexturePaint(texture: ColorBuffer) : SourcePreset() {
    override val dynamicParameter = glslFunction<Vector4>(
        "vec2 uv", """
        vec4 c = texture(p_texture, uv);
        if (c.a > 0.0) {
            c.rgb /= c.a;
        }
        return c;
        """.trimIndent()
    )

    /** the texture that is read */
    var texture: ColorBuffer by dynamicParameter.Parameter("texture", texture)
}
