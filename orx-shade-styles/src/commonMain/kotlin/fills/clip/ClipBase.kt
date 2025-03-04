package org.openrndr.extra.shadestyles.fills.clip

import org.openrndr.draw.ShadeStyle

class ClipbaseStructure(
    val clipFunction: String,
    val domainWarpFunction: String
)

class ClipBase(structure: ClipbaseStructure) : ShadeStyle() {
    var clipUnits: Int by Parameter()
    var clipFit: Int by Parameter()

    init {
        fragmentPreamble = """
            ${structure.domainWarpFunction}
            ${structure.clipFunction}
        """.trimIndent()

        fragmentTransform = """vec2 coord = vec2(0.0);
            if (p_clipUnits == 0) { // BOUNDS
                coord = c_boundsPosition.xy;

                if (p_clipFit == 1) { // COVER
                    float mx = max(c_boundsSize.x, c_boundsSize.y);
                    float ar = min(c_boundsSize.x, c_boundsSize.y) / mx;
                    if (c_boundsSize.x == mx) {
                        coord.y = (coord.y - 0.5) * ar + 0.5;   
                    } else {
                        coord.x = (coord.x - 0.5) * ar + 0.5;
                    }
                } else if (p_clipFit == 2) { // CONTAIN
                    float mx = max(c_boundsSize.x, c_boundsSize.y); 
                    float ar = mx / min(c_boundsSize.x, c_boundsSize.y);
                    if (c_boundsSize.y == mx) {
                        coord.y = (coord.y - 0.5) * ar + 0.5;
                    } else {
                        coord.x = (coord.x - 0.5) * ar + 0.5;
                    }
                }                
            } else if (p_clipUnits == 1) { // WORLD
                coord = v_worldPosition.xy;            
            } else if (p_clipUnits == 2) { // VIEW
                coord = v_viewPosition.xy;
            } else if (p_clipUnits == 3) { // SCREEN
                coord = c_screenPosition.xy;
                coord.y = u_viewDimensions.y - coord.y;
            }
            coord = clipDomainWarp(coord);
            coord = (p_clipTransform * vec4(coord, 0.0, 1.0)).xy;
            
            float mask = clipMask(coord);
            if (p_clipInvert) {
                mask = -mask;
            }
            
            float maskWidth = abs(fwidth(mask));
            float maskFiltered = clamp(p_clipFloor + 
                p_clipBlend * 
                smoothstep(maskWidth * 0.5, -maskWidth * 0.5 - p_clipFeather, mask - p_clipOuter) *
                smoothstep(-maskWidth * 0.5, maskWidth * 0.5 + p_clipFeather, mask - p_clipInner),
                
                0.0, 1.0);
            x_fill.a *= maskFiltered;
            x_stroke.a *= maskFiltered;
        """.trimIndent()
    }
}