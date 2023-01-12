uniform sampler2D tex0;
in vec2 v_texCoord0;
uniform float threshold;

out vec4 o_color;

float zd(float d) {
    if (d < 0.0001) {
        return 1.0;
    } else {
        return d;
    }
}

void main() {
    vec2 stepSize = 1.0 / vec2(textureSize(tex0, 0));
    float ref = step(threshold, texture(tex0, v_texCoord0).a);


    vec2 o = vec2(0.0); //stepSize/2.0;
    float t00 = texture(tex0, v_texCoord0 + o +  vec2(0.0, 0.0)).a;
    float t10 = texture(tex0, v_texCoord0 + o + vec2(stepSize.x, 0.0)).a;
    float t01 = texture(tex0, v_texCoord0 + o + vec2(0.0, stepSize.y)).a;
    float t11 = texture(tex0, v_texCoord0 + o + vec2(stepSize.x, stepSize.y)).a;

    int mask = 0;

    if (t00 >= threshold) {
        mask += 1;
    }
    if (t10 >= threshold) {
        mask += 2;
    }
    if (t01 >= threshold) {
        mask += 4;
    }
    if (t11 >= threshold) {
        mask += 8;
    }

    vec2 offset = vec2(0.0);
    if (mask == 1) {
        offset.x = 1.0 - (threshold-t10) / zd(t00-t10);
        offset.y = 1.0 - ((threshold-t01) / zd(t00-t01));
        offset /= 2;
    }
    if (mask == 2) {
        offset.x = ((threshold-t00) / zd(t10-t00));
        offset.y = 1.0-(threshold-t11) / zd(t10-t11);
        offset /= 2;
    }
    if (mask == 3) { // OK
        float dy0 =  1.0 - (threshold - t01) / zd(t00 - t01);
        float dy1 =  1.0 - (threshold - t11) / zd(t10 - t11);
        offset.y = dy0 + dy1;
        offset.x = 1.0;
        offset /= 2;
    }
    if (mask == 4) { // OK
        offset.x = 1.0 - (threshold-t11) / zd(t01-t11);
        offset.y = (threshold-t00) / zd(t01-t00);
        offset /= 2;
    }
    if (mask == 5) { // OK
        float dx0 = 1.0- (threshold - t10) / zd(t00 - t10);
        float dx1 = 1.0-(threshold - t11) / zd(t01 - t11);
        offset.x = dx0 + dx1;
        offset.y = 1.0;
        offset /= 2;
    }
    if (mask == 6 || mask == 9) {
        offset = vec2(0.5);
    }
    if (mask == 7) { // OK
        offset.x = 1.0 - (threshold-t11) / zd(t01-t11);
        offset.y = 1.0 - (threshold-t11) / zd(t10-t11);
        offset /= 2;
    }
    if (mask == 8) { // OK
        offset.x = (threshold-t01) / zd(t11-t01);
        offset.y = (threshold-t10) / zd(t11-t10);
        offset /= 2;
    }
    if (mask == 10) { // OK
        float dx0 = (threshold - t00) / zd(t10 - t00);
        float dx1 = (threshold - t01) / zd(t11 - t01);
        offset.x = (dx0 + dx1);
        offset.y = 1.0;
        offset /= 2;
    }
    if (mask == 11) { // OK
        offset.x = (threshold-t01) / zd(t11-t01);
        offset.y = (threshold-t01) / zd(t00-t01);
        offset /= 2;
    }
    if (mask == 12) { // OK
        float dy0 = (threshold - t00) / zd(t01 - t00);
        float dy1 = (threshold - t10) / zd(t11 - t10);
        offset.y = dy0 + dy1;
        offset.x = 1.0;
        offset /= 2;
    }
    if (mask == 13) { // OK
        offset.x = 1.0 - (threshold-t10) / zd(t00-t10);
        offset.y = (threshold-t10) / zd(t11-t10);
        offset /= 2;
    }
    if (mask == 14) { // OK
        offset.x = (threshold-t00) / zd(t10-t00);
        offset.y = (threshold-t00) / zd(t01-t00);
        offset /= 2;
    }

    float contour = (mask != 0 && mask != 15)?1.0:0.0;

    //float contour = (mask == 14 || mask == 11 || mask == 7 || mask == 13) ? 1.0 : 0.0;
    if (contour > 0.0) {
        o_color = vec4(v_texCoord0 /*+ offset*stepSize*/ , ref, 1.0);
    } else {
        o_color = vec4(-1.0, -1.0, 0.0, 1.0);
    }
}