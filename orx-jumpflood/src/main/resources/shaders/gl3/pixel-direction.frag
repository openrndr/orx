#version 330 core

uniform sampler2D tex0;
uniform sampler2D tex1;
uniform vec2 originalSize;
uniform vec2 directionalField;
uniform float distanceScale;
in vec2 v_texCoord0;

out vec4 o_color;

void main() {
    vec2 sizeDF = textureSize(tex0, 0); // this is always square
    vec2 sizeTF = textureSize(tex1, 0); // this can be non-square

    vec2 pixelPosition = v_texCoord0;
    vec2 centroidPixelPosition = texture(tex0, v_texCoord0).xy;
    vec2 pixelDistance = (centroidPixelPosition - pixelPosition) * sizeDF * vec2(1.0, -1.0);
    vec2 dfTf = sizeDF / sizeTF; // texture adjusment factor
    float threshold = texture(tex1, v_texCoord0 * dfTf).r;

    o_color = vec4(pixelDistance * distanceScale, threshold, 1.0);
}