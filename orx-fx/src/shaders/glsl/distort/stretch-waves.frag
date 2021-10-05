uniform sampler2D tex0;
in vec2 v_texCoord0;

uniform float phase;
uniform float rotation;
uniform float distortion;
uniform float frequency;
uniform float feather;
out vec4 o_color;

void main() {
    float phi = radians(rotation);
    float cp = cos(phi);
    float sp = sin(phi);
    mat2 rm = mat2(vec2(cp, sp), vec2(-sp, cp));
    mat2 irm = transpose(rm);

    float tw = 1.0 / frequency;
    vec2 uv = rm * (v_texCoord0 - vec2(0.5)) + vec2(0.5) + vec2(phase * tw, 0.0);

    float xd = (uv.x) * frequency;
    float xo = (fract(xd) - 0.5) * 2.0;
    float xf = fract(xd);

    float offs = (1.0- xo * xo) * 1.0 * xo * distortion * 0.5;
    float f = mix(1.0, (1.0 - xo * xo), distortion);

    vec2 fuv = uv;
    fuv.x = floor(uv.x * frequency) / frequency;
    fuv.x += (xf - offs) * tw;

    fuv = irm * (fuv - vec2(0.5) - vec2(phase * tw, 0.0)) + vec2(0.5);

    vec2 step = fwidth(fuv) * feather;
    float fx = smoothstep(0.0, step.x, fuv.x) * smoothstep(1.0, 1.0 - step.x, fuv.x);
    float fy = smoothstep(0.0, step.y, fuv.y) * smoothstep(1.0, 1.0 - step.y, fuv.y);

    vec4 c = texture(tex0, fuv) * f * fx * fy;
    o_color = c;
}