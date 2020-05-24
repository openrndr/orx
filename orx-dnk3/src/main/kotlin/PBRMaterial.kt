package org.openrndr.extra.dnk3

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import org.openrndr.math.transforms.normalMatrix
import java.nio.ByteBuffer
import javax.naming.Context
import kotlin.math.cos


private val noise128 by lazy {
    val cb = colorBuffer(128, 128)
    val items = cb.width * cb.height * cb.format.componentCount
    val buffer = ByteBuffer.allocateDirect(items)
    for (y in 0 until cb.height) {
        for (x in 0 until cb.width) {
            for (i in 0 until 4)
                buffer.put((Math.random() * 255).toByte())
        }
    }
    buffer.rewind()
    cb.write(buffer)
    cb.generateMipmaps()
    cb.filter(MinifyingFilter.LINEAR_MIPMAP_LINEAR, MagnifyingFilter.LINEAR)
    cb.wrapU = WrapMode.REPEAT
    cb.wrapV = WrapMode.REPEAT
    cb
}

private fun PointLight.fs(index: Int, hasNormalAttribute: Boolean): String = """
|{
|   vec3 Lr = p_lightPosition$index - v_worldPosition;
|   float distance = length(Lr);
|   float attenuation = 1.0 / (p_lightConstantAttenuation$index +
|   p_lightLinearAttenuation$index * distance + p_lightQuadraticAttenuation$index * distance * distance);
|   vec3 L = normalize(Lr);
|
|   float side = ${if (hasNormalAttribute) "dot(L, N)" else "3.1415"};
|   f_diffuse += attenuation * max(0, side / 3.1415) * p_lightColor$index.rgb * m_color.rgb;
|   f_specular += attenuation * ggx(N, V, L, m_roughness, m_f0) * p_lightColor$index.rgb * m_color.rgb;
}
""".trimMargin()

private fun AmbientLight.fs(index: Int): String = "f_ambient += p_lightColor$index.rgb * ((1.0 - m_metalness) * m_color.rgb);"

private fun DirectionalLight.fs(index: Int, hasNormalAttribute: Boolean) = """
|{
|    vec3 L = normalize(-p_lightDirection$index);
|    float attenuation = 1.0;
|    vec3 H = normalize(V + L);
|    float NoL = ${if (hasNormalAttribute) "clamp(dot(N, L), 0.0, 1.0)" else "1"};
|    float LoH = clamp(dot(L, H), 0.0, 1.0);
|    float NoH = ${if (hasNormalAttribute) "clamp(dot(N, H), 0.0, 1.0)" else "1"};
|    vec3 Lr = (p_lightPosition$index - v_worldPosition);
//|    vec3 L = normalize(Lr);
|    ${shadows.fs(index)}
|    
|    f_diffuse += NoL * attenuation * Fd_Burley(m_roughness * m_roughness, NoV, NoL, LoH) * p_lightColor$index.rgb * m_color.rgb * m_ambientOcclusion;;
|    float Dg = D_GGX(m_roughness * m_roughness, NoH, H);
|    float Vs = V_SmithGGXCorrelated(m_roughness * m_roughness, NoV, NoL);
|    vec3 F = F_Schlick(m_color.rgb * (m_metalness) + 0.04 * (1.0-m_metalness), LoH);
|    vec3 Fr = (Dg * Vs) * F;
|    f_specular += NoL * attenuation * Fr * p_lightColor$index.rgb *  m_ambientOcclusion;;
|}
""".trimMargin()

private fun HemisphereLight.fs(index: Int, hasNormalAttribute: Boolean): String = """
|{
|   float f = ${if (hasNormalAttribute) "dot(N, p_lightDirection$index) * 0.5 + 0.5" else "1"};
|   vec3 irr = ${irradianceMap?.let { "texture(p_lightIrradianceMap$index, N).rgb" } ?: "vec3(1.0)"};
|   f_diffuse += mix(p_lightDownColor$index.rgb, p_lightUpColor$index.rgb, f) * irr * ((1.0 - m_metalness) * m_color.rgb) * m_ambientOcclusion;
|}
""".trimMargin()

private fun SpotLight.fs(index: Int, hasNormalAttribute: Boolean): String {
    val shadows = shadows
    return """
|{
|   vec3 Lr = p_lightPosition$index - v_worldPosition;
|   float distance = length(Lr);
|   float attenuation = 1.0 / (p_lightConstantAttenuation$index +
|   p_lightLinearAttenuation$index * distance + p_lightQuadraticAttenuation$index * distance * distance);
|   attenuation = 1.0;
|   vec3 L = normalize(Lr);

|   float NoL = ${if (hasNormalAttribute) "clamp(dot(N, L), 0.0, 1.0)" else "1"};
|   float side = dot(L, N);
|   float hit = max(dot(-L, p_lightDirection$index), 0.0);
|   float falloff = clamp((hit - p_lightOuterCos$index) / (p_lightInnerCos$index - p_lightOuterCos$index), 0.0, 1.0);
|   attenuation *= falloff;
|   ${shadows.fs(index)}
|   {
|       vec3 H = normalize(V + L);
|       float LoH = clamp(dot(L, H), 0.0, 1.0);
|       float NoH = ${if (hasNormalAttribute) "clamp(dot(N, H), 0.0, 1.0)" else 1.0};
|       f_diffuse += NoL * (0.1+0.9*attenuation) * Fd_Burley(m_roughness * m_roughness, NoV, NoL, LoH) * p_lightColor$index.rgb * m_color.rgb ;
|       float Dg = D_GGX(m_roughness * m_roughness, NoH, H);
|       float Vs = V_SmithGGXCorrelated(m_roughness * m_roughness, NoV, NoL);
|       vec3 F = F_Schlick(m_color.rgb * (m_metalness) + 0.04 * (1.0-m_metalness), LoH);
|       vec3 Fr = (Dg * Vs) * F;
|       f_specular += NoL * attenuation * Fr * p_lightColor$index.rgb;
|   }
}
""".trimMargin()
}

private fun Fog.fs(index: Int): String = """
|{
|    float dz = min(1.0, -v_viewPosition.z/p_fogEnd$index);
|    f_fog = vec4(p_fogColor$index.rgb, dz);
|}
""".trimMargin()

sealed class TextureSource
object DummySource : TextureSource() {
    override fun toString(): String {
        return "DummySource()"
    }
}

abstract class TextureFromColorBuffer(var texture: ColorBuffer, var textureFunction: TextureFunction) : TextureSource()

class TextureFromCode(val code: String) : TextureSource()

private fun TextureFromCode.fs(index: Int, target: TextureTarget) = """
|vec4 tex$index = vec4(0.0, 0.0, 0.0, 1.0);
|{
|vec4 texOut;
|$code;
|tex$index = texOut;
|}
"""

enum class TextureFunction(val function: (String, String) -> String) {
    TILING({ texture, uv -> "texture($texture, $uv)" }),
    NOT_TILING({ texture, uv -> "textureNoTile(p_textureNoise, $texture, x_noTileOffset, $uv)" })
}

/**
 * @param texture the texture to sample from
 * @param input input coordinates, default is "va_texCoord0.xy"
 * @param textureFunction the texture function to use, default is TextureFunction.TILING
 * @param pre the pre-fetch shader code to inject, can only adjust "x_texCoord"
 * @param post the post-fetch shader code to inject, can only adjust "x_texture"
 */
class ModelCoordinates(texture: ColorBuffer,
                       var input: String = "va_texCoord0.xy",
                       var tangentInput: String? = null,
                       textureFunction: TextureFunction = TextureFunction.TILING,
                       var pre: String? = null,
                       var post: String? = null) : TextureFromColorBuffer(texture, textureFunction) {
    override fun toString(): String {
        return "ModelCoordinates(texture: $texture, input: $input, $tangentInput: $tangentInput, textureFunction: $textureFunction, pre: $pre, post: $post)"
    }
}

class Triplanar(texture: ColorBuffer,
                var scale: Double = 1.0,
                var offset: Vector3 = Vector3.ZERO,
                var sharpness: Double = 2.0,
                textureFunction: TextureFunction = TextureFunction.TILING,
                var pre: String? = null,
                var post: String? = null) : TextureFromColorBuffer(texture, textureFunction) {

    init {
        texture.filter(MinifyingFilter.LINEAR_MIPMAP_LINEAR, MagnifyingFilter.LINEAR)
        texture.wrapU = WrapMode.REPEAT
        texture.wrapV = WrapMode.REPEAT
    }
}

private fun ModelCoordinates.fs(index: Int) = """
|vec4 tex$index = vec4(0.0, 0.0, 0.0, 1.0); 
|{
|   vec2 x_texCoord = $input;
|   vec2 x_noTileOffset = vec2(0.0);
|   vec4 x_texture;
|   ${if (pre != null) "{ $pre } " else ""}
|   x_texture = ${textureFunction.function("p_texture$index", "x_texCoord")};
|   ${if (post != null) "{ $post } " else ""}
|   ${if (tangentInput != null) {
    """
|        vec3 normal = normalize(va_normal.xyz);
|        vec3 tangent = normalize(${tangentInput}.xyz);
|        vec3 bitangent = cross(normal, tangent) * ${tangentInput}.w;
|        mat3 tbn = mat3(tangent, bitangent, normal);
|        x_texture.rgb = tbn * normalize(x_texture.rgb - vec3(0.5, 0.5, 0.)) ;
|    
""".trimMargin()

} else ""}   
|   tex$index = x_texture;
|}
""".trimMargin()

private fun Triplanar.fs(index: Int, target: TextureTarget) = """
|vec4 tex$index = vec4(0.0, 0.0, 0.0, 1.0);
|{
|   vec3 x_normal = va_normal;
|   vec3 x_position = va_position;
|   float x_scale = p_textureTriplanarScale$index;
|   vec3 x_offset = p_textureTriplanarOffset$index;
|   vec2 x_noTileOffset = vec2(0.0);
|   ${if (pre != null) "{ $pre } " else ""}
|   vec3 n = normalize(x_normal);
|   vec3 an = abs(n);
|   vec2 uvY = x_position.xz * x_scale + x_offset.x;
|   vec2 uvX = x_position.zy * x_scale + x_offset.y;
|   vec2 uvZ = x_position.xy * x_scale + x_offset.z;
|   vec4 tY = ${textureFunction.function("p_texture$index", "uvY")};
|   vec4 tX = ${textureFunction.function("p_texture$index", "uvX")};
|   vec4 tZ = ${textureFunction.function("p_texture$index", "uvZ")};
|   vec3 weights = pow(an, vec3(p_textureTriplanarSharpness$index));
|   weights = weights / (weights.x + weights.y + weights.z);
|   tex$index = tX * weights.x + tY * weights.y + weights.z * tZ;
|   ${if (target == TextureTarget.NORMAL) """
    |   vec3 tnX = normalize( tX.xyz - vec3(0.5, 0.5, 0.0));
    |   vec3 tnY = normalize( tY.xyz - vec3(0.5, 0.5, 0.0)) * vec3(1.0, -1.0, 1.0);
    |   vec3 tnZ = normalize( tZ.xyz - vec3(0.5, 0.5, 0.0));
    |   vec3 nX = vec3(0.0, tnX.yx);
    |   vec3 nY = vec3(tnY.x, 0.0, tnY.y);
    |   vec3 nZ = vec3(tnZ.xy, 0.0);
    |   vec3 normal = normalize(nX * weights.x + nY * weights.y + nZ * weights.z + n);
    |   tex$index = vec4(normal, 0.0);
""".trimMargin() else ""}
|}
    ${if (post != null) """
        vec4 x_texture = tex$index;
        {
            $post
        }
        tex$index = x_texture;
    """.trimIndent() else ""}
""".trimMargin()

sealed class TextureTarget(val name: String) {
    object NONE : TextureTarget("NONE")
    object COLOR : TextureTarget("COLOR")
    object ROUGHNESS : TextureTarget("ROUGHNESS")
    object METALNESS : TextureTarget("METALNESS")
    object METALNESS_ROUGHNESS : TextureTarget("METALNESS_ROUGHNESS")
    object EMISSION : TextureTarget("EMISSION")
    object NORMAL : TextureTarget("NORMAL")
    object AMBIENT_OCCLUSION : TextureTarget("AMBIENT_OCCLUSION")
    class Height(var scale: Double = 1.0) : TextureTarget("Height")

    override fun toString(): String {
        return "TextureTarget(name: $name)"
    }
}

class Texture(var source: TextureSource,
              var target: TextureTarget) {
    fun copy(): Texture {
        val copied = Texture(source, target)
        return copied
    }

    override fun toString(): String {
        return "Texture(source: $source, target: $target)"
    }
}

class PBRMaterial : Material {
    override fun toString(): String {
        return "PBRMaterial(textures: $textures, color: $color, metalness: $metalness, roughness: $roughness, emissive: $emission))"
    }

    override var doubleSided: Boolean = false
    override var transparent: Boolean = false
    var environmentMap = false
    var color = ColorRGBa.WHITE
    var metalness = 0.5
    var roughness = 1.0
    var emission = ColorRGBa.BLACK

    var vertexPreamble: String? = null
    var vertexTransform: String? = null
    var parameters = mutableMapOf<String, Any>()
    var textures = mutableListOf<Texture>()

    val shadeStyles = mutableMapOf<ContextKey, ShadeStyle>()

//    fun copy(): PBRMaterial {
//        val copied = PBRMaterial()
//        copied.environmentMap = environmentMap
//        copied.color = color
//        copied.opacity = opacity
//        copied.metalness = metalness
//        copied.roughness = roughness
//        copied.emission = emission
//        copied.vertexPreamble = vertexPreamble
//        copied.vertexTransform = vertexTransform
//        copied.parameters.putAll(parameters)
//        copied.textures.addAll(textures.map { it.copy() })
//        return copied
//    }

    override fun generateShadeStyle(materialContext: MaterialContext, primitiveContext: PrimitiveContext): ShadeStyle {
        val cached = shadeStyles.getOrPut(ContextKey(materialContext, primitiveContext)) {
            val needLight = needLight(materialContext)
            val preambleFS = """
            vec4 m_color = p_color;
            float m_f0 = 0.5;
            float m_roughness = p_roughness;
            float m_metalness = p_metalness;
            float m_ambientOcclusion = 1.0;
            vec3 m_emission = p_emission.rgb;
            vec3 m_normal = vec3(0.0, 0.0, 1.0);
            vec4 f_fog = vec4(0.0, 0.0, 0.0, 0.0);
            vec3 f_worldNormal = v_worldNormal;
        """.trimIndent()

            val textureFs = if (needLight) {
                (textures.mapIndexed { index, it ->
                    when (val source = it.source) {
                        DummySource -> "vec4 tex$index = vec4(1.0);"
                        is ModelCoordinates -> source.fs(index)
                        is Triplanar -> source.fs(index, it.target)
                        is TextureFromCode -> source.fs(index, it.target)
                        else -> TODO()
                    }
                } + textures.mapIndexed { index, texture ->
                    when (texture.target) {
                        TextureTarget.NONE -> ""
                        TextureTarget.COLOR -> "m_color.rgb *= pow(tex$index.rgb, vec3(2.2)); m_color.a *= tex$index.a;"
                        TextureTarget.METALNESS -> "m_metalness = tex$index.r;"
                        TextureTarget.ROUGHNESS -> "m_roughness = tex$index.r;"
                        TextureTarget.METALNESS_ROUGHNESS -> "m_metalness = tex$index.r; m_roughness = tex$index.g;"
                        TextureTarget.EMISSION -> "m_emission *= tex$index.rgb;"
                        TextureTarget.NORMAL -> "f_worldNormal = normalize((v_modelNormalMatrix * vec4(tex$index.xyz,0.0)).xyz);"
                        TextureTarget.AMBIENT_OCCLUSION -> "m_ambientOcclusion *= tex$index.r;"
                        is TextureTarget.Height -> ""
                    }
                }).joinToString("\n")
            } else ""

            val displacers = textures.filter { it.target is TextureTarget.Height }

            val skinVS = if (primitiveContext.hasSkinning) """
                    uvec4 j = a_joints;
                    mat4 skinTransform = p_jointTransforms[j.x] * a_weights.x 
                    + p_jointTransforms[j.y] * a_weights.y 
                    + p_jointTransforms[j.z] * a_weights.z 
                    + p_jointTransforms[j.w] * a_weights.w;                    

                    ${if (primitiveContext.hasNormalAttribute) """
                    x_normal = normalize(mat3(skinTransform) * x_normal);                        
                    """.trimIndent() else ""}
                    
                    x_position = (skinTransform * vec4(x_position,1)).xyz;
            """.trimIndent() else ""

            val textureVS = if (displacers.isNotEmpty()) textures.mapIndexed { index, it ->
                if (it.target is TextureTarget.Height) {
                    when (val source = it.source) {
                        DummySource -> "vec4 tex$index = vec4(1.0);"
                        is ModelCoordinates -> source.fs(index)
                        is Triplanar -> source.fs(index, it.target)
                        is TextureFromCode -> source.fs(index, it.target)
                        else -> TODO()
                    } + """
                x_position += x_normal * tex$index.r * p_textureHeightScale$index;
            """.trimIndent()
                } else ""
            }.joinToString("\n") else ""

            val lights = materialContext.lights
            val lightFS = if (needLight) """
        vec3 f_diffuse = vec3(0.0);
        vec3 f_specular = vec3(0.0);
        vec3 f_emission = m_emission;
        vec3 f_ambient = vec3(0.0);
        float f_occlusion = 1.0;
        vec3 N = normalize(f_worldNormal);
        vec3 ep = (p_viewMatrixInverse * vec4(0.0, 0.0, 0.0, 1.0)).xyz;
        vec3 Vr = ep - v_worldPosition;
        vec3 V = normalize(Vr);
        float NoV = ${if (primitiveContext.hasNormalAttribute) "abs(dot(N, V)) + 1e-5" else "1"};

        ${if (environmentMap && materialContext.meshCubemaps.isNotEmpty() && primitiveContext.hasNormalAttribute) """
           {
                vec2 dfg = PrefilteredDFG_Karis(m_roughness, NoV);
                vec3 sc = m_metalness * m_color.rgb + (1.0-m_metalness) * vec3(0.04);

                f_specular.rgb += sc * (texture(p_environmentMap, reflect(-V, normalize(f_worldNormal))).rgb * dfg.x + dfg.y) * m_ambientOcclusion;
            }
        """.trimIndent() else ""}

        ${lights.mapIndexed { index, (node, light) ->
                when (light) {
                    is AmbientLight -> light.fs(index)
                    is PointLight -> light.fs(index, primitiveContext.hasNormalAttribute)
                    is SpotLight -> light.fs(index, primitiveContext.hasNormalAttribute)
                    is DirectionalLight -> light.fs(index, primitiveContext.hasNormalAttribute)
                    is HemisphereLight -> light.fs(index, primitiveContext.hasNormalAttribute)
                    else -> TODO()
                }
            }.joinToString("\n")}

        ${materialContext.fogs.mapIndexed { index, (node, fog) ->
                fog.fs(index)
            }.joinToString("\n")}

    """.trimIndent() else ""
            val rt = RenderTarget.active

            val combinerFS = materialContext.pass.combiners.map {
                it.generateShader()
            }.joinToString("\n")

            val fs = preambleFS + textureFs + lightFS + combinerFS
            val vs = (this@PBRMaterial.vertexTransform ?: "") + textureVS + skinVS

            shadeStyle {
                vertexPreamble = """
                    $shaderNoRepetitionVert
                     ${(this@PBRMaterial.vertexPreamble) ?: ""}
                """.trimIndent()
                fragmentPreamble = """
            |$shaderLinePlaneIntersect
            |$shaderProjectOnPlane
            |$shaderSideOfPlane
            |$shaderGGX
            |$shaderVSM
            |$shaderNoRepetition
            """.trimMargin()
                this.suppressDefaultOutput = true
                this.vertexTransform = vs
                fragmentTransform = fs
                materialContext.pass.combiners.map {
                    if (rt.colorBuffers.size <= 1) {
                        this.output(it.targetOutput, 0)
                    } else
                        this.output(it.targetOutput, rt.colorBufferIndex(it.targetOutput))
                }
            }
        }
        return cached
    }

    private fun needLight(context: MaterialContext): Boolean {
        val needSpecular = context.pass.combiners.any { FacetType.SPECULAR in it.facets }
        val needDiffuse = context.pass.combiners.any { FacetType.DIFFUSE in it.facets }
        val needLight = needSpecular || needDiffuse
        return needLight
    }

    override fun applyToShadeStyle(context: MaterialContext, shadeStyle: ShadeStyle) {
        shadeStyle.parameter("emission", emission)
        shadeStyle.parameter("color", color)
        shadeStyle.parameter("metalness", metalness)
        shadeStyle.parameter("roughness", roughness)

        parameters.forEach { (k, v) ->
            when (v) {
                is Double -> shadeStyle.parameter(k, v)
                is Int -> shadeStyle.parameter(k, v)
                is Vector2 -> shadeStyle.parameter(k, v)
                is Vector3 -> shadeStyle.parameter(k, v)
                is Vector4 -> shadeStyle.parameter(k, v)
                is BufferTexture -> shadeStyle.parameter(k, v)
                is ColorBuffer -> shadeStyle.parameter(k, v)
                else -> TODO("support ${v::class.java}")
            }
        }
        if (needLight(context)) {
            textures.forEachIndexed { index, texture ->
                when (val source = texture.source) {
                    is TextureFromColorBuffer -> {
                        shadeStyle.parameter("texture$index", source.texture)
                        if (source.textureFunction == TextureFunction.NOT_TILING) {
                            shadeStyle.parameter("textureNoise", noise128)
                        }
                    }
                }
                when (val source = texture.source) {
                    is Triplanar -> {
                        shadeStyle.parameter("textureTriplanarSharpness$index", source.sharpness)
                        shadeStyle.parameter("textureTriplanarScale$index", source.scale)
                        shadeStyle.parameter("textureTriplanarOffset$index", source.offset)
                    }
                }
                if (texture.target is TextureTarget.Height) {
                    val target = texture.target as TextureTarget.Height
                    shadeStyle.parameter("textureHeightScale$index", target.scale)
                }

            }

            val lights = context.lights
            lights.forEachIndexed { index, (node, light) ->
                shadeStyle.parameter("lightColor$index", light.color)
                when (light) {
                    is AmbientLight -> {
                    }
                    is PointLight -> {
                        shadeStyle.parameter("lightPosition$index", (node.worldTransform * Vector4.UNIT_W).xyz)
                        shadeStyle.parameter("lightConstantAttenuation$index", light.constantAttenuation)
                        shadeStyle.parameter("lightLinearAttenuation$index", light.linearAttenuation)
                        shadeStyle.parameter("lightQuadraticAttenuation$index", light.quadraticAttenuation)
                    }

                    is SpotLight -> {
                        shadeStyle.parameter("lightPosition$index", (node.worldTransform * Vector4.UNIT_W).xyz)
                        shadeStyle.parameter("lightDirection$index", ((normalMatrix(node.worldTransform)) * light.direction).normalized)
                        shadeStyle.parameter("lightConstantAttenuation$index", light.constantAttenuation)
                        shadeStyle.parameter("lightLinearAttenuation$index", light.linearAttenuation)
                        shadeStyle.parameter("lightQuadraticAttenuation$index", light.quadraticAttenuation)
                        shadeStyle.parameter("lightInnerCos$index", cos(Math.toRadians(light.innerAngle)))
                        shadeStyle.parameter("lightOuterCos$index", cos(Math.toRadians(light.outerAngle)))

                        if (light.shadows is Shadows.MappedShadows) {
                            context.shadowMaps[light]?.let {
                                val look = light.view(node)
                                shadeStyle.parameter("lightTransform$index",
                                        light.projection(it) * look)

                                if (light.shadows is Shadows.DepthMappedShadows) {
                                    shadeStyle.parameter("lightShadowMap$index", it.depthBuffer ?: TODO())
                                }

                                if (light.shadows is Shadows.ColorMappedShadows) {
                                    shadeStyle.parameter("lightShadowMap$index", it.colorBuffer(0))
                                }
                            }
                        }
                    }
                    is DirectionalLight -> {
                        shadeStyle.parameter("lightPosition$index", (node.worldTransform * Vector4.UNIT_W).xyz)
                        shadeStyle.parameter("lightDirection$index", ((normalMatrix(node.worldTransform)) * light.direction).normalized)
                        if (light.shadows is Shadows.MappedShadows) {
                            context.shadowMaps[light]?.let {
                                val look = light.view(node)
                                shadeStyle.parameter("lightTransform$index",
                                        light.projection(it) * look)

                                if (light.shadows is Shadows.DepthMappedShadows) {
                                    shadeStyle.parameter("lightShadowMap$index", it.depthBuffer ?: TODO())
                                }

                                if (light.shadows is Shadows.ColorMappedShadows) {
                                    shadeStyle.parameter("lightShadowMap$index", it.colorBuffer(0))
                                }
                            }
                        }
                    }

                    is HemisphereLight -> {
                        shadeStyle.parameter("lightDirection$index", ((normalMatrix(node.worldTransform)) * light.direction).normalized)
                        shadeStyle.parameter("lightUpColor$index", light.upColor)
                        shadeStyle.parameter("lightDownColor$index", light.downColor)

                        light.irradianceMap?.let {
                            shadeStyle.parameter("lightIrradianceMap$index", it)
                        }
                    }
                }
            }
            context.fogs.forEachIndexed { index, (node, fog) ->
                shadeStyle.parameter("fogColor$index", fog.color)
                shadeStyle.parameter("fogEnd$index", fog.end)
            }
        } else {
            textures.forEachIndexed { index, texture ->
                if (texture.target is TextureTarget.Height) {
                    when (val source = texture.source) {
                        is TextureFromColorBuffer -> shadeStyle.parameter("texture$index", source.texture)
                    }
                    when (val source = texture.source) {
                        is Triplanar -> {
                            shadeStyle.parameter("textureTriplanarSharpness$index", source.sharpness)
                            shadeStyle.parameter("textureTriplanarScale$index", source.scale)
                            shadeStyle.parameter("textureTriplanarOffset$index", source.offset)
                        }
                    }
                    val target = texture.target as TextureTarget.Height
                    shadeStyle.parameter("textureHeightScale$index", target.scale)
                }
            }
        }
    }
}

