#version 330

out vec4 o_output;
in vec2 v_texCoord0;
uniform sampler2D tex0;


// -- based on https://github.com/excess-demogroup/even-laster-engine/blob/a451a89f6bd6d3c6017d5890b92d9f72823bc742/src/shaders/bloom.fra
void main()
{
	float centerWeight = 0.16210282163712664;
	vec2 diagonalOffsets = vec2(0.3842896354828526, 1.2048616327242379);
	vec4 offsets = vec4(-diagonalOffsets.xy, +diagonalOffsets.xy) / textureSize(tex0, 0).xyxy;
	float diagonalWeight = 0.2085034734347498;

	o_output = textureLod(tex0, v_texCoord0, 0) * centerWeight +
	               textureLod(tex0, v_texCoord0 + offsets.xy, 0) * diagonalWeight +
	               textureLod(tex0, v_texCoord0 + offsets.wx, 0) * diagonalWeight +
	               textureLod(tex0, v_texCoord0 + offsets.zw, 0) * diagonalWeight +
	               textureLod(tex0, v_texCoord0 + offsets.yz, 0) * diagonalWeight;
}