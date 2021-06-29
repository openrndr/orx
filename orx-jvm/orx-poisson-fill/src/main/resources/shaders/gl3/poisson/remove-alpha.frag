// from https://github.com/kosua20/Rendu/blob/master/resources/common/shaders/screens/convolution-pyramid/fill-boundary.frag

#version 330

in vec2 v_texCoord0;
uniform sampler2D tex0;

out vec4 o_output;

void main(){
    o_output = vec4(0.0);
    vec4 fullColor = textureLod(tex0, v_texCoord0, 0.0);

    if (fullColor.a == 1.0) {
        o_output = fullColor;
    }

}