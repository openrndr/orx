#version 330

uniform sampler2D tex0;
out     vec3 color;

void main() {
    float depth = texelFetch(tex0, ivec2(int(gl_FragCoord.x), int(gl_FragCoord.y)), 0).r;
    color = (depth >= .999) ? vec3(0) : vec3(depth);
}
