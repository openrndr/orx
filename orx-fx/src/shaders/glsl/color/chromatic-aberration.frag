in vec2 v_texCoord0;

uniform sampler2D tex0;

uniform float aberrationFactor;
uniform vec2 dimensions;

out vec4 o_color;

void main() {
    vec2 uv = v_texCoord0;
    float factor = (1.0 / dimensions.x) * aberrationFactor;

    vec4 tex = texture(tex0, uv);

    float r = texture(tex0, vec2(uv.x - factor, uv.y)).r;
    float g = tex.g;
    float b = texture(tex0, vec2(uv.x + factor, uv.y)).b;

    o_color = vec4(vec3(r, g, b), tex.a);
}