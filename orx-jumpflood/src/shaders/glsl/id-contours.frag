uniform sampler2D tex0;
in vec2 v_texCoord0;

out vec4 o_color;

void main() {
vec4 colorMask = vec4(0.0, 0.0, 1.0, 0.0);
    vec2 stepSize = 1.0 / vec2(textureSize(tex0, 0));
    vec4 ref = texture(tex0, v_texCoord0);

    float laplacian = 0.0;

    laplacian += abs(texture(tex0, v_texCoord0 + vec2(stepSize.x, 0.0)).b-ref.b);
    laplacian += abs(texture(tex0, v_texCoord0 - vec2(stepSize.x, 0.0)).b-ref.b);
    laplacian += abs(texture(tex0, v_texCoord0 + vec2(0.0, stepSize.y)).b-ref.b);
    laplacian += abs(texture(tex0, v_texCoord0 - vec2(0.0, stepSize.y)).b-ref.b);

    float contour = step(0.0, laplacian);

    if (laplacian > 0.001) {
        o_color = vec4(v_texCoord0.x, v_texCoord0.y, ref.b, 1.0);
    } else {
        o_color = vec4(-1.0, -1.0, -1.0, 1.0);
    }
}