package org.openrndr.extra.fx.blend

import org.openrndr.draw.Filter2to1
import org.openrndr.draw.filterShaderFromCode
import org.openrndr.extra.fx.fx_spectral
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter

private val spectralBlendShader = """// includes    
${fx_spectral}
// filter code
uniform sampler2D tex0;
uniform sampler2D tex1;
in vec2 v_texCoord0;

out vec4 o_color;

uniform bool linearizeInputA;
uniform bool linearizeInputB;
uniform bool delinearizeOutput;
uniform float fill;
uniform bool clip;

vec3 srgb_to_linear(vec3 c) {
    const float t = 0.00313066844250063;
    return vec3(
        c.r <= t ? c.r / 12.92 : pow((c.r + 0.055) / 1.055, 2.4),
        c.g <= t ? c.g / 12.92 : pow((c.g + 0.055) / 1.055, 2.4),
        c.b <= t ? c.b / 12.92 : pow((c.b + 0.055) / 1.055, 2.4));
}

vec3 linear_to_srgb(vec3 c) {
    const float t = 0.00313066844250063;
    return vec3(
        c.r <= t ? c.r * 12.92 : 1.055 * pow(c.r, 1 / 2.4) - 0.055,
        c.g <= t ? c.g * 12.92 : 1.055 * pow(c.g, 1 / 2.4) - 0.055,
        c.b <= t ? c.b * 12.92 : 1.055 * pow(c.b, 1 / 2.4) - 0.055
    );
}

void main() {
    vec4 a = texture(tex0, v_texCoord0);
    vec4 b = texture(tex1, v_texCoord0);
    if (linearizeInputA) {
        a.rgb = srgb_to_linear(a.rgb);
    }
    
    if (linearizeInputB) {
        b.rgb = srgb_to_linear(b.rgb);
    }
    
    // depremultiply alpha
    vec3 na = a.a == 0.0 ? vec3(0.0): a.rgb / a.a;
    vec3 nb = b.a == 0.0 ? vec3(0.0): b.rgb / b.a;

    vec4 mixed = vec4(spectral_mix(na, nb, min(1.0,  b.a * fill) ), min(a.a, b.a));

    // premultiply alpha
    mixed.rgb *= mixed.a;    
    
    if (!clip) {
        vec4 b_over_a = a * (1.0 - b.a) + b;    
        mixed = b_over_a * (1.0-mixed.a)  + mixed;
    }

    if (delinearizeOutput) {
         mixed.rgb = linear_to_srgb(mixed.rgb);
    }

    o_color = mixed;
}
 
"""

/**
 * Blend based on pigment simulation
 */
@Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")
@Description("Blend spectral")
class BlendSpectral : Filter2to1(filterShaderFromCode(spectralBlendShader, "color-burn")) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    @BooleanParameter("linearize input A")
    var linearizeInputA: Boolean by parameters

    @BooleanParameter("linearize input B")
    var linearizeInputB: Boolean by parameters

    @BooleanParameter("delinearize output")
    var delinearizeOutput: Boolean by parameters

    @DoubleParameter("fill", 0.0, 1.0)
    var fill: Double by parameters

    init {
        clip = false
        linearizeInputA = true
        linearizeInputB = true
        delinearizeOutput = true
        fill = 1.0
    }
}