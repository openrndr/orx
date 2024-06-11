#version 430

layout (local_size_x = 8, local_size_y = 8) in;

uniform ivec2 resolution;
uniform vec2 floatResolution;
layout(rgba32f, binding = 0) uniform readonly image2D heightMap;
uniform float heightScale;
#ifdef PRESERVE_PROPORTIONS
uniform vec2 scale;
uniform vec2 offset;
#endif

layout (std430, binding = 1) buffer pointCloud {
    vec4 points[];
};

void main() {
    ivec2 coord = ivec2(gl_GlobalInvocationID.xy);
    if (coord.x >= resolution.x || coord.y >= resolution.y) {
        return;
    }
    vec4 height = imageLoad(heightMap, coord);
    vec2 position = coord / floatResolution;
    #ifdef PRESERVE_PROPORTIONS
    position = position * scale + offset;
    #endif
    int index = coord.y * resolution.x + coord.x;
    points[index] = vec4(
        position.x,
        position.y,
        height.r * heightScale,
        height.a // the alpha channel is used to populate size attribute
    );
}
