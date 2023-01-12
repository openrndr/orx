uniform sampler2D tex0;
uniform sampler2D tex1;

uniform vec2 originalSize;
uniform float distanceScale;
uniform bool signedBit;
uniform bool signedDistance;

in vec2 v_texCoord0;

out vec4 o_color;

void main() {
    vec2 sizeDF = vec2(textureSize(tex0, 0)); // this is always square
    vec2 sizeTF = vec2(textureSize(tex1, 0)); // this can be non-square

    vec2 pixelPosition = v_texCoord0;
    vec2 centroidPixelPosition = texture(tex0, v_texCoord0).xy;
    vec2 pixelDistance = (centroidPixelPosition - pixelPosition) * sizeDF * vec2(1.0, -1.0);

    vec2 dfTf = sizeDF / sizeTF; // texture adjusment factor

    float threshold = texture(tex1, v_texCoord0 * dfTf).r;
    float distance = length(pixelDistance) * distanceScale;

    if (signedDistance) {
        if (threshold > 0.5) {
            distance *= -1.0;
        }
    }

    if (signedBit) {
        o_color = vec4(distance, threshold, 0.0, 1.0);
    }  else {
        o_color = vec4(vec3(distance), 1.0);
    }
}