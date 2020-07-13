#version 430 core

layout (lines) in;
layout (line_strip, max_vertices = 2) out;

in InVertex {
    vec3 va_position;
    vec3 va_normal;
    vec4 v_addedProperty;
} vertices[];

out vec3 va_position;
out vec3 va_normal;
out vec4 v_addedProperty;


uniform vec3 offset;

void main() {
    int i;
    for(i = 0;i < gl_in.length();i++) {
        v_addedProperty = vertices[i].v_addedProperty;
        va_normal = vertices[i].va_normal;
        va_position = vertices[i].va_position;
        gl_Position = gl_in[i].gl_Position;
        EmitVertex();
    }
    EndPrimitive();
}