#version 430 core

out vec4 o_color;

in vec3 va_position;
in vec3 va_normal;
in vec4 v_addedProperty;

void main() {
    o_color = v_addedProperty;
}