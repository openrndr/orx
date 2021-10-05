in vec2 v_texCoord0;
uniform sampler2D tex0; // input
uniform float foregroundLuma;
uniform float backgroundLuma;
uniform float foregroundOpacity;
uniform float backgroundOpacity;

out vec4 o_color;
void main() {
    vec4 c = texture(tex0, v_texCoord0);
    float l = dot( (c.a> 0.0? c.rgb/c.a : vec3(0.0)), vec3(1.0/3.0));
    float mf = smoothstep(backgroundLuma, foregroundLuma, l);
    float o = mix(backgroundOpacity, foregroundOpacity, mf);
    o_color = c * o;
}
