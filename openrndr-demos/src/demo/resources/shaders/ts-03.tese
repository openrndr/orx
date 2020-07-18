#version 430 core

vec3 bezier2(vec3 a, vec3 b, float t) {
    return mix(a, b, t);
}
vec3 bezier3(vec3 a, vec3 b, vec3 c, float t) {
    return mix(bezier2(a, b, t), bezier2(b, c, t), t);
}
vec3 bezier4(vec3 a, vec3 b, vec3 c, vec3 d, float t) {
    return mix(bezier3(a, b, c, t), bezier3(b, c, d, t), t);
}

struct Vertex {
    vec3 va_position;
    vec3 va_normal;
    vec4 v_addedProperty;
};

layout(isolines) in;
in vec3 cva_position[];

out vec3 derivative;
out vec3 position;

uniform int resolution;

uniform mat4 proj;
uniform mat4 view;
uniform mat4 model;

void main() {
    float t = gl_TessCoord.x;
    vec3 ePos = bezier4(
    cva_position[0],
    cva_position[1],
    cva_position[2],
    cva_position[3],
    t);

    // calculate derivative using Hodograph
    derivative = bezier3(cva_position[1] - cva_position[0], cva_position[2]-cva_position[1], cva_position[3]-cva_position[2], t);

    // output model space positions
    position = ePos;

    float r = resolution + 1.0;
    //gl_Position = proj * view * model * vec4(ePos, 1);
}
