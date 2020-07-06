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

    vec3 up    = vec3(0.0, 1.0, 0.0);
    vec3 right = cross(up, normal);
    up         = cross(normal, right);

    float sampleDelta = 0.025;
    int nrSamples = 0;
    for(float phi = 0.0; phi < 2.0 * PI; phi += sampleDelta)  {
        for(float theta = 0.0; theta < 0.5 * PI; theta += sampleDelta) {
            // spherical to cartesian (in tangent space)
            vec3 tangentSample = vec3(sin(theta) * cos(phi),  sin(theta) * sin(phi), cos(theta));
            // tangent space to world
            vec3 sampleVec = tangentSample.x * right + tangentSample.y * up + tangentSample.z * normal;

            irradiance += texture(tex0, sampleVec).rgb * cos(theta) * sin(theta);
            nrSamples++;
        }
    }
    irradiance = PI * irradiance * (1.0 / float(nrSamples));
    o_output.rgb = irradiance.rgb;
    o_output.a = 1.0;

}