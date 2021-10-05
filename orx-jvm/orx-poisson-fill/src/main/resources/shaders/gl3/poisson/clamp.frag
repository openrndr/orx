// from https://github.com/kosua20/Rendu/blob/master/resources/common/shaders/screens/convolution-pyramid/fill-boundary.frag

#version 330

in vec2 v_texCoord0;
uniform sampler2D tex0;
uniform int levels;
out vec4 o_output;
uniform float scale;
uniform float phase;

uniform float minValue;
uniform float maxValue;

void main(){

    vec4 c = texture(tex0, v_texCoord0);

    c.rgb = clamp(c.rgb, vec3(minValue), vec3(maxValue));

    o_output = c;



}