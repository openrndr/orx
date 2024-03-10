// from https://github.com/kosua20/Rendu/blob/master/resources/common/shaders/screens/convolution-pyramid/fill-combine.frag

in vec2 v_texCoord0;

uniform sampler2D tex0; // result of pyramid convolution
uniform sampler2D tex1; // input image

out vec4 o_output;

/** Composite the initial image and the filled image in the regions where the initial image is black. */
void main(){

    vec4 inputColor = textureLod(tex1, v_texCoord0, 0.0);
    float mask = 1.0 - inputColor.a;

    vec4 fillColor = textureLod(tex0, v_texCoord0, 0.0);
    fillColor.rgb /= fillColor.a;

    o_output.rgb = fillColor.rgb * (mask) + inputColor.rgb; //mix(inputColor.rgb, fillColor.rgb, mask);
    o_output.a = 1.0;
}