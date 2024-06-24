package org.openrndr.extra.dnk3

import org.openrndr.draw.Cubemap
import org.openrndr.draw.RenderTarget
import org.openrndr.draw.ShadeStyle
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.dnk3.features.IrradianceSH

interface Material {
    val name: String?
    var doubleSided: Boolean
    var transparent: Boolean
    val fragmentID: Int
    fun generateShadeStyle(context: MaterialContext, primitiveContext: PrimitiveContext): ShadeStyle
    fun applyToShadeStyle(context: MaterialContext, shadeStyle: ShadeStyle)
}

class DummyMaterial : Material {
    override var name: String? = null
    override var doubleSided: Boolean = true
    override var transparent: Boolean = false
    override var fragmentID = 0

    override fun generateShadeStyle(context: MaterialContext, primitiveContext: PrimitiveContext): ShadeStyle {
        return shadeStyle {
            fragmentPreamble = """
                int f_fragmentID = p_fragmentID;
            """.trimIndent()

            fragmentTransform = """
                x_fill.rgb = vec3(normalize(v_viewNormal).z);
            """.trimIndent()

            parameter("fragmentID", fragmentID)
        }
    }

    override fun applyToShadeStyle(context: MaterialContext, shadeStyle: ShadeStyle) {

    }

}

data class MaterialContext(val pass: RenderPass,
                           val lights: List<NodeContent<Light>>,
                           val fogs: List<NodeContent<Fog>>,
                           val shadowMaps: Map<ShadowLight, RenderTarget>,
                           val meshCubemaps: Map<Mesh, Cubemap>,
                           val irradianceProbeCount: Int
                           ) {

    var irradianceSH: IrradianceSH? = null
}



@JvmRecord
data class PrimitiveContext(val hasNormalAttribute: Boolean, val hasSkinning: Boolean)

@JvmRecord
data class ContextKey(val materialContext: MaterialContext, val primitiveContext: PrimitiveContext)
