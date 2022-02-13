// based on Hashed blur by David Hoskins.
// License Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License.

#ifdef OR_IN_OUT
in vec2 v_texCoord0;
#else
varying vec2 v_texCoord0;
#endif

uniform sampler2D tex0;
uniform vec2 textureSize0;
uniform float radius;
uniform float time;
uniform int samples;
uniform float gain;

#ifndef OR_GL_FRAGCOLOR
out vec4 o_color;
#endif

#define TAU 6.28318530718

//-------------------------------------------------------------------------------------------
#define HASHSCALE 443.8975
vec2 hash22(vec2 p) {
	vec3 p3 = fract(vec3(p.xyx) * HASHSCALE);
	p3 += dot(p3, p3.yzx+19.19);
	return fract(vec2((p3.x + p3.y)*p3.z, (p3.x+p3.z)*p3.y));
}

vec2 sampleTexture(inout vec2 r) {
	r = fract(r * vec2(33.3983, 43.4427));
	//return r-.5;
	return sqrt(r.x+.001) * vec2(sin(r.y * TAU), cos(r.y * TAU))*.5; // <<=== circular sampling.
}


//-------------------------------------------------------------------------------------------
vec4 blur(vec2 uv, float radius) {
	vec2 circle = vec2(radius) * (vec2(1.0) / textureSize0);
	vec2 random = hash22(uv + vec2(time));

	vec4 acc = vec4(0.0);

	for (int i = 0; i < 100; i++) {
		if (i > samples) break;
		#ifndef OR_GL_TEXTURE2D
		acc += texture(tex0, uv + circle * sampleTexture(random));
		#else
		acc += texture2D(tex0, uv + circle * sampleTexture(random));
		#endif
	}
	return acc / float(samples);
}

//-------------------------------------------------------------------------------------------
void main() {
	vec2 uv = v_texCoord0;
	float radiusSqr = pow(radius, 2.0);

	vec4 result = blur(uv, radiusSqr);
	result.rgb *= gain;

	#ifdef OR_GL_FRAGCOLOR
	gl_FragColor = result;
	#else
	o_color = result;
	#endif
}