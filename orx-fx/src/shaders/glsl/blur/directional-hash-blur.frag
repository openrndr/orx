// based on Hashed blur by David Hoskins.
// License Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License.

in vec2 v_texCoord0;

layout(binding = 0) uniform sampler2D tex0;
layout(binding = 1) uniform sampler2D tex1;

#ifdef RADIUS_FROM_TEXTURE
layout(binding = 2) uniform sampler2D tex2;
#endif


uniform vec2 textureSize0;
uniform float radius;
uniform float spread;

uniform float time;
uniform int samples;
uniform float gain;

out vec4 o_color;

#define TAU 6.28318530718

//-------------------------------------------------------------------------------------------
#define HASHSCALE 443.8975
vec2 hash22(vec2 p) {
	vec3 p3 = fract(vec3(p.xyx) * HASHSCALE);
	p3 += dot(p3, p3.yzx+19.19);
	return fract(vec2((p3.x + p3.y)*p3.z, (p3.x+p3.z)*p3.y));
}


vec2 sampleOffset(inout vec2 r, vec2 direction) {
	r = fract(r * vec2(33.3983, 43.4427));
	return (r.x+.001) * direction;
}

vec2 sampleCircle(inout vec2 r) {
	r = fract(r * vec2(33.3983, 43.4427));
	return sqrt(r.x+.001) * vec2(sin(r.y * TAU), cos(r.y * TAU))*.5; // <<=== circular sampling.
}


//-------------------------------------------------------------------------------------------
vec4 blur(vec2 uv, float r) {
	float radius = r;
	#ifdef RADIUS_FROM_TEXTURE
	radius *= texture(tex2, uv).r;
	#endif
	vec2 direction =  texture(tex1, uv).xy;

	vec2 line = vec2(spread) * (vec2(1.0) / textureSize0);
	vec2 circle = vec2(radius) * (vec2(1.0) / textureSize0);
	vec2 randomL = hash22(uv + vec2(time));
	vec2 randomC = hash22(uv + vec2(time));

	vec4 acc = vec4(0.0);

	for (int i = 0; i < samples; i++) {
		vec2 lineOffset = line * sampleOffset(randomL, direction);
		vec2 circleOffset = circle * sampleCircle(randomC);
		acc += textureLod(tex0, uv + circleOffset + lineOffset, 0 );
	}
	return acc / float(samples);
}

//-------------------------------------------------------------------------------------------
void main() {
	vec2 uv = v_texCoord0;
	float radiusSqr = pow(radius, 2.0);

	vec4 result = blur(uv, radiusSqr);
	result.rgb *= gain;


	o_color = result;
}