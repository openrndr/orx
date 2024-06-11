#version 430

layout (local_size_x = 8, local_size_y = 8) in;

uniform ivec2 resolution;
uniform ivec2 resolutionMinus1;

struct Point {
    vec3 position;
    float size;
    #ifdef COLORED
    vec4 color;
    #endif
};

struct Line {
    Point start;
    Point end;
};

layout (std430, binding = 1) buffer pointCloud {
    Point points[];
};

layout (std430, binding = 2) buffer wireframe {
    Line lines[];
};

void main() {
    ivec2 coord = ivec2(gl_GlobalInvocationID.xy);
    if (coord.x >= (resolutionMinus1.x) || coord.y >= (resolutionMinus1.y)) {
        return;
    }

    int pointBase = coord.x + coord.y * resolution.x;

    Point p0 = points[pointBase];
    Point p1 = points[pointBase + 1];
    Point p2 = points[pointBase + resolution.x + 1];
    Point p3 = points[pointBase + resolution.x];

    int base = (coord.y * resolutionMinus1.x + coord.x) * 4;

    lines[base + 0].start = p0;
    lines[base + 0].end = p1;

    lines[base + 1].start = p1;
    lines[base + 1].end = p2;

    lines[base + 2].start = p2;
    lines[base + 2].end = p3;

    lines[base + 3].start = p3;
    lines[base + 3].end = p0;
}
