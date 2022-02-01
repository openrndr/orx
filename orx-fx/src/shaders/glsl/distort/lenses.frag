in vec2 v_texCoord0;
uniform sampler2D tex0;// input
uniform float scale;
uniform float rotation;

out vec4 o_color;
void main() {
    vec2 uv = v_texCoord0;
    vec2 blockSize = vec2(1.0/8.0, 1.0/6.0);

    vec2 blockIndex = floor(uv / blockSize);
    vec2 blockCenter = (blockIndex+0.5) * blockSize;

    float ca = cos(radians(rotation));
    float sa = sin(radians(rotation));

    vec2 ts = textureSize(tex0, 0);

    mat2 rm =mat2(1.0, 0.0, 0.0, ts.x/ts.y) * mat2(vec2(ca, sa), vec2(-sa, ca)) * mat2(1.0, 0.0, 0.0, ts.y/ts.x);
    vec2 cuv = (rm * (uv - blockCenter)
    * scale + blockCenter);



    float sx = step(0.0, cuv.x) * (1.0 - step(1.0, cuv.x));
    float sy = step(0.0, cuv.y) * (1.0 - step(1.0, cuv.y));
    vec4 c = texture(tex0, cuv) * sx * sy;

    o_color = c;
}
