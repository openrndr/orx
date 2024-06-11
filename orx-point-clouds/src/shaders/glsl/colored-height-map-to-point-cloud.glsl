#version 430

layout (local_size_x = 8, local_size_y = 8) in;

uniform ivec2 resolution;
uniform vec2 floatResolution;
layout(rgba32f, binding = 0) uniform readonly image2D heightMap;
layout(rgba32f, binding = 1) uniform readonly image2D colors;
uniform float heightScale;
#ifdef PRESERVE_PROPORTIONS
uniform vec2 scale;
uniform vec2 offset;
#endif

struct Point {
    vec3 position;
    float size;
    vec4 color;
};

layout (std430, binding = 2) buffer pointCloud {
    Point points[];
};

void main() {
    ivec2 coord = ivec2(gl_GlobalInvocationID.xy);
    if (coord.x >= resolution.x || coord.y >= resolution.y) {
        return;
    }
    vec4 color = imageLoad(colors, coord);
    vec4 height = imageLoad(heightMap, coord);
    vec2 position = coord / floatResolution;
    #ifdef PRESERVE_PROPORTIONS
    position = position * scale + offset;
    #endif
    int index = coord.y * resolution.x + coord.x;
    points[index].position = vec3(
        position.x,
        position.y,
        height.r * heightScale
    );
    points[index].size = height.a; // the alpha channel is used to populate size attribute
    points[index].color = color;
}
