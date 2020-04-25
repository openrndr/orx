#pragma glslify: bounceOut = require(./bounce-out)

float bounceIn(float t) {
  return 1.0 - bounceOut(1.0 - t);
}

#pragma glslify: export(bounceIn)
