package org.openrndr.extra.dnk3

import org.openrndr.draw.Cubemap
import org.openrndr.draw.RenderTarget
import org.openrndr.draw.ShadeStyle
import org.openrndr.draw.shadeStyle

interface Material {
    var doubleSided: Boolean
    var transparent: Boolean
    fun generateShadeStyle(context: MaterialContext): ShadeStyle
    fun applyToShadeStyle(context: MaterialContext, shadeStyle: ShadeStyle)
}

class DummyMaterial : Material {
    override var doubleSided: Boolean = true
    override var transparent: Boolean = false


    override fun generateShadeStyle(context: MaterialContext): ShadeStyle {
        return shadeStyle {
            fragmentTransform = """
                x_fill.rgb = vec3(normalize(v_viewNormal).z);
            """.trimIndent()
        }
    }

    override fun applyToShadeStyle(context: MaterialContext, shadeStyle: ShadeStyle) {

    }

}

data class MaterialContext(val pass: RenderPass,
                           val lights: List<NodeContent<Light>>,
                           val fogs: List<NodeContent<Fog>>,
                           val shadowMaps: Map<ShadowLight, RenderTarget>,
                           val meshCubemaps: Map<Mesh, Cubemap>
)

