#version 330

uniform sampler2D tex0;             // kinect raw
uniform vec2      resolution;       // kinect resolution
uniform float     depthScale;       // 32 for kinect1, 64 for kinect2
uniform bool      mirror;

out     float depth;

void main() {
    ivec2 uv = ivec2(
        mirror ? int(resolution.x) - 1 - int(gl_FragCoord.x) : int(gl_FragCoord.x),
        int(resolution.y) - 1 - int(gl_FragCoord.y)
    );
    depth = texelFetch(tex0, uv, 0).r * depthScale;
}
