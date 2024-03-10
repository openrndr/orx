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
    vec2 s = 1.0 / vec2(textureSize(tex0, sourceLevel).xy);
    int w = window;

    vec4 sum = vec4(0.0);
    float weight = 0.0;
    for (int x = -w; x <= w; ++x) {
        float lw = exp( float(-(x*x)) / (2.0 * sigma * sigma) ) ;
        vec2 tc = v_texCoord0 + float(x) * blurDirection * s;// * spread;
        #ifndef OR_WEBGL2
        sum += textureLod(tex0, tc, float(sourceLevel)) * lw;
        #else
        sum += texture(tex0, tc);
        #endif
        weight += lw;
    }
    o_color = (sum / weight) * gain;
}