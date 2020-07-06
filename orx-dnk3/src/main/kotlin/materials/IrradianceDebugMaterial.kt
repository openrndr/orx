package org.openrndr.extra.dnk3.materials

import org.openrndr.draw.ShadeStyle
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.dnk3.Material
import org.openrndr.extra.dnk3.MaterialContext
import org.openrndr.extra.dnk3.PrimitiveContext
import org.openrndr.extra.dnk3.cubemap.glslEvaluateSH
import org.openrndr.extra.dnk3.cubemap.glslFetchSH
import org.openrndr.extra.dnk3.cubemap.genGlslGatherSH

class IrradianceDebugMaterial : Material {
    override val name: String? = null

    override var doubleSided: Boolean = false
    override var transparent: Boolean = false
    override val fragmentID: Int = 0

    override fun generateShadeStyle(context: MaterialContext, primitiveContext: PrimitiveContext): ShadeStyle {
        return shadeStyle {
            fragmentPreamble = """
                $glslEvaluateSH
                $glslFetchSH
                ${genGlslGatherSH(context.irradianceSH!!.xCount, context.irradianceSH!!.yCount, context.irradianceSH!!.zCount, context.irradianceSH!!.spacing, context.irradianceSH!!.offset)}
                vec3 f_emission = vec3(0.0);
            """

            if (context.irradianceSH != null) {
                fragmentTransform = """
                    vec3[9] sh;
                    gatherSH(p_shMap, v_worldPosition, sh);
                x_fill.rgb = evaluateSH(normalize(v_worldNormal), sh);
                
            """.trimIndent()
            } else {
                fragmentTransform = """
                    discard;
                    """
            }
        }
    }

    override fun applyToShadeStyle(context: MaterialContext, shadeStyle: ShadeStyle) {
        context.irradianceSH?.shMap?.let {
            shadeStyle.parameter("shMap", it)
        }

    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IrradianceDebugMaterial) return false

        if (name != other.name) return false
        if (doubleSided != other.doubleSided) return false
        if (transparent != other.transparent) return false
        if (fragmentID != other.fragmentID) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + doubleSided.hashCode()
        result = 31 * result + transparent.hashCode()
        result = 31 * result + fragmentID
        return result
    }


}