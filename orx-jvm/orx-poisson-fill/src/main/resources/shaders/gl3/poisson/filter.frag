// from https://github.com/kosua20/Rendu/blob/master/resources/common/shaders/screens/convolution-pyramid/filter.frag

//layout(binding = 0) uniform sampler2D screenTexture; ///< Level to filter.

uniform sampler2D tex0;
in vec2 v_texCoord0;
out vec4 o_output;

uniform float g[3]; ///< g filter parameters.

/** Denotes if a pixel falls outside an image.
 \param pos the pixel position
 \param size the image size
 \return true if the pixel is outside of the image
 */
bool isOutside(ivec2 pos, ivec2 size){
    return (pos.x < 0 || pos.y < 0 || pos.x >= size.x || pos.y >= size.y);
}

/**  Apply the g filter to the input data. */
void main(){
    vec4 accum = vec4(0.0);
    ivec2 size = textureSize(tex0, 0).xy;

    ivec2 coords = ivec2(v_texCoord0 * vec2(size));

    for(int dy = -1; dy <=1; dy++){
        for(int dx = -1; dx <=1; dx++){

            ivec2 newPix = coords + ivec2(dx,dy);

            if(isOutside(newPix, size)){
                continue;
            }
            accum += g[dx+1] * g[dy+1] * texelFetch(tex0, newPix,0 );
        }
    }
    o_output = accum;
}