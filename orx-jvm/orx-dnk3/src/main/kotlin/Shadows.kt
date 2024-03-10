package org.openrndr.extra.dnk3

import org.openrndr.draw.RenderTarget
import org.openrndr.math.Matrix44

sealed class Shadows {
    object None : Shadows()
    abstract class MappedShadows(val mapSize: Int) : Shadows()
    abstract class DepthMappedShadows(mapSize: Int) : MappedShadows(mapSize)
    abstract class ColorMappedShadows(mapSize: Int) : MappedShadows(mapSize)
    class Simple(mapSize: Int = 1024) : DepthMappedShadows(mapSize)
    class PCF(mapSize: Int = 1024, val sampleCount: Int = 12) : DepthMappedShadows(mapSize)
    class VSM(mapSize: Int = 1024) : ColorMappedShadows(mapSize)
}

interface ShadowLight {
    var shadows: Shadows
    fun projection(renderTarget: RenderTarget): Matrix44
    fun view(node: SceneNode): Matrix44 {
        return node.worldTransform.inversed
    }
}

// shaders

fun Shadows.VSM.fs(index: Int) : String = """
|{
|   vec4 smc = (p_lightTransform$index * vec4(v_worldPosition,1.0));
|   vec3 lightProj = (smc.xyz/smc.w) * 0.5 + 0.5;
|   if (lightProj.x > 0.0 && lightProj.x < 1.0 && lightProj.y > 0.0 && lightProj.y < 1.0) {
|       vec2 moments = texture(p_lightShadowMap$index, lightProj.xy).xy;
|       attenuation *= (chebyshevUpperBound(moments, length(Lr), 50.0));
|   }
|}
""".trimMargin()

fun Shadows.Simple.fs(index: Int): String = """
|{
|   vec4 smc = (p_lightTransform$index * vec4(v_worldPosition,1.0));
|   vec3 lightProj = (smc.xyz/smc.w) * 0.5 + 0.5;
|   if (lightProj.x > 0.0 && lightProj.x < 1.0 && lightProj.y > 0.0 && lightProj.y < 1.0) {
|       vec3 smz = texture(p_lightShadowMap$index, lightProj.xy).rgb;
|       vec2 step = 1.0 / vec2(textureSize(p_lightShadowMap$index,0));
|       float result = 0.0;
|       float compToZ = (lightProj.z- 0.0020 * tan(acos(NoL))) - 0.0003;
|       float currentDepth = lightProj.z;
|       float closestDepth = smz.x;
|       float shadow = (currentDepth - 0.0020 * tan(acos(NoL))) - 0.0003  >= closestDepth  ? 0.0 : 1.0;
|       attenuation *= shadow;
|   }
|}
""".trimMargin()

fun Shadows.PCF.fs(index: Int): String = """
|{
|   float lrl = length(Lr)/100.0;
|   vec2 fTaps_Poisson[12];
|   fTaps_Poisson[0]  = vec2(-.326,-.406);
|   fTaps_Poisson[1]  = vec2(-.840,-.074);
|   fTaps_Poisson[2]  = vec2(-.696, .457);
|	fTaps_Poisson[3]  = vec2(-.203, .621);
|	fTaps_Poisson[4]  = vec2( .962,-.195);
|	fTaps_Poisson[5]  = vec2( .473,-.480);
|	fTaps_Poisson[6]  = vec2( .519, .767);
|	fTaps_Poisson[7]  = vec2( .185,-.893);
|	fTaps_Poisson[8]  = vec2( .507, .064);
|	fTaps_Poisson[9]  = vec2( .896, .412);
|	fTaps_Poisson[10] = vec2(-.322,-.933);
|	fTaps_Poisson[11] = vec2(-.792,-.598);
|   vec4 smc = (p_lightTransform$index * vec4(v_worldPosition,1.0));
|   vec3 lightProj = (smc.xyz/smc.w) * 0.5 + 0.5;
|   if (lightProj.x > 0.0 && lightProj.x < 1.0 && lightProj.y > 0.0 && lightProj.y < 1.0) {
|       vec3 smz = texture(p_lightShadowMap$index, lightProj.xy).rgb;
|       vec2 stepSize = 1.0 / vec2(textureSize(p_lightShadowMap$index,0));
|       float result = 0.0;
|       float compToZ = (lightProj.z- 0.0020 * tan(acos(NoL))) - 0.0003;
|       float noise = hash22(lightProj.xy*10.0).x;
|       float r = noise * 3.1415926535 * 2.0;
|       mat2 rot = mat2( vec2(cos(r), -sin(r)), vec2(sin(r),cos(r)));
|       for (int i = 0; i < 12; ++i) {
|           float depth = texture(p_lightShadowMap$index, lightProj.xy + rot*fTaps_Poisson[i]*float(i)*lrl*stepSize ).r;
|           result += step(compToZ, depth);
|       }
|       result /= 12.0;
|       float currentDepth = lightProj.z;
|       float closestDepth = smz.x;
|       float shadow = result;// (currentDepth - 0.0020 * tan(acos(NoL))) - 0.0003  >= closestDepth  ? 0.0 : 1.0;
|       attenuation *= shadow;
|   }
|}
""".trimMargin()

fun Shadows.fs(index: Int): String = when (this) {
    is Shadows.PCF -> this.fs(index)
    is Shadows.Simple -> this.fs(index)
    is Shadows.VSM -> this.fs(index)
    is Shadows.None -> ""
    else -> TODO()
}
