#version 330

in vec2 v_texCoord0;

uniform sampler2D tex0; // membrane
uniform sampler2D tex1; // target image
uniform sampler2D tex2; // source image
uniform sampler2D tex3; // mask
uniform sampler2D tex4; // soft mask

uniform float softMaskGain;
out vec4 o_output;

void main(){

    vec4 targetColor = textureLod(tex1, v_texCoord0, 0.0).rgba;
    float maskColor = textureLod(tex3, v_texCoord0, 0.0).r;
    float mask = maskColor == 1.0 ? 1.0 : 0.0;
    float softMask = textureLod(tex4, v_texCoord0, 0.0).r;

    float maskLaplacian = -4.0 * mask;
    float mask110 = textureLodOffset(tex3, v_texCoord0, 0.0, ivec2( 1, 0)).r;
    float mask101 = textureLodOffset(tex3, v_texCoord0, 0.0, ivec2( 0, 1)).r;
    float mask010 = textureLodOffset(tex3, v_texCoord0, 0.0, ivec2(-1, 0)).r;
    float mask001 = textureLodOffset(tex3, v_texCoord0, 0.0, ivec2( 0,-1)).r;

    maskLaplacian += mask110 == 1.0 ? 1.0 : 0.0;
    maskLaplacian += mask101 == 1.0 ? 1.0 : 0.0;
    maskLaplacian += mask010 == 1.0 ? 1.0 : 0.0;
    maskLaplacian += mask001 == 1.0 ? 1.0 : 0.0;

    if (maskLaplacian > 0) {
        mask = 1;
    }
    {
        vec4 sourceColor = textureLod(tex2, v_texCoord0, 0.0);
        vec4 membraneColor = textureLod(tex0, v_texCoord0, 0.0);
        membraneColor.rgb /= membraneColor.a;

        vec3 blend = membraneColor.rgb + sourceColor.rgb;

        o_output.rgb = mix(targetColor.rgb, blend, mask*max(0.0,min(1.0, softMask * softMaskGain)));
        o_output.a = 1.0;

    }
}