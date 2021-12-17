uniform sampler2D tex0;// signed distance
uniform vec4 skeletonColor;
uniform vec4 backgroundColor;
uniform vec4 foregroundColor;
uniform float angleTreshold;

in vec2 v_texCoord0;

out vec4 o_color;

void main() {
    vec4 ct = texture(tex0, v_texCoord0);
    vec2 cd = normalize(ct.xy);
    vec2 step = 1.0 / textureSize(tex0, 0);

    float minDistance = 1000.0;

    vec4 nt = texture(tex0, v_texCoord0 + step * vec2(0.0, -1.0));
    vec2 nd = normalize(nt.xy);
    vec4 et = texture(tex0, v_texCoord0 + step * vec2(1.0, 0.0));
    vec2 ed = normalize(et.xy);
    vec4 wt = texture(tex0, v_texCoord0 + step * vec2(-1.0, 0.0));
    vec2 wd = normalize(wt.xy);
    vec4 st = texture(tex0, v_texCoord0 + step * vec2(0.0, 1.0));
    vec2 sd = normalize(st.xy);

    float d0 = dot(cd, nd);
    float d1 = dot(cd, ed);
    float d2 = dot(cd, wd);
    float d3 = dot(cd, sd);

    float r = (d0+d1+d2+d3);

    vec4 fc = vec4(0.0);

    if (ct.z > 0.0) {
        fc += foregroundColor * foregroundColor.a;
    } else {
        fc += backgroundColor * backgroundColor.a;
    }

    if ((d0 < angleTreshold || d1 < angleTreshold || d2 < angleTreshold || d3 < angleTreshold) && ct.z > 0.0 && length(ct.xy) > 4) {
        fc = fc * (1.0 - skeletonColor.a) + (skeletonColor * skeletonColor.a);
    }
    o_color = fc;
}