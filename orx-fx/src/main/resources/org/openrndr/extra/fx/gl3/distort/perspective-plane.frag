#version 330 core

// based on https://www.shadertoy.com/view/wsBSWG by bloxard

uniform sampler2D tex0;
in vec2 v_texCoord0;
uniform vec3 cameraPosition;
uniform vec3 planePosition;
uniform mat4 planeMatrix;
uniform bool tile;
uniform vec2 targetSize;
out vec4 o_color;

void main() {
    vec3 vCamPos = cameraPosition;
    vec3 vPlanePos = planePosition;
    vec3 vPlaneRight = vec3(1.0, 0.0, 0.0);
    vec3 vPlaneUp = vec3(0.0, 1.0, 0.0);

    mat3 m = mat3(planeMatrix);
    vPlaneUp *= m;
    vPlaneRight *= m;

    vec3 vPlaneNormal = normalize(cross(vPlaneRight, vPlaneUp));
    float fPlaneDeltaNormalDistance = dot(vPlanePos, vPlaneNormal) - dot(vPlaneNormal, vCamPos);
    vec4 color = vec4(0.);
    for (int m = 0; m < 2; m++) {
        for (int n = 0; n < 2; n++) {
            vec2 s = (v_texCoord0 - vec2(0.5)) * 2.0;
            s*= vec2(1.0, targetSize.y / targetSize.x);
            vec3 vRayDir = normalize(vec3(s, -1.0));
            float t = fPlaneDeltaNormalDistance / dot(vPlaneNormal, vRayDir);
            vec3 hitPos = vCamPos + vRayDir * t;
            vec3 delta = hitPos - vPlanePos;
            vec2 bary = vec2(dot(delta, vPlaneRight), dot(delta, vPlaneUp));

            bary /= vec2(1.0, targetSize.y / targetSize.x);
            bary += vec2(0.5);
            if ((tile || (bary.x >= 0.0 && bary.x <= 1.0 && bary.y >=0.0 && bary.y <= 1.0)) && t > 0.0) {
                color += texture(tex0, bary);
            }
        }
    }
    o_color = color * 0.25;
}
