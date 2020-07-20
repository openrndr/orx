#version 410 core

in vec3 a_position;


out vec3 va_position;

uniform mat4 view;
uniform mat4 proj;
uniform mat4 model;

void main() {
    va_position = a_position;
    gl_Position = proj * view * model * vec4(a_position, 1.0);
}