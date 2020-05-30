package org.openrndr.extra.dnk3

val shaderNoRepetition = """
float sum( vec3 v ) { return v.x+v.y+v.z; }
    
// based on https://www.shadertoy.com/view/Xtl3zf 
vec4 textureNoTile(in sampler2D noiseTex, in sampler2D tex, in vec2 noiseOffset,  in vec2 x)
{
    float v = 1.0;
    float k = texture(noiseTex, noiseOffset + x*0.01 ).x; // cheap (cache friendly) lookup
    
    vec2 duvdx = dFdx( x );
    vec2 duvdy = dFdx( x );
    
    float l = k*8.0;
    float f = fract(l);
    
#if 0
    float ia = floor(l); // my method
    float ib = ia + 1.0;
#else
    float ia = floor(l+0.5); // suslik's method (see comments)
    float ib = floor(l);
    f = min(f, 1.0-f)*2.0;
#endif    
    
    vec2 offa = sin(vec2(3.0,7.0)*ia); // can replace with any other hash
    vec2 offb = sin(vec2(3.0,7.0)*ib); // can replace with any other hash

    vec3 cola = textureGrad( tex, x + v*offa, duvdx, duvdy ).xyz;
    vec3 colb = textureGrad( tex, x + v*offb, duvdx, duvdy ).xyz;
    
    return vec4(mix( cola, colb, smoothstep(0.2,0.8,f-0.1*sum(cola-colb)) ), 1.0);
}
"""

val shaderNoRepetitionVert = """
// shaderNoRepetitionVert
float sum( vec3 v ) { return v.x+v.y+v.z; }
    
// based on https://www.shadertoy.com/view/Xtl3zf 
vec4 textureNoTile(in sampler2D tex, in vec2 noiseOffset, in vec2 x)
{
    float v = 1.0;
    float k = texture(tex, noiseOffset + 0.005*x ).x; // cheap (cache friendly) lookup
    
    float l = k*8.0;
    float f = fract(l);
    
#if 0
    float ia = floor(l); // my method
    float ib = ia + 1.0;
#else
    float ia = floor(l+0.5); // suslik's method (see comments)
    float ib = floor(l);
    f = min(f, 1.0-f)*2.0;
#endif    
    
    vec2 offa = sin(vec2(3.0,7.0)*ia); // can replace with any other hash
    vec2 offb = sin(vec2(3.0,7.0)*ib); // can replace with any other hash

    vec3 cola = texture( tex, x + v*offa).xyz;
    vec3 colb = texture( tex, x + v*offb).xyz;
    
    return vec4(mix( cola, colb, smoothstep(0.2,0.8,f-0.1*sum(cola-colb)) ), 1.0);
}
"""

val shaderProjectOnPlane = """
// shaderProjectOnPlane
vec3 projectOnPlane(vec3 p, vec3 pc, vec3 pn) {
    float distance = dot(pn, p-pc);
    return p - distance * pn;
}
""".trimIndent()

val shaderSideOfPlane = """
int sideOfPlane(in vec3 p, in vec3 pc, in vec3 pn){
   if (dot(p-pc,pn) >= 0.0) return 1; else return 0;
}
""".trimIndent()

val shaderLinePlaneIntersect = """
vec3 linePlaneIntersect(in vec3 lp, in vec3 lv, in vec3 pc, in vec3 pn){
   return lp+lv*(dot(pn,pc-lp)/dot(pn,lv));
}
""".trimIndent()

val shaderVSM = """
|float linstep(float min, float max, float v)
|{
|  return clamp((v - min) / (max - min), 0, 1);
|}
|// https://developer.nvidia.com/gpugems/GPUGems3/gpugems3_ch08.html
|float chebyshevUpperBound(vec2 moments, float t, float minVariance) {
|   // One-tailed inequality valid if t > Moments.x
|   float p = (t <= moments.x) ? 1.0 : 0.0;
|   // Compute variance.
|   float variance = moments.y - (moments.x * moments.x);
|   variance = max(variance, minVariance);
|   // Compute probabilistic upper bound.
|   float d = t - moments.x;
|   float p_max = variance / (variance + d*d);
|   p_max = smoothstep(0.6, 1.0, p_max);
|   return max(p, p_max);
}
""".trimIndent()

/*
N - world space normal
V - eye - world vertex position
L - world light pos - world vertex position
 */
val shaderGGX = """
#define bias 0.125
#define HASHSCALE 443.8975
vec2 hash22(vec2 p) {
	vec3 p3 = fract(vec3(p.xyx) * HASHSCALE);
    p3 += dot(p3, p3.yzx+19.19);
    return fract(vec2((p3.x + p3.y)*p3.z, (p3.x+p3.z)*p3.y));
}

#define PI 3.1415926535

float pow5(float x) {
    float x2 = x * x;
    return x2 * x2 * x;
}

float D_GGX(float linearRoughness, float NoH, const vec3 h) {
    // Walter et al. 2007, "Microfacet Models for Refraction through Rough Surfaces"
    float oneMinusNoHSquared = 1.0 - NoH * NoH;
    float a = NoH * linearRoughness;
    float k = linearRoughness / (oneMinusNoHSquared + a * a);
    float d = k * k * (1.0 / PI);
    return d;
}

float D_GGXm(float linearRoughness, float NoH, const vec3 h, const vec3 n) {
    vec3 NxH = cross(n, h);
        float oneMinusNoHSquared = dot(NxH, NxH);


    // Walter et al. 2007, "Microfacet Models for Refraction through Rough Surfaces"
    //float oneMinusNoHSquared = 1.0 - NoH * NoH;
    float a = NoH * linearRoughness;
    float k = linearRoughness / (oneMinusNoHSquared + a * a);
    float d = k * k * (1.0 / PI);
    return d;
}


float V_SmithGGXCorrelated(float linearRoughness, float NoV, float NoL) {
    // Heitz 2014, "Understanding the Masking-Shadowing Function in Microfacet-Based BRDFs"
    float a2 = linearRoughness * linearRoughness;
    float GGXV = NoL * sqrt((NoV - a2 * NoV) * NoV + a2);
    float GGXL = NoV * sqrt((NoL - a2 * NoL) * NoL + a2);
    return 0.5 / (GGXV + GGXL);
}

vec3 F_Schlick(const vec3 f0, float VoH) {
    // Schlick 1994, "An Inexpensive BRDF Model for Physically-Based Rendering"
    return f0 + (vec3(1.0) - f0) * pow5(1.0 - VoH);
}

float F_Schlick(float f0, float f90, float VoH) {
    return f0 + (f90 - f0) * pow5(1.0 - VoH);
}

float Fd_Burley(float linearRoughness, float NoV, float NoL, float LoH) {
    // Burley 2012, "Physically-Based Shading at Disney"
    float f90 = 0.5 + 2.0 * linearRoughness * LoH * LoH;
    float lightScatter = F_Schlick(1.0, f90, NoL);
    float viewScatter  = F_Schlick(1.0, f90, NoV);
    return lightScatter * viewScatter * (1.0 / PI);
}

vec2 PrefilteredDFG_Karis(float roughness, float NoV) {
    //https://www.shadertoy.com/view/XlKSDR
    // Karis 2014, "Physically Based Material on Mobile"
    const vec4 c0 = vec4(-1.0, -0.0275, -0.572,  0.022);
    const vec4 c1 = vec4( 1.0,  0.0425,  1.040, -0.040);

    vec4 r = roughness * c0 + c1;
    float a004 = min(r.x * r.x, exp2(-9.28 * NoV)) * r.x + r.y;
    return vec2(-1.04, 1.04) * a004 + r.zw;
}

float saturate(float x) {
    return clamp(x, 0.0, 1.0);
}

float G1V(float dotNV, float k)
{
	return 1.0f/(dotNV*(1.0f-k)+k);
}

float ggx(vec3 N, vec3 V, vec3 L, float roughness, float F0)
{
	float alpha = roughness*roughness;

	vec3 H = normalize(V+L);

	float dotNL = saturate(dot(N,L));
	float dotNV = saturate(dot(N,V));
	float dotNH = saturate(dot(N,H));
	float dotLH = saturate(dot(L,H));

	float F, D, vis;

	// D
	float alphaSqr = alpha*alpha;
	float pi = 3.14159f;
	float denom = dotNH * dotNH *(alphaSqr-1.0) + 1.0f;
	D = alphaSqr/(pi * denom * denom);

	// F
	float dotLH5 = pow(1.0f-dotLH,5);
	F = F0 + (1.0-F0)*(dotLH5);

	// V
	float k = alpha/2.0f;
	vis = G1V(dotNL,k)*G1V(dotNV,k);

	float specular = dotNL * D * F * vis;
	return specular;
}
""".trimIndent()