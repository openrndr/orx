#version 330

float add(float a, float b) {
    return a + b;
}

float multiply(float a, float b) {
    return a * b;
}


float equation(float a, float b) {
    return multiply(add(a, b), b);
}

float luminance(vec3 color) {
    return dot(color, vec3(0.299, 0.587, 0.114));
}

float luminance(vec4 color) {
    return dot(color.rgb, vec3(0.299, 0.587, 0.114));
}


void main() {

}