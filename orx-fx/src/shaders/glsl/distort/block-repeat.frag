in vec2 v_texCoord0;
uniform sampler2D tex0;// input
uniform float blockWidth;
uniform float blockHeight;
uniform float blockOffsetX;
uniform float blockOffsetY;
uniform float sourceOffsetX;
uniform float sourceOffsetY;
uniform float sourceScale;

out vec4 o_color;
void main() {
    vec2 uv = v_texCoord0;
    vec2 blockSize = vec2(blockWidth, blockHeight);
    vec2 blockOffset = vec2(blockOffsetX, blockOffsetY);
    vec2 blockCoord = uv / blockSize + blockOffset;

    ivec2 blockIndex = ivec2(blockCoord);
    vec2 blockUV = mod(blockCoord - blockIndex, vec2(1.0));
    vec2 blockAspect = vec2(1.0);


    if (blockWidth < blockHeight) {
        blockAspect = vec2(blockWidth / blockHeight, 1.0);
    }

    if (blockHeight < blockWidth) {
        blockAspect = vec2(1.0, blockHeight/blockWidth);
    }

    vec2 tUV = mix(blockUV * blockSize, blockUV * blockAspect, sourceScale);

//    vec2 fw = fwidth(blockCoord);
//    float f = smoothstep(0.0, 0.01, blockUV.x) * smoothstep(0.0, 0.01, blockUV.y);

    vec2 sourceOffset = vec2(sourceOffsetX, sourceOffsetY);
    vec4 c = texture(tex0, mod(tUV + sourceOffset, vec2(1.0)));
    o_color = c;
}
