#version 330

uniform samplerCube tex0;
uniform vec3 sideUp;
uniform vec3 sideRight;
uniform vec3 sideNormal;
in vec2 v_texCoord0;

out vec4 o_output;

#define PI 3.1415926536

void main() {
    vec3 irradiance = vec3(0.0);

    vec2 uv = (v_texCoord0 - vec2(0.5))*2.0;
    vec3 normal = normalize(uv.x * sideRight + uv.y * sideUp + sideNormal);

    o_output.rgb = texture(tex0, normal).rgb;
    o_output.a = 1.0;

}