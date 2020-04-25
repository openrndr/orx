precision mediump float;

uniform sampler2D uTexture;
varying vec2 vUv;

#pragma glslify: luma = require(../)

void main() {
  vec4 color = texture2D(uTexture, vUv);
  float brightness = luma(color);

  gl_FragColor = vec4(vec3(brightness), 1.0);
}
