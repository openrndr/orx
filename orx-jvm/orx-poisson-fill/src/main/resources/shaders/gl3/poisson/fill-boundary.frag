// from https://github.com/kosua20/Rendu/blob/master/resources/common/shaders/screens/convolution-pyramid/fill-boundary.frag

in vec2 v_texCoord0;
uniform sampler2D tex0;

//layout(binding = 0) uniform sampler2D screenTexture; ///< Image to process.

//layout(location = 0) out vec4 fragColor; ///< Color.

out vec4 o_output;

/** Denotes if a pixel falls outside an image.
 \param pos the pixel position
 \param size the image size
 \return true if the pixel is outside of the image
 */

/**  Output color only on the edges of the black regions in the input image, along with a 1.0 alpha. */
void main(){

    o_output = vec4(0.0);

    vec4 fullColor = textureLod(tex0, v_texCoord0, 0.0);

    float isInMask = fullColor.a == 1.0 ? 0.0 : 1.0; //float(all(equal(fullColor.rgb, vec3(0.0))));
    float maskLaplacian = -4.0*(1.0-fullColor.a);

//    float maskLaplacian = -4.0 * isInMask;
    vec4 cola110 = textureLodOffset(tex0, v_texCoord0, 0.0, ivec2( 1, 0));
    vec4 cola101 = textureLodOffset(tex0, v_texCoord0, 0.0, ivec2( 0, 1));
    vec4 cola010 = textureLodOffset(tex0, v_texCoord0, 0.0, ivec2(-1, 0));
    vec4 cola001 = textureLodOffset(tex0, v_texCoord0, 0.0, ivec2( 0,-1));

    vec3 col110 = cola110.rgb; // cola110.a;
    vec3 col101 = cola101.rgb; // cola101.a;
    vec3 col010 = cola010.rgb; // cola010.a;
    vec3 col001 = cola001.rgb; // cola001.a;
//    maskLaplacian += float(all(equal(col110, vec3(0.0))));
//    maskLaplacian += float(all(equal(col101, vec3(0.0))));
//    maskLaplacian += float(all(equal(col010, vec3(0.0))));
//    maskLaplacian += float(all(equal(col001, vec3(0.0))));

//    maskLaplacian += cola110.a == 1.0 ? 0.0 : 1.0;
//    maskLaplacian += cola101.a == 1.0 ? 0.0 : 1.0;
//    maskLaplacian += cola010.a == 1.0 ? 0.0 : 1.0;
//    maskLaplacian += cola001.a == 1.0 ? 0.0 : 1.0;

    maskLaplacian += (1.0-cola110.a);
    maskLaplacian += (1.0-cola101.a);
    maskLaplacian += (1.0-cola010.a);
    maskLaplacian += (1.0-cola001.a);

    if(maskLaplacian > 0.0){
        o_output.rgb = fullColor.rgb;///fullColor.a;;
        o_output.a = fullColor.a;

    }



}