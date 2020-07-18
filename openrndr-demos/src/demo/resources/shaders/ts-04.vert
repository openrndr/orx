#version 430 core

#pragma import org.openrndr.extra.noise.phrases.SimplexKt.phraseSimplex3;

in vec3 a_position;

out vec3 va_position;

uniform mat4 view;
uniform mat4 proj;
uniform mat4 model;

uniform float time;

void main() {
    va_position = a_position; //4.0* vec3(simplex31(a_position + vec3(time, time, -time) ), 4.0*simplex31(a_position.zxy + vec3(-time, time, time)), 4.0*simplex31(a_position.yzx + vec3(time, -time, time))) ;
}