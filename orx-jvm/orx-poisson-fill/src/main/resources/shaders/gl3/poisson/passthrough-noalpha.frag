#version 330

in vec2 v_texCoord0;
uniform sampler2D tex0;

out vec4 o_output;

/** Denotes if a pixel falls outside an image.
 \param pos the pixel position
 \param size the image size
 \return true if the pixel is outside of the image
 */

/** Output an image translated by a fixed number of pixels on each axis. useful for padding when rendering in a larger framebuffer. */
void main(){
    vec4 c = texture(tex0, v_texCoord0);

    o_output.rgb = c.rgb;
    o_output.a = 1.0;
}