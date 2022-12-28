in vec2 v_texCoord0;

uniform sampler2D tex0;

uniform vec4 backgroundColor;
uniform vec4 edgeColor;
uniform float backgroundOpacity;
uniform float edgeOpacity;
out vec4 o_output;

float step = 1.0;

float luma(vec4 color){
    vec3 n = color.a == 0.0? vec3(0.0) : color.rgb/color.a;
    return dot(n, vec3(1.0/3.0));
}

/** Denotes if UV coordinates falls outside an image.
 \param pos the UV coordinates
 \return true if the UV are outside of the image
 */
bool isOutside(vec2 pos){
    return (pos.x < 0.0 || pos.y < 0.0 || pos.x > 1.0 || pos.y > 1.0);
}

/** Compute the Laplacian field of an input RGB image, adding a 1px black border around it before computing the gradients and divergence. */
void main(){
    float div = 0.0;
    ivec2 size = textureSize(tex0, 0).xy;

    vec3 pixelShift = vec3(0.0);
    pixelShift.xy = 1.0/vec2(size);

    vec2 uvs = v_texCoord0;
    if(!isOutside(uvs)){
        float col = luma(textureLod(tex0, uvs, 0.0));
        div = 4.0 * col;
    }

    vec2 uvs110 = uvs + pixelShift.xz;
    if(!isOutside(uvs110)){
        float col110 = luma(textureLod(tex0, uvs110, 0.0));
        div -= col110;
    }
    vec2 uvs101 = uvs + pixelShift.zy;
    if(!isOutside(uvs101)){
        float col101 = luma(textureLod(tex0, uvs101, 0.0));
        div -= col101;
    }
    vec2 uvs010 = uvs - pixelShift.xz;
    if(!isOutside(uvs010)){
        float col010 = luma(textureLod(tex0, uvs010, 0.0));
        div -= col010;
    }
    vec2 uvs001 = uvs - pixelShift.zy;
    if(!isOutside(uvs001)){
        float col001 = luma(textureLod(tex0, uvs001, 0.0));
        div -= col001;
    }
    o_output.rgb = vec3(div);
    o_output.a = 1.0f;
}