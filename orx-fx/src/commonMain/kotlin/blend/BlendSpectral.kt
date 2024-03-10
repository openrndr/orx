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
        c.r <= t ? c.r * 12.92 : 1.055 * pow(c.r, 1.0 / 2.4) - 0.055,
        c.g <= t ? c.g * 12.92 : 1.055 * pow(c.g, 1.0 / 2.4) - 0.055,
        c.b <= t ? c.b * 12.92 : 1.055 * pow(c.b, 1.0 / 2.4) - 0.055
    );
}

void main() {
    vec4 a = texture(tex0, v_texCoord0);
    vec4 b = texture(tex1, v_texCoord0);
    
    // depremultiply alpha
    vec4 na = a.a == 0.0 ? vec4(0.0): vec4(a.rgb / a.a,a.a);
    vec4 nb = b.a == 0.0 ? vec4(0.0): vec4(b.rgb / b.a,b.a);

    
    if (linearizeInputA) {
        na.rgb = srgb_to_linear(na.rgb);
    }
    
    if (linearizeInputB) {
        nb.rgb = srgb_to_linear(nb.rgb);
    }
    
    vec4 mixed = vec4(spectral_mix(na.rgb, nb.rgb, min(1.0, fill)), 1.0);

    if (!clip) {
        na.rgb *= a.a;
        nb.rgb *= b.a;
        mixed = na * (1.0 - nb.a) + nb * (1.0 - na.a) + mixed * na.a * nb.a; 
    } else {
        mixed = mixed * na.a * nb.a;        
    }

    mixed.rgb = mixed.a == 0.0 ? vec3(0.0): mixed.rgb / mixed.a;

    if (delinearizeOutput) {
         mixed.rgb = linear_to_srgb(mixed.rgb);
    }

// premultiply alpha
    mixed.rgb *= mixed.a;    
    

    o_color = mixed;
}
 
"""

/**
 * Blend based on pigment simulation
 */
@Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")
@Description("Blend spectral")
class BlendSpectral : Filter2to1(filterShaderFromCode(spectralBlendShader, "blend-spectral")) {
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