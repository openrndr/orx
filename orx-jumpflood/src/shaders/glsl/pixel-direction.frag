/*
use #define OUTPUT_DISTANCE to output distance
use #define OUTPUT_DIRECTION to output direction
*/

uniform sampler2D tex0;
uniform sampler2D tex1;
uniform vec2 originalSize;
uniform vec2 directionalField;
uniform float distanceScale;
uniform bool normalizedDistance;
uniform bool unitDirection;
uniform bool flipV;
uniform bool outputIds;
uniform bool signedMagnitude;
in vec2 v_texCoord0;

out vec4 o_color;

void main() {
    vec2 sizeDF = vec2(textureSize(tex0, 0)); // this is always square
    vec2 sizeTF = vec2(textureSize(tex1, 0)); // this can be non-square

    vec2 pixelPosition = v_texCoord0;
    vec4 textureData = texture(tex0, v_texCoord0);
    vec2 centroidPixelPosition = textureData.xy;


    vec2 pixelDistance = (centroidPixelPosition - pixelPosition) * sizeDF;

    if (flipV) {
        pixelDistance *= vec2(1.0, -1.0);
    }

    float length_ = length(pixelDistance);
    if (unitDirection) {
        if (length_ >= 1E-6) {
            pixelDistance /= length_;
        }
    }

    vec2 dfTf = sizeDF / sizeTF; // texture adjusment factor

    float outputData = (!outputIds) ? texture(tex1, v_texCoord0 * dfTf).r : textureData.b;

    #ifdef OUTPUT_DIRECTION
    if (!normalizedDistance) {
        o_color = vec4(pixelDistance * distanceScale, outputData, 1.0);
    } else if (!unitDirection) {
        o_color = vec4(pixelDistance / originalSize, outputData, 1.0);
    }
    #else
    if (!normalizedDistance) {
        o_color = vec4(vec2(length(pixelDistance * distanceScale)), outputData, 1.0);
    } else if (!unitDirection) {
        o_color = vec4(vec2(length(pixelDistance / originalSize)), outputData, 1.0);
    }
    #endif

    if (!outputIds) {
        if (signedMagnitude) {
            float s = -sign(o_color.b - 0.5);
            o_color.rg *= s;
            o_color.b = s * length_;
        }
    }
}