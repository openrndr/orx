#version 430 core

layout (triangles) in;
layout (triangle_strip, max_vertices = 3) out;

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
        gl_Position = gl_in[i].gl_Position;
        EmitVertex();
    }
    EndPrimitive();
}