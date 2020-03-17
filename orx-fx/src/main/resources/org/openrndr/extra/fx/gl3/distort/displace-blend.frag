#version 330 core

in vec2 v_texCoord0;
uniform sampler2D tex0;// input
uniform sampler2D tex1;// input
uniform float offset;
uniform float gain;
uniform vec2 targetSize;
uniform float rotation;
uniform float feather;
uniform float sourceOpacity;
uniform float targetOpacity;
out vec4 o_color;
void main() {


    float phi = radians(rotation);
    float cp = cos(phi);
    float sp = sin(phi);
    mat2 rm = mat2(vec2(cp,sp), vec2(-sp,cp));

    vec4 oa = texture(tex0, v_texCoord0);
    vec4 b = texture(tex1, v_texCoord0);

    float ar = targetSize.y / targetSize.x;

    vec4 nb = b.a > 0? b/b.a : vec4(0.0);

    vec2 offset = (nb.rg - vec2(offset))*vec2(gain) * nb.a;
    offset = rm * offset * vec2(1.0, ar);


    vec2 step = fwidth(v_texCoord0) * feather;

    vec2 displaced = v_texCoord0 + offset;

    float fx = smoothstep(0.0, step.x, displaced.x) * smoothstep(1.0, 1.0-step.x, displaced.x);
    float fy = smoothstep(0.0, step.y, displaced.y) * smoothstep(1.0, 1.0-step.y, displaced.y);

    vec4 a = texture(tex0, displaced) * mix(1.0, fx * fy, b.a);

    o_color = (a + (1.0-a.a) * oa * sourceOpacity) * b.a * targetOpacity + (1.0-b.a*targetOpacity) * oa * sourceOpacity;

}
