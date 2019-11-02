#version 330

in vec2 v_texCoord0;
uniform sampler2D tex0;
out vec4 o_output;

void main(){
    vec4 c = texture(tex0, v_texCoord0);
    o_output.rgb = vec3(step(1.0, c.a));
    o_output.a = 1.0;
}