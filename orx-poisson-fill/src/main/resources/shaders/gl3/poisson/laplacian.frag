// adapted from https://github.com/kosua20/Rendu/blob/master/resources/common/shaders/screens/convolution-pyramid/laplacian.frag
#version 330

in vec2 v_texCoord0;
uniform sampler2D tex0;
out vec4 o_output;

/** Denotes if UV coordinates falls outside an image.
 \param pos the UV coordinates
 \return true if the UV are outside of the image
 */
bool isOutside(vec2 pos){
    return (pos.x < 0.0 || pos.y < 0.0 || pos.x > 1.0 || pos.y > 1.0);
}

/** Compute the Laplacian field of an input RGB image, adding a 1px black border around it before computing the gradients and divergence. */
void main(){
    vec3 div = vec3(0.0);
    ivec2 size = textureSize(tex0, 0).xy;

    vec3 pixelShift = vec3(0.0);
    pixelShift.xy = 1.0/vec2(size);

    vec2 uvs = v_texCoord0;
    if(!isOutside(uvs)){
        vec3 col = textureLod(tex0, uvs, 0.0).rgb;
        div = 4.0 * col;
    }

    vec2 uvs110 = uvs + pixelShift.xz;
    if(!isOutside(uvs110)){
        vec3 col110 = textureLod(tex0, uvs110, 0.0).rgb;
        div -= col110;
    }
    vec2 uvs101 = uvs + pixelShift.zy;
    if(!isOutside(uvs101)){
        vec3 col101 = textureLod(tex0, uvs101, 0.0).rgb;
        div -= col101;
    }
    vec2 uvs010 = uvs - pixelShift.xz;
    if(!isOutside(uvs010)){
        vec3 col010 = textureLod(tex0, uvs010, 0.0).rgb;
        div -= col010;
    }
    vec2 uvs001 = uvs - pixelShift.zy;
    if(!isOutside(uvs001)){
        vec3 col001 = textureLod(tex0, uvs001, 0.0).rgb;
        div -= col001;
    }
    o_output.rgb = div;
    o_output.a = 1.0f;
}