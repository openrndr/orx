in vec2 v_texCoord0;
uniform sampler2D tex0;// input
uniform float scale;
uniform float rotation;
uniform int rows;
uniform int columns;
uniform float distort;

out vec4 o_color;
void main() {
    vec2 uv = v_texCoord0;
    vec2 blockSize = vec2(1.0 / float(columns), 1.0 / float(rows));
    vec2 blockIndex = floor(uv / blockSize);
    vec2 blockUV = mod(uv/blockSize, vec2(1.0));
    vec2 blockUVC1 = (blockUV - vec2(0.5)) * 2.0;
    vec2 blockCenter = (blockIndex + 0.5) * blockSize;

    float ca = cos(radians(rotation));
    float sa = sin(radians(rotation));

    vec2 ts = vec2(textureSize(tex0, 0));
    mat2 rm = mat2(1.0, 0.0, 0.0, ts.x / ts.y) * mat2(vec2(ca, sa), vec2(-sa, ca)) * mat2(1.0, 0.0, 0.0, ts.y / ts.x);
    vec2 ruv = (uv - blockCenter);
    vec2 luv;
    luv.x = (1.0 - blockUVC1.y * blockUVC1.y * distort) * ruv.x;
    luv.y = (1.0 - blockUVC1.x * blockUVC1.x * distort) * ruv.y;
    vec2 cuv = (rm * luv * scale + blockCenter);

    float sx = step(0.0, cuv.x) * (1.0 - step(1.0, cuv.x));
    float sy = step(0.0, cuv.y) * (1.0 - step(1.0, cuv.y));
    vec4 c = texture(tex0, cuv) * sx * sy;

    o_color = c;
}
