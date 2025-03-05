package org.openrndr.extra.shadestyles.fills.patterns

import org.openrndr.draw.ShadeStyle


class PatternBaseStructure(
    val patternFunction: String,
    val domainWarpFunction: String,
)

class PatternBase(structure: PatternBaseStructure) : ShadeStyle() {

    var patternUnits: Int by Parameter()
    var patternFit: Int by Parameter()


    init {
        fragmentPreamble = """
            ${structure.domainWarpFunction}
            ${structure.patternFunction}
        """.trimIndent()

        fragmentTransform = """vec2 coord = vec2(0.0);
            if (p_patternUnits == 0) { // BOUNDS
                coord = c_boundsPosition.xy;

                if (p_patternFit == 1) { // COVER
                    float mx = max(c_boundsSize.x, c_boundsSize.y);
                    float ar = min(c_boundsSize.x, c_boundsSize.y) / mx;
                    if (c_boundsSize.x == mx) {
                        coord.y = (coord.y - 0.5) * ar + 0.5;   
                    } else {
                        coord.x = (coord.x - 0.5) * ar + 0.5;
                    }
                } else if (p_patternFit == 2) { // CONTAIN
                    float mx = max(c_boundsSize.x, c_boundsSize.y); 
                    float ar = mx / min(c_boundsSize.x, c_boundsSize.y);
                    if (c_boundsSize.y == mx) {
                        coord.y = (coord.y - 0.5) * ar + 0.5;
                    } else {
                        coord.x = (coord.x - 0.5) * ar + 0.5;
                    }
                }                
            } else if (p_patternUnits == 1) { // WORLD
                coord = v_worldPosition.xy;            
            } else if (p_patternUnits == 2) { // VIEW
                coord = v_viewPosition.xy;
            } else if (p_patternUnits == 3) { // SCREEN
                coord = c_screenPosition.xy;
                coord.y = u_viewDimensions.y - coord.y;
            }
            
            vec2 dx = dFdx(coord);
            vec2 dy = dFdy(coord);
            
            int window = p_patternFilterWindow;
            float filterSpread = p_patternFilterSpread; 
            float mask = 0.0;
            for (int v = 0; v < window; v++) {
                for (int u = 0; u < window; u++) {
                    float fv = filterSpread * float(v) / (float(window) - 1.0) - 0.5;
                    float fu = filterSpread * float(u) / (float(window) - 1.0) - 0.5;
                    vec2 scoord = coord + dx * fu + dy * fv;
                    vec2 wcoord = patternDomainWarp(scoord);
                    wcoord = (p_patternTransform * vec4(wcoord, 0.0, 1.0)).xy;
                    mask += clamp(pattern(wcoord * p_patternScale), 0.0, 1.0);
                }
            }
            mask /= (float(window) * float(window));
            
            if (p_patternInvert) {
                mask = 1.0 - mask;
            }
            
            vec4 color = mix(p_patternBackgroundColor, p_patternForegroundColor, mask);
                
            x_fill *= color;
            x_stroke *= color;
        """.trimIndent()
    }
}