// adapted from https://github.com/kosua20/Rendu/blob/master/resources/common/shaders/screens/convolution-pyramid/fill-boundary.frag

#version 330

in vec2 v_texCoord0;
uniform sampler2D tex0; // input image
uniform sampler2D tex1; // mask

out vec4 o_output;

/**  Output color only on the edges of the black regions in the input image, along with a 1.0 alpha. */
void main(){

    o_output = vec4(0.0);

    vec4 fullColor = textureLod(tex0, v_texCoord0, 0.0);
    float maskColor = textureLod(tex1, v_texCoord0, 0.0).r;

    float isInMask = maskColor == 1.0 ? 1.0 : 0.0;

    float maskLaplacian = -4.0 * isInMask;
    float mask110 = textureLodOffset(tex1, v_texCoord0, 0.0, ivec2( 1, 0)).r;
    float mask101 = textureLodOffset(tex1, v_texCoord0, 0.0, ivec2( 0, 1)).r;
    float mask010 = textureLodOffset(tex1, v_texCoord0, 0.0, ivec2(-1, 0)).r;
    float mask001 = textureLodOffset(tex1, v_texCoord0, 0.0, ivec2( 0,-1)).r;

    maskLaplacian += mask110 == 1.0 ? 1.0 : 0.0;
    maskLaplacian += mask101 == 1.0 ? 1.0 : 0.0;
    maskLaplacian += mask010 == 1.0 ? 1.0 : 0.0;
    maskLaplacian += mask001 == 1.0 ? 1.0 : 0.0;

    if(maskLaplacian > 0.0){
        o_output.rgb = fullColor.rgb;
        o_output.a = 1.0;
    }

}