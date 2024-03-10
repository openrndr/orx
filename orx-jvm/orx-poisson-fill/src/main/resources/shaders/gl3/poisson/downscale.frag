// from https://github.com/kosua20/Rendu/blob/master/resources/common/shaders/screens/convolution-pyramid/downscale.frag
in vec2 v_texCoord0;

uniform vec2 targetSize;
uniform sampler2D tex0;
out vec4 o_output;
uniform vec2 padding;
uniform float h1[5]; ///< h1 filter parameters.

/** Denotes if a pixel falls outside an image.
 \param pos the pixel position
 \param size the image size
 \return true if the pixel is outside of the image
 */
bool isOutside(ivec2 pos, ivec2 size){

    return (pos.x < 0 || pos.y < 0 || pos.x > size.x || pos.y > size.y);
}

/** Apply the h1 filter and downscale the input data by a factor of 2. */
void main(){

    vec4 accum = vec4(0.0);

    ivec2 size = textureSize(tex0, 0).xy;

    ivec2 ts = size;

    //ivec2 ts = ivec2(targetSize - 2 * padding);

    // Our current size is half this one, so we have to scale by 2.

    ivec2 coords = ivec2(floor( targetSize * v_texCoord0)) * 2 - ivec2(10);


    for(int dy = -2; dy <=2; dy++){
        for(int dx = -2; dx <=2; dx++){
            ivec2 newPix = coords+ivec2(dx,dy);
            if(isOutside(newPix, size)){
                continue;
                //accum = vec4(1.0, 0.0, 0.0, 1.0);
            }
            accum += h1[dx+2] * h1[dy+2] * texelFetch(tex0, newPix,0
            );
        }
    }
    o_output = accum;
}