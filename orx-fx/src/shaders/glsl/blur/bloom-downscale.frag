highp out vec4 o_output;
highp in vec2 v_texCoord0;
uniform highp sampler2D tex0;


// -- based on https://github.com/excess-demogroup/even-laster-engine/blob/a451a89f6bd6d3c6017d5890b92d9f72823bc742/src/shaders/bloom.fra
void main()
{
	float centerWeight = 0.16210282163712664;
	vec2 diagonalOffsets = vec2(0.3842896354828526, 1.2048616327242379);
	vec4 offsets = vec4(-diagonalOffsets.xy, +diagonalOffsets.xy) / vec2(textureSize(tex0, 0)).xyxy;
	float diagonalWeight = 0.2085034734347498;

	o_output = texture(tex0, v_texCoord0) * centerWeight +
	               texture(tex0, v_texCoord0 + offsets.xy) * diagonalWeight +
	               texture(tex0, v_texCoord0 + offsets.wx) * diagonalWeight +
	               texture(tex0, v_texCoord0 + offsets.zw) * diagonalWeight +
	               texture(tex0, v_texCoord0 + offsets.yz) * diagonalWeight;
}