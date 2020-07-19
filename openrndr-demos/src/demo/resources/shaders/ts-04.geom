#version 430 core

layout (lines) in;
layout (triangle_strip, max_vertices = 4) out;
//
//in InVertex {
//    vec3 va_position;
//    vec3 va_normal;
//    vec4 v_addedProperty;
//} vertices[];

out vec3 va_position;
out vec3 va_normal;
out vec4 v_addedProperty;

uniform vec3 offset;
in vec3 derivative[];
in vec3 position[];

uniform mat4 proj;
uniform mat4 view;
uniform mat4 model;
uniform float weight;




void main() {

    mat4 pvm = proj * view * model;

    //    vec2 direction0 = normalize(derivative[0].xy);
    //    vec4 perp0 = vec4(direction0.y, -direction0.x, 0.0, 0.0);
    //
    //    vec2 direction1 = normalize(derivative[1].xy);
    //    vec4 perp1 = vec4(direction1.y, -direction1.x, 0.0, 0.0);

    vec4 p00 = (pvm * vec4(position[0], 1.0));
    //p00 /= p00.w;
    vec4 p01 = (pvm * vec4(position[0] + derivative[0], 1.0));
    //p01 /= p01.w;
    vec4 p10 = (pvm * vec4(position[1], 1.0));
    //p10 /= p10.w;
    vec4 p11 = (pvm * vec4(position[1] + derivative[1], 1.0));
    //p11 /= p11.w;

    vec2 direction0 = normalize(p01.xy - p00.xy);
    vec2 direction1 = normalize(p11.xy - p10.xy);
    vec4 perp0 = vec4(direction0.y, -direction0.x, 0.0, 0.0);
    vec4 perp1 = vec4(direction1.y, -direction1.x, 0.0, 0.0);

//    v_addedProperty = vertices[0].v_addedProperty;
//    va_normal = vertices[0].va_normal;
//    va_position = vertices[0].va_position;
    gl_Position = pvm * vec4(vec4(position[0], 1.0)) + perp0 * weight * 0.01;
    EmitVertex();

//    v_addedProperty = vertices[0].v_addedProperty;
//    va_normal = vertices[0].va_normal;
//    va_position = vertices[0].va_position;
    gl_Position = pvm * vec4(vec4(position[0], 1.0)) - perp0 * weight * 0.01;
    EmitVertex();

//    v_addedProperty = vertices[1].v_addedProperty;
//    va_normal = vertices[1].va_normal;
//    va_position = vertices[1].va_position;
    gl_Position = pvm * vec4(vec4(position[1], 1.0)) + perp1 * weight * 0.01;
    EmitVertex();

//    v_addedProperty = vertices[1].v_addedProperty;
//    va_normal = vertices[1].va_normal;
//    va_position = vertices[1].va_position;
    gl_Position = pvm * vec4(vec4(position[1], 1.0)) - perp1 * weight * 0.01;
    EmitVertex();

    EndPrimitive();
}