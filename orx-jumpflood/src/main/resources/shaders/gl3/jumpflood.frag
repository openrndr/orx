in vec2 v_texCoord0;

uniform sampler2D tex0;
uniform int maxSteps;
uniform int step;

out vec4 o_color;
void main() {

    float stepwidth = 1.0 / pow(2.0, min(maxSteps, step+1));

    float bestDistance = 999999.0;
    vec2 bestCoord = vec2(-1.0);
    vec2 bestColor = vec2(-1.0);

    vec2 is = vec2(1.0) / textureSize(tex0, 0);

    float found = 0.0;
    for (int y = -1; y <= 1; ++y) {
        for (int x = -1; x <= 1; ++x) {
            vec2 sampleCoord = v_texCoord0 + vec2(stepwidth) * vec2(x,y);
            vec4 data = texture( tex0, sampleCoord);
            vec2 seedCoord = data.xy;
            vec2 seedColor = data.zw;
            float dist = length(seedCoord - v_texCoord0);
            if ((seedCoord.x >= 0.0 || seedCoord.y >= 0.0) && dist < bestDistance)
            {
                found = 1.0;
                bestDistance = dist;
                bestCoord = seedCoord;
                bestColor = seedColor;
            }
        }
    }

    o_color = vec4(bestCoord, bestColor.r, 1.0);

}