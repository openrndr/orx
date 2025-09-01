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

uniform float fill;
uniform bool clip;

void main() {
    vec4 a = texture(tex0, v_texCoord0);
    vec4 b = texture(tex1, v_texCoord0);
    
    // depremultiply alpha
    vec4 na = a.a == 0.0 ? vec4(0.0): vec4(a.rgb / a.a,a.a);
    vec4 nb = b.a == 0.0 ? vec4(0.0): vec4(b.rgb / b.a,b.a);

    
    
    vec4 mixed = vec4(spectral_mix(na.rgb, nb.rgb, min(1.0, fill)), 1.0);

    if (!clip) {
        na.rgb *= a.a;
        nb.rgb *= b.a;
        mixed = na * (1.0 - nb.a) + nb * (1.0 - na.a) + mixed * na.a * nb.a; 
    } else {
        mixed = mixed * na.a * nb.a;        
    }

    mixed.rgb = mixed.a == 0.0 ? vec3(0.0): mixed.rgb / mixed.a;


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