#version 430

layout (local_size_x = 8, local_size_y = 8) in;

uniform ivec2 resolution;
uniform vec2 floatResolution;

layout(rgba8, binding = 0) uniform readonly image2D heightMap;
#ifdef COLORED
layout(rgba8, binding = 1) uniform readonly image2D colors;
#endif

uniform float heightScale;

#ifdef PRESERVE_PROPORTIONS
uniform vec2 scale;
uniform vec2 offset;
#endif

struct Point {
    vec3 position;
    float size;
    #ifdef COLORED
    vec4 color;
    #endif
};

layout(binding = 2) buffer pointCloud {
    Point points[];
};

void main() {
    ivec2 coord = ivec2(gl_GlobalInvocationID.xy);
    if (coord.x >= resolution.x || coord.y >= resolution.y) {
        return;
    }
    vec4 height = imageLoad(heightMap, coord);
    vec2 floatCoord = vec2(coord);
    #ifndef PRESERVE_PROPORTIONS
    floatCoord += vec2(.5);
    #endif
    vec2 position = floatCoord / floatResolution;
    #ifdef PRESERVE_PROPORTIONS
    position = position * scale + offset;
    #endif
    int index = coord.y * resolution.x + coord.x;
    points[index].position = vec3(
        position.x,
        position.y,
        height * heightScale.r
    );
    points[index].size = height.a; // the alpha channel is used to populate size attribute
    #ifdef COLORED
    points[index].color = imageLoad(colors, coord);
    #endif
}
