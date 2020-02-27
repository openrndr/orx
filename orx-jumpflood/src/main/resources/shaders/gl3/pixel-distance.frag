#version 330 core

uniform sampler2D tex0;
uniform sampler2D tex1;

uniform vec2 originalSize;
uniform float distanceScale;
uniform bool signedBit;

in vec2 v_texCoord0;

out vec4 o_color;

void main() {
    vec2 size = textureSize(tex0, 0);
    vec2 fixUp = v_texCoord0;



    vec2 pixelPosition = fixUp;
    vec2 centroidPixelPosition = texture(tex0, v_texCoord0).xy;
    vec2 pixelDistance = (centroidPixelPosition - pixelPosition) * size * vec2(1.0, -1.0);
    float threshold = texture(tex1, v_texCoord0).r;
    if (signedBit) {
        o_color = vec4(length(pixelDistance)* distanceScale, threshold, 0.0, 1.0);
    }  else {
        o_color = vec4(vec3(length(pixelDistance) * distanceScale), 1.0);
    }
}