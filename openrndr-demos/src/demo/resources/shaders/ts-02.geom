#version 410 core

layout (lines) in;
layout (line_strip, max_vertices = 2) out;


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