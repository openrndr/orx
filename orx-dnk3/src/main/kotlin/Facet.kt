package org.openrndr.extra.dnk3

import org.openrndr.draw.BlendMode
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType

enum class FacetType(val shaderFacet: String) {
    WORLD_POSITION("f_worldPosition"),
    VIEW_POSITION("f_viewPosition"),
    CLIP_POSITION("f_clipPosition"),
    WORLD_NORMAL("f_worldNormal"),
    VIEW_NORMAL("f_viewNormal"),
    SPECULAR("f_specular"),
    DIFFUSE("f_diffuse"),
    EMISSIVE("f_emission"),
    AMBIENT("f_ambient"),
    OCCLUSION("f_occlusion"),
    FRAGMENT_ID("f_fragmentID"),
    COLOR("m_color"),
}

abstract class FacetCombiner(val facets: Set<FacetType>, val targetOutput: String) {
    abstract fun generateShader(): String
    override fun toString(): String {
        return "FacetCombiner(facets=$facets, targetOutput='$targetOutput')"
    }
}

abstract class ColorBufferFacetCombiner(facets: Set<FacetType>,
                                        targetOutput: String,
                                        val format: ColorFormat,
                                        val type: ColorType,
                                        val blendMode: BlendMode = BlendMode.BLEND) : FacetCombiner(facets, targetOutput) {

}

class MomentsFacet : ColorBufferFacetCombiner(setOf(FacetType.WORLD_POSITION), "moments", ColorFormat.RG, ColorType.FLOAT16) {
    override fun generateShader(): String {
        return """
            float depth = length(v_viewPosition);
            float dx = dFdx(depth);
            float dy = dFdy(depth);
            o_$targetOutput = vec4(depth, depth*depth + 0.25 * dx*dx+dy*dy, 0.0, 1.0);
        """
    }
}

class DiffuseSpecularFacet : ColorBufferFacetCombiner(setOf(FacetType.DIFFUSE, FacetType.SPECULAR),
        "diffuseSpecular", ColorFormat.RGB, ColorType.FLOAT16) {
    override fun generateShader(): String =
            "o_$targetOutput = vec4( max(vec3(0.0), f_diffuse.rgb) + max(vec3(0.0), f_specular.rgb), 1.0);"
}
class DiffuseSpecularAlphaFacet : ColorBufferFacetCombiner(setOf(FacetType.DIFFUSE, FacetType.SPECULAR),
        "diffuseSpecular", ColorFormat.RGB, ColorType.FLOAT16) {
    override fun generateShader(): String =
            "o_$targetOutput = vec4( (max(vec3(0.0), f_diffuse.rgb) + max(vec3(0.0), f_specular.rgb)) * f_alpha, f_alpha);"
}

class AmbientOcclusionFacet : ColorBufferFacetCombiner(setOf(FacetType.AMBIENT, FacetType.OCCLUSION),
        "ambientOcclusion", ColorFormat.RGBa, ColorType.FLOAT16) {
    override fun generateShader(): String =
            "o_$targetOutput = vec4(f_ambient, f_occlusion);"
}

class MaterialFacet : ColorBufferFacetCombiner(setOf(FacetType.DIFFUSE),
        "material", ColorFormat.RGBa, ColorType.UINT8) {
    override fun generateShader(): String =
            "o_$targetOutput = vec4(m_metalness, m_roughness, 0.0, 1.0);"
}

class BaseColorFacet : ColorBufferFacetCombiner(setOf(FacetType.COLOR),
        "baseColor", ColorFormat.RGB, ColorType.UINT8) {
    override fun generateShader(): String = "o_$targetOutput = vec4(m_color.rgb, 1.0);"
}

class DiffuseFacet : ColorBufferFacetCombiner(setOf(FacetType.DIFFUSE),
        "diffuse", ColorFormat.RGB, ColorType.FLOAT16) {
    override fun generateShader(): String =
            "o_$targetOutput = vec4( max(vec3(0.0), f_diffuse.rgb), 1.0 );"
}

class SpecularFacet : ColorBufferFacetCombiner(setOf(FacetType.SPECULAR),
        "diffuseSpecular", ColorFormat.RGB, ColorType.FLOAT16) {
    override fun generateShader(): String =
            "o_$targetOutput = vec4( max(vec3(0.0), f_specular.rgb), 1.0);"
}

class EmissiveFacet: ColorBufferFacetCombiner(setOf(FacetType.EMISSIVE),
        "emissive", ColorFormat.RGB, ColorType.FLOAT16) {
    override fun generateShader(): String =
            "o_$targetOutput =  vec4(f_emission, 1.0);"
}

class EmissiveAlphaFacet: ColorBufferFacetCombiner(setOf(FacetType.EMISSIVE),
        "emissive", ColorFormat.RGB, ColorType.FLOAT16, BlendMode.OVER) {
    override fun generateShader(): String =
            "o_$targetOutput =  vec4(f_emission, f_alpha);"
}

class PositionFacet : ColorBufferFacetCombiner(setOf(FacetType.WORLD_POSITION), "position", ColorFormat.RGB, ColorType.FLOAT16) {
    override fun generateShader(): String = "o_$targetOutput = vec4(v_worldPosition.rgb, 1.0);"
}

class NormalFacet : ColorBufferFacetCombiner(setOf(FacetType.WORLD_NORMAL), "normal", ColorFormat.RGB, ColorType.FLOAT16) {
    override fun generateShader(): String = "o_$targetOutput = vec4(v_worldNormal.rgb, 1.0);"
}

class ViewDepthFacet : ColorBufferFacetCombiner(setOf(FacetType.VIEW_POSITION), "viewDepth", ColorFormat.R, ColorType.FLOAT16) {
    override fun generateShader(): String = "o_$targetOutput.r = v_viewPosition.z;"
}
class ClipDepthFacet : ColorBufferFacetCombiner(setOf(FacetType.CLIP_POSITION), "clipDepth", ColorFormat.R, ColorType.FLOAT32) {
    override fun generateShader(): String = "o_$targetOutput.r = gl_FragCoord.z;"
}


class ViewPositionFacet : ColorBufferFacetCombiner(setOf(FacetType.VIEW_POSITION), "viewPosition", ColorFormat.RGB, ColorType.FLOAT32) {
    override fun generateShader(): String = "o_$targetOutput.rgb = v_viewPosition.rgb;"
}

class ViewNormalFacet : ColorBufferFacetCombiner(setOf(FacetType.VIEW_NORMAL), "viewNormal", ColorFormat.RGB, ColorType.FLOAT16) {
    override fun generateShader(): String = "o_$targetOutput.rgb = normalize( (u_viewNormalMatrix * vec4(f_worldNormal,0.0)).xyz );"
}

class ClipPositionFacet : ColorBufferFacetCombiner(setOf(FacetType.CLIP_POSITION), "position", ColorFormat.RGB, ColorType.FLOAT16) {
    override fun generateShader() = "o_$targetOutput.rgb = gl_FragCoord.xyz;"
}

class FragmentIDFacet: ColorBufferFacetCombiner(setOf(FacetType.FRAGMENT_ID), "fragmentID", ColorFormat.R, ColorType.UINT16_INT) {
    override fun generateShader(): String {
        return "o_$targetOutput = f_fragmentID;"
    }
}

class LDRColorFacet : ColorBufferFacetCombiner(setOf(FacetType.DIFFUSE, FacetType.SPECULAR, FacetType.EMISSIVE), "color", ColorFormat.RGBa, ColorType.UINT8) {
    override fun generateShader() = """
    vec3 finalColor =  (max(vec3(0.0), f_diffuse.rgb) + max(vec3(0.0),f_specular.rgb) + max(vec3(0.0), f_emission.rgb) + max(vec3(0.0), f_ambient.rgb)) * (1.0 - f_fog.a) + f_fog.rgb * f_fog.a;
    o_$targetOutput = pow(vec4(finalColor.rgb, 1.0), vec4(1.0/2.2));
    o_$targetOutput *= m_color.a;
    
    """
}

class HDRColorFacet : ColorBufferFacetCombiner(setOf(FacetType.DIFFUSE, FacetType.SPECULAR, FacetType.EMISSIVE), "color", ColorFormat.RGBa, ColorType.FLOAT16) {
    override fun generateShader() = """
    vec3 finalColor =  (max(vec3(0.0), f_diffuse.rgb) + max(vec3(0.0),f_specular.rgb) + max(vec3(0.0), f_emission.rgb) + max(vec3(0.0), f_ambient.rgb)) * (1.0 - f_fog.a) + f_fog.rgb * f_fog.a;
    o_$targetOutput = vec4(finalColor.rgb, 1.0);
    o_$targetOutput *= m_color.a;
    """
}

class DiffuseIrradianceFacet : ColorBufferFacetCombiner(setOf(FacetType.DIFFUSE, FacetType.SPECULAR), "color", ColorFormat.RGBa, ColorType.UINT8) {
    override fun generateShader() = """
    vec3 finalColor =  (max(vec3(0.0), f_diffuse.rgb) + max(vec3(0.0), f_emission.rgb));
    o_$targetOutput = vec4(finalColor.rgb, 1.0);
    
    
    """
}