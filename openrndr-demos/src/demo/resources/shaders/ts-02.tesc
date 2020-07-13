#version 430 core

layout(vertices = 4) out; // 4 points per patch



in vec3 va_position[];
out vec3 cva_position[];

void main() {
    cva_position[gl_InvocationID] = va_position[gl_InvocationID];
    if(gl_InvocationID == 0) { // levels only need to be set once per patch
        gl_TessLevelOuter[0] = 1; // we're only tessellating one line
        gl_TessLevelOuter[1] = 4; // tessellate the line into 100 segments
    }
}