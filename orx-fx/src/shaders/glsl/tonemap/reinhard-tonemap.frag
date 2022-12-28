uniform sampler2D tex0;
uniform float exposureBias;
uniform float maxLuminance;

in vec2 v_texCoord0;
out vec4 o_output;

vec3 saturate(vec3 x) {
    return clamp(x, vec3(0.0), vec3(1.0));
}

float luminance(vec3 v) {
    return dot(v, vec3(0.2126f, 0.7152f, 0.0722f));
}

vec3 change_luminance(vec3 c_in, float l_out) {
    float l_in = luminance(c_in);
    return c_in * (l_out / l_in);
}

vec3 reinhard_extended_luminance(vec3 v, float max_white_l) {
    float l_old = luminance(v);
    float numerator = l_old * (1.0f + (l_old / (max_white_l * max_white_l)));
    float l_new = numerator / (1.0f + l_old);
    return change_luminance(v, l_new);
}

void main() {
    vec3 texColor = texture(tex0,v_texCoord0).rgb;
    vec3 color = reinhard_extended_luminance(texColor * exposureBias, maxLuminance);
    vec3 retColor = pow(color, vec3(1/2.2));
    o_output = vec4(retColor, 1);
}