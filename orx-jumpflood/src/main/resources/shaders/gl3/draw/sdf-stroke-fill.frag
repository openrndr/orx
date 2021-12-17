uniform sampler2D tex0;// signed distance
uniform float radius;

uniform vec4 strokeColor;
uniform float strokeWeight;
uniform float strokeFeather;

uniform float fillFeather;
uniform vec4 fillColor;

in vec2 v_texCoord0;
out vec4 o_color;

void main() {
    float d = texture(tex0, v_texCoord0).r;
    float strokeFactor = smoothstep(strokeWeight + strokeFeather, strokeWeight, abs(d));
    float fillFactor = smoothstep(0.0, fillFeather, -d);

    vec4 fc = (fillColor * fillColor.a) * fillFactor;
    fc = fc * (1.0 - strokeFactor) + strokeFactor * (strokeColor * strokeColor.a);
    o_color = fc;
}