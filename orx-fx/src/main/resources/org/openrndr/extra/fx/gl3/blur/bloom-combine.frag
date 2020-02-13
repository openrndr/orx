#version 330 core

out vec4 o_output;
in vec2 v_texCoord0;

uniform sampler2D tex0;
uniform sampler2D tex1;

uniform float gain;
uniform vec4 bias;

void main() {
	o_output = texture(tex0, v_texCoord0) +  texture(tex1, v_texCoord0)*gain;
	o_output.a = clamp(o_output.a, 0.0, 1.0);
}