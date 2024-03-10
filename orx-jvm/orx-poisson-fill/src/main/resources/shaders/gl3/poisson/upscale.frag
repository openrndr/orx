// from https://github.com/kosua20/Rendu/blob/master/resources/common/shaders/screens/convolution-pyramid/upscale.frag

in vec2 v_texCoord0;

uniform sampler2D tex0; ///< Current h1 filtered level.
uniform sampler2D tex1; ///< Previous h1+g filtered level.

out vec4 o_output; ///< Color.

uniform float h1[5]; ///< h1 filter parameters.
uniform float h2; ///< h2 scaling parameter.
uniform float g[3]; ///< g filter parameters.

/** Denotes if a pixel falls outside an image.
 \param pos the pixel position
 \param size the image size
 \return true if the pixel is outside of the image
 */
bool isOutside(ivec2 pos, ivec2 size){
    return (pos.x < 0 || pos.y < 0 || pos.x >= size.x || pos.y >= size.y);
}

/** Combine previous level filtered with h2 (applying a 0-filled upscaling) and the current level filtered with g.
 */
void main(){
    vec4 accum = vec4(0.0);
    ivec2 size = textureSize(tex0, 0).xy;
    ivec2 coords = ivec2(v_texCoord0 * vec2(size));

    for(int dy = -1; dy <=1; dy++){
        for(int dx = -1; dx <=1; dx++){
            ivec2 newPix = coords+ivec2(dx,dy);
            if(isOutside(newPix, size)){
                continue;
            }
            accum += g[dx+1] * g[dy+1] * texelFetch(tex0, newPix,0);
        }
    }

    ivec2 sizeSmall = textureSize(tex1, 0).xy;

    for(int dy = -2; dy <=2; dy++){
        for(int dx = -2; dx <=2; dx++){
            ivec2 newPix = coords+ivec2(dx,dy);
            // The filter is applied to a texture upscaled by inserting zeros.
            if(newPix.x%2 != 0 || newPix.y%2 != 0){
                continue;
            }
            newPix /= 2;
            newPix += 5;
            if(isOutside(newPix, sizeSmall)){
                accum = vec4(0.0, 0.0, 1.0, 1.0);
            }
            accum += h2 * h1[dx+2] * h1[dy+2] * texelFetch(tex1, newPix, 0);
        }
    }
    o_output = accum;
}