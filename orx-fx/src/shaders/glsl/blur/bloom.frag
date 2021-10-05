in vec2 v_texCoord0;
out vec4 o_color;

uniform sampler2D tex0;
uniform sampler2D tex1;
uniform float blendFactor;
uniform float brightness;

void main() {
    vec3 original = texture(tex0, v_texCoord0).rgb;
    vec3 bloom = texture(tex1, v_texCoord0).rgb;

    vec3 hdrColor = mix(original, bloom, blendFactor);

    vec3 result = vec3(1.0) - exp(-hdrColor * brightness);

    o_color = vec4(result, 1.0);
}
