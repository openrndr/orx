uniform sampler2D tex0;// signed distance
uniform sampler2D tex1;// signed distance
uniform float radius;

in vec2 v_texCoord0;
out vec4 o_color;

float opSmoothDifference( float d1, float d2, float k ) {
    float h = clamp( 0.5 - 0.5*(d2+d1)/k, 0.0, 1.0 );
    return mix( d2, -d1, h ) + k*h*(1.0-h); }


void main() {
    float d0 = texture(tex0, v_texCoord0).r;
    float d1 = texture(tex1, v_texCoord0).r;
    o_color = vec4(opSmoothDifference(d0, d1, radius), 0.0, 0.0, 1.0);
}