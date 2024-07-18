#version 430

layout (local_size_x = 8, local_size_y = 8) in;

uniform ivec2 resolution;
uniform ivec2 resolutionMinus1;
uniform vec3 eye;

struct Point {
    vec3 position;
    float size;
    #ifdef COLORED
    vec4 color;
    #endif
};

struct Vertex {
    vec3 position;
    vec3 normal;
    #ifdef COLORED
    vec4 color;
    #endif
};

// input buffer for world positions
layout (binding = 0) buffer pointCloud {
    Point points[];
};

// output buffer for vertex positions
layout (binding = 1) buffer mesh {
    Vertex vertices[];
};

vec3 calculateNormal(in vec3 p0, in vec3 p1, in vec3 p2) {
    vec3 edge1 = p1 - p0;
    vec3 edge2 = p2 - p0;
    return normalize(cross(edge1, edge2));
}

void main() {
    const ivec2 coord = ivec2(gl_GlobalInvocationID.xy);
    if ((coord.x >= resolutionMinus1.x) || (coord.y >= resolutionMinus1.y)) {
        return;
    }
    int pointBase = coord.x + coord.y * resolution.x;

    Point p0 = points[pointBase];
    Point p1 = points[pointBase + 1];
    Point p2 = points[pointBase + 1 + resolution.x];
    Point p3 = points[pointBase + resolution.x];

    const int base = (coord.y * resolutionMinus1.x + coord.x) * 6;

    vec3 normal1 = calculateNormal(p0.position, p1.position, p3.position);

    vertices[base + 0].position = p0.position;
    vertices[base + 0].normal = normal1;
    #ifdef COLORED
    vertices[base + 0].color = p0.color;
    #endif
    vertices[base + 1].position = p1.position;
    vertices[base + 1].normal = normal1;
    #ifdef COLORED
    vertices[base + 1].color = p1.color;
    #endif
    vertices[base + 2].position = p3.position;
    vertices[base + 2].normal = normal1;
    #ifdef COLORED
    vertices[base + 2].color = p3.color;
    #endif

    vec3 normal2 = calculateNormal(p1.position, p2.position, p3.position);

    vertices[base + 3].position = p1.position;
    vertices[base + 3].normal = normal2;
    #ifdef COLORED
    vertices[base + 3].color = p1.color;
    #endif
    vertices[base + 4].position = p2.position;
    vertices[base + 4].normal = normal2;
    #ifdef COLORED
    vertices[base + 4].color = p2.color;
    #endif
    vertices[base + 5].position = p3.position;
    vertices[base + 5].normal = normal2;
    #ifdef COLORED
    vertices[base + 5].color = p3.color;
    #endif
}
