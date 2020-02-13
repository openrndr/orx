#version 330

float nrand(vec2 n) {
	return fract(sin(dot(n.xy, vec2(12.9898, 78.233))) * 43758.5453);
}

// -- based on https://github.com/excess-demogroup/even-laster-engine/blob/a451a89f6bd6d3c6017d5890b92d9f72823bc742/src/shaders/bloom_upscale.frag
uniform float seed;
uniform float shape;
uniform float gain;

in vec2 v_texCoord0;
out vec4 o_output;

uniform sampler2D tex0;
uniform sampler2D tex1;
uniform sampler2D tex2;
uniform sampler2D tex3;
uniform sampler2D tex4;
uniform sampler2D tex5;

vec4 sampleBloom(vec2 pos, float shape) {
	vec4 sum = vec4(0);
	float total = 0;

	{
		float weight = pow(0.0, shape);
		vec2 rnd = vec2(nrand(3 + 0.0 + pos.xy + seed),
		                nrand(5 + 0.0 + pos.yx - seed));
		rnd = (rnd * 2 - 1) / textureSize(tex0, 0);
		sum += textureLod(tex0, pos + rnd * 0.25, 0.0) * weight;
		total += weight;
	}
	{
        float weight = pow(1.0, shape);
        vec2 rnd = vec2(nrand(3 + 0.0 + pos.xy + seed),
                        nrand(5 + 0.0 + pos.yx - seed));
        rnd = (rnd * 2 - 1) / textureSize(tex1, 0);
        sum += textureLod(tex1, pos + rnd * 0.25, 0.0) * weight;
        total += weight;
    }
    {
        float weight = pow(2.0, shape);
        vec2 rnd = vec2(nrand(3 + 0.0 + pos.xy + seed),
    		                nrand(5 + 0.0 + pos.yx - seed));
        rnd = (rnd * 2 - 1) / textureSize(tex2, 0);
        sum += textureLod(tex2, pos + rnd * 0.25, 0.0) * weight;
        total += weight;
    }

    {
        float weight = pow(3.0, shape);
        vec2 rnd = vec2(nrand(3 + 0.0 + pos.xy + seed),
    		                nrand(5 + 0.0 + pos.yx - seed));
        rnd = (rnd * 3 - 1) / textureSize(tex3, 0);
        sum += textureLod(tex3, pos + rnd * 0.25, 0.0) * weight;
        total += weight;
    }
    {
        float weight = pow(4.0, shape);
        vec2 rnd = vec2(nrand(3 + 0.0 + pos.xy + seed),
    		                nrand(5 + 0.0 + pos.yx - seed));
        rnd = (rnd * 3 - 1) / textureSize(tex3, 0);
        sum += textureLod(tex4, pos + rnd * 0.25, 0.0) * weight;
        total += weight;
    }
    {
        float weight = pow(5.0, shape);
        vec2 rnd = vec2(nrand(3 + 0.0 + pos.xy + seed),
    		                nrand(5 + 0.0 + pos.yx - seed));
        rnd = (rnd * 3 - 1) / textureSize(tex3, 0);
        sum += textureLod(tex5, pos + rnd * 0.25, 0.0) * weight;
        total += weight;
    }

	return sum / total;
}

void main() {
	o_output = sampleBloom(v_texCoord0, shape) * gain;
}