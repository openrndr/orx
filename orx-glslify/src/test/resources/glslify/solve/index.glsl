#pragma glslify: add = require(sum)
#pragma glslify: multiply = require(multiply)

float solve(float a, float b) {
    return multiply(add(a, b), b);
}

#pragma glslify: export(solve)