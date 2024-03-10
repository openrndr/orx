uniform sampler2D tex0;
uniform float exposureBias;

in vec2 v_texCoord0;
out vec4 o_output;

vec3 saturate(vec3 x) {
    return clamp(x, vec3(0.0), vec3(1.0));
}

vec3 ACESFilm(vec3 x) {
    float a = 2.51f;
    float b = 0.03f;
    float c = 2.43f;
    float d = 0.59f;
    float e = 0.14f;
    return saturate((x*(a*x+b))/(x*(c*x+d)+e));
}

void main() {
    vec3 texColor = texture(tex0,v_texCoord0).rgb;
    vec3 color = ACESFilm(texColor * exposureBias);
    vec3 retColor = pow(color, vec3(1.0/2.2));
    o_output = vec4(retColor, 1.0);
}