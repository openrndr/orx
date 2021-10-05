// ad[a|o]pted from http://filmicworlds.com/blog/filmic-tonemapping-operators/
float A = 0.15;
float B = 0.50;
float C = 0.10;
float D = 0.20;
float E = 0.02;
float F = 0.30;
float W = 11.2;

uniform sampler2D tex0;
uniform float exposureBias;
in vec2 v_texCoord0;
out vec4 o_output;

vec3 Uncharted2Tonemap(vec3 x) {
    return ((x*(A*x+C*B)+D*E)/(x*(A*x+B)+D*F))-E/F;
}

void main() {
    vec3 texColor = texture(tex0,v_texCoord0).rgb;
    vec3 curr = Uncharted2Tonemap(exposureBias*texColor);
    vec3 whiteScale = 1.0f/Uncharted2Tonemap(vec3(W));
    vec3 color = curr*whiteScale;

    vec3 retColor = pow(color, vec3(1/2.2));
    o_output = vec4(retColor, 1);
}