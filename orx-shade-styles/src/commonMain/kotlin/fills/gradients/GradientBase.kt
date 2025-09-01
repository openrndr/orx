package org.openrndr.extra.shadestyles.fills.gradients

import org.openrndr.color.AlgebraicColor
import org.openrndr.color.ConvertibleToColorRGBa
import org.openrndr.draw.ShadeStyle
import org.openrndr.extra.color.phrases.linearRgbToOklabPhrase
import org.openrndr.extra.color.phrases.oklabToLinearRgbPhrase
import org.openrndr.extra.shadestyles.fills.FillFit
import org.openrndr.extra.shadestyles.fills.FillUnits
import org.openrndr.extra.shadestyles.fills.SpreadMethod
import org.openrndr.extra.shadestyles.generateColorTransform
import org.openrndr.math.CastableToVector4
import org.openrndr.math.Vector4
import kotlin.math.PI
import kotlin.reflect.KClass

class GradientBaseStructure(
    val gradientFunction: String,
    val domainWarpFunction: String,
    val levelWarpFunction: String
)

open class GradientBase<C>(
    colorType: KClass<C>,
    colors: Array<Vector4>,
    points: Array<Double> = Array(colors.size) { it / (colors.size - 1.0) },
    structure: GradientBaseStructure
) : ShadeStyle()
        where C : ConvertibleToColorRGBa, C : AlgebraicColor<C>, C : CastableToVector4 {

    var quantization: Int by Parameter()
    var colors: Array<Vector4> by Parameter()
    var points: Array<Double> by Parameter()
    var spreadMethod: Int by Parameter()
    var fillUnits: Int by Parameter()
    var fillFit: Int by Parameter()
    var resetFill: Boolean by Parameter()

    init {
        this.quantization = 0
        this.colors = colors
        this.points = points
        this.fillUnits = FillUnits.BOUNDS.ordinal
        this.spreadMethod = SpreadMethod.PAD.ordinal
        this.fillFit = FillFit.STRETCH.ordinal
        this.resetFill = false
        fragmentPreamble = """
            |vec4 g_fill;
            |#ifndef SP_ROTATE2D
            |#define SP_ROTATE2D
            |vec2 rotate2D(vec2 x, float angle){
            |   float rad = angle / 180.0 * $PI;
            |   mat2 m = mat2(cos(rad),-sin(rad), sin(rad),cos(rad));
            |   return m * x;
            |}
            |#endif
            |$oklabToLinearRgbPhrase
            |$linearRgbToOklabPhrase
            |${structure.gradientFunction}
            |${structure.domainWarpFunction}
            |${structure.levelWarpFunction}
            |""".trimMargin()

        fragmentTransform = """
            g_fill = x_fill;
            vec2 coord = vec2(0.0);
            
            if (p_fillUnits == 0) { // BOUNDS
                coord = c_boundsPosition.xy;
                if (p_fillFit == 1) { // COVER
                    float mx = max(c_boundsSize.x, c_boundsSize.y);
                    float ar = min(c_boundsSize.x, c_boundsSize.y) / mx;
                    if (c_boundsSize.x == mx) {
                        coord.y = (coord.y - 0.5) * ar + 0.5;   
                    } else {
                        coord.x = (coord.x - 0.5) * ar + 0.5;
                    }

                } else if (p_fillFit == 2) { // CONTAIN
                    float mx = max(c_boundsSize.x, c_boundsSize.y); 
                    float ar = mx / min(c_boundsSize.x, c_boundsSize.y);
                    if (c_boundsSize.y == mx) {
                        coord.y = (coord.y - 0.5) * ar + 0.5;
                    } else {
                        coord.x = (coord.x - 0.5) * ar + 0.5;
                    }
                }                
            } else if (p_fillUnits == 1) { // WORLD
                coord = v_worldPosition.xy;            
            } else if (p_fillUnits == 2) { // VIEW
                coord = v_viewPosition.xy;
            } else if (p_fillUnits == 3) { // SCREEN
                coord = c_screenPosition.xy;
                coord.y = u_viewDimensions.y - coord.y;
            }

            vec4 gradient = vec4(0.0);
            vec2 dx = p_filterSpread * dFdx(coord) / (float(p_filterWindow) + 1.0);
            vec2 dy = p_filterSpread * dFdy(coord) / (float(p_filterWindow) + 1.0);
            
            for (int u = 0; u < p_filterWindow; u++) {
                for (int v = 0; v < p_filterWindow; v++) {

                    float fv = float(v) / (float(p_filterWindow) - 1.0) - 0.5;
                    float fu = float(u) / (float(p_filterWindow) - 1.0) - 0.5;

                    vec2 scoord = coord + dx * float(u) + dy * float(v);
                    vec2 wcoord = domainWarp(scoord);
        
                    float f = gradientFunction(wcoord);
                    f = levelWarp(wcoord, f);
                    
                    if (p_quantization != 0) {
                        f *= float(p_quantization);
                        f = floor(f);
                        f /= float(p_quantization) - 1.0;
                    }
                    
                    float sf;
                    float mf = 0.0; 
                    if (p_spreadMethod == 0) { // PAD
                        sf = clamp(f, 0.0, 1.0);    
                    } else if (p_spreadMethod == 1) { // REFLECT
                        sf = 2.0 * abs(f / 2.0 - floor(f / 2.0 + 0.5));
                    } else if (p_spreadMethod == 2) { // REPEAT
                        sf = mod(f, 1.0);
                        float seam = ceil(f);
                        vec2 d = vec2(dFdx(f), dFdy(f));
                        mf = (f - seam) / length(d*2.0) + seam;
                    }
        
                    int i = 0;
                    while (i < p_points_SIZE - 1 && sf >= p_points[i+1]) { i++; }
                    
                    vec4 color0 = p_colors[i];
                    vec4 color1 = p_colors[(i+1) % p_colors_SIZE]; 
                    
                    float g = (sf - p_points[i]) / (p_points[i+1] - p_points[i]);
                    vec4 m = mix(color0, color1, clamp(g, 0.0, 1.0));
                    ${generateColorTransform(colorType, "m")} 
                    
                    m.rgb *= m.a;
                    gradient += m;
                }
            }
            gradient /= float(p_filterWindow) * float(p_filterWindow); 
            if (gradient.a > 0.0) {
                gradient.rgb /= gradient.a;
            }
            if (p_resetFill) {
                x_fill.rgb = vec3(1.0);
            }
            x_fill *= gradient;
        """
    }
}