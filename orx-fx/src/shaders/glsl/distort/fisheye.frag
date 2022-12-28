uniform sampler2D tex0;
uniform float strength;
uniform float feather;
uniform float scale;
uniform float rotation;
in vec2 v_texCoord0;
out vec4 o_color;

void main() {
    vec2 uv = v_texCoord0;
    vec2 ts = vec2(textureSize(tex0, 0));
    vec2 step = 1.0 / ts;

    float phi = radians(rotation);
    float cp = cos(phi);
    float sp = sin(phi);
    mat2 rm = mat2(vec2(cp, sp), vec2(-sp, cp));

    float aspectRatio = ts.y / ts.x;
    step.y /= aspectRatio;
    step *= feather;

    vec2 intensity = vec2(strength, strength);

    vec2 coords = uv;
    coords = (coords - 0.5) * 2.0;

    coords = rm * coords;

    vec2 realCoordOffs;
    realCoordOffs.x = (1.0 - coords.y * coords.y) * intensity.y * (coords.x);
    realCoordOffs.y = (1.0 - coords.x * coords.x) * intensity.x * (coords.y);

    vec2 fuv = ((uv - realCoordOffs) - vec2(0.5)) * scale + vec2(0.5);

    float fx = smoothstep(0.0, step.x, fuv.x) * smoothstep(1.0, 1.0 - step.x, fuv.x);
    float fy = smoothstep(0.0, step.y, fuv.y) * smoothstep(1.0, 1.0 - step.y, fuv.y);

    vec4 color = texture(tex0, fuv) * fx * fy;
    o_color = color;
}