#pragma package org.openrndr.extra.shaderphrases.phrases
mat3 tbnMatrix(vec4 tangent, vec3 normal) {
    vec3 bitangent = cross(normal, tangent.xyz) * tangent.w;
    return mat3(tangent.xyz, bitangent, normal);
}