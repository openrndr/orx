in vec2 v_texCoord0;
uniform sampler2D tex0;
uniform vec2 blurDirection;

uniform int window;
uniform float sigma;
uniform float spread;
uniform float gain;

uniform int sourceLevel;

out vec4 o_color;
void main() {
    vec2 s = 1.0 / textureSize(tex0, sourceLevel).xy;
    int w = window;

    vec4 sum = vec4(0.0);
    float weight = 0;
    for (int x = -w; x <= w; ++x) {
        float lw = exp( -(x*x) / (2 * sigma * sigma) ) ;
        vec2 tc = v_texCoord0 + x * blurDirection * s;// * spread;
        sum += textureLod(tex0, tc, sourceLevel) * lw;
        weight += lw;
    }
    o_color = (sum / weight) * gain;
}