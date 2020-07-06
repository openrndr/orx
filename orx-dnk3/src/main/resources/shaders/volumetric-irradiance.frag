#version 330 core

#pragma import org.openrndr.extra.shaderphrases.phrases.Depth.projectionToViewCoordinate;
#pragma import org.openrndr.extra.dnk3.cubemap.SphericalHarmonicsKt.glslFetchSH0;
#pragma import org.openrndr.extra.dnk3.cubemap.SphericalHarmonicsKt.glslGridCoordinates;
#pragma import org.openrndr.extra.dnk3.cubemap.SphericalHarmonicsKt.glslGridIndex;
#pragma import org.openrndr.extra.dnk3.cubemap.SphericalHarmonicsKt.glslGatherSH0;
#pragma import org.openrndr.extra.noise.phrases.NoisePhrasesKt.phraseHash22;
#pragma import org.openrndr.extra.noise.phrases.SimplexKt.phraseSimplex3;

in vec2 v_texCoord0;
uniform sampler2D tex0; // image
uniform sampler2D tex1; // projDepth

uniform samplerBuffer shMap;
uniform ivec3 shMapDimensions;
uniform vec3 shMapOffset;
uniform float shMapSpacing;

uniform mat4 projectionMatrixInverse;
uniform mat4 viewMatrixInverse;
uniform float stepLength;

out vec4 o_output;

void main() {
    vec3 inputColor = texture(tex0, v_texCoord0).rgb;
    float projDepth = texture(tex1, v_texCoord0).r;
    vec3 viewCoordinate = projectionToViewCoordinate(v_texCoord0, projDepth, projectionMatrixInverse);

    vec3 worldCoordinate = (viewMatrixInverse * vec4(viewCoordinate, 1.0)).xyz;
    vec3 cameraPosition = (viewMatrixInverse * vec4(vec3(0.0), 1.0)).xyz;

    // trace in world space
    vec3 traverse = cameraPosition - worldCoordinate;
    vec3 direction = normalize(traverse);
    if (length(traverse) > 10.0) {
        traverse = direction*10.0;
        worldCoordinate = cameraPosition - traverse;
    }

    int steps = min(100, int(length(traverse) / 0.1));
    vec3 step = traverse / steps;

    vec3 marchPosition = worldCoordinate;
    vec3 accumulated = inputColor;
    float jitter = hash22(v_texCoord0).x;
    marchPosition += jitter * step*0.5;
    for (int stepIndex = 0; stepIndex < steps; ++stepIndex) {
        float density = pow(abs(simplex31(marchPosition*0.25)), 4.0) * 0.1;
        vec3 sh0;
        gatherSH0(shMap, marchPosition, shMapDimensions, shMapOffset, shMapSpacing, sh0);
        accumulated = accumulated * (1.0-density) + sh0 * density;
        marchPosition += step;
    }
    o_output = vec4(accumulated, 1.0);
}
