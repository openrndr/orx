#version 410 core

in vec3 a_position;
in vec3 a_normal;
in vec2 a_texCoord0;

out InVertex {
    vec3 va_position;
    vec3 va_normal;
    vec4 v_addedProperty;
} vertexOut;

uniform mat4 view;
uniform mat4 proj;
uniform mat4 model;

void main() {
    vertexOut.v_addedProperty = vec4(1.0, 0.0, 0.0, 1.0);
    vertexOut.va_position = a_position;
    vertexOut.va_normal = a_normal;
    gl_Position = proj * view * model * vec4(a_position, 1.0);
}