#pragma glslify: add = require(sum)
#pragma glslify: multiply = require(multiply)
#pragma glslify: subtract = require('./subtract.glsl')

float solve(float a, float b) {
    return subtract(multiply(add(a, b), b), b);
}

#pragma glslify: export(solve)