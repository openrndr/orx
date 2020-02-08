#version 330 core

in vec2 v_texCoord0;
uniform sampler2D tex0; // input
uniform vec4 foreground;
uniform vec4 background;
uniform float foregroundOpacity;
uniform float backgroundOpacity;

out vec4 o_color;
void main() {
    vec4 c = texture(tex0, v_texCoord0);
    vec4 fgc = foreground * foregroundOpacity;
    vec4 bgc = background * backgroundOpacity;
    float luma = dot( (c.a> 0.0? c.rgb/c.a : vec3(0.0)), vec3(1.0/3.0));
    o_color = mix(bgc, fgc, luma) * c.a;
}
