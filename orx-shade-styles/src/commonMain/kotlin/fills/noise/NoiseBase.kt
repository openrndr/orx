package org.openrndr.extra.shadestyles.fills.noise

import org.openrndr.draw.ShadeStyle


class NoiseBase(
    override var parameterValues: MutableMap<String, Any>,
    domainWarpFunction: String,
    noiseFunction: String,
    fbmFunction: String,
    levelWarpFunction: String,
    blendFunction: String
) : ShadeStyle() {
    var fit: Int by Parameter("noiseFit", 0)
    var units: Int by Parameter("noiseUnits", 0)
    var scale: Double by Parameter("noiseScale", 1.0)

    init {
        fragmentPreamble = """
            $noiseFunction
            ${domainWarpFunction.replace("domainWarp(", "noiseDomainWarp(")}
            $fbmFunction
            ${levelWarpFunction.replace("levelWarp(", "noiseLevelWarp(")}
            ${blendFunction.replace("blend(", "noiseBlend(")}
            
        """.trimIndent()

        fragmentTransform = """
            vec3 coord = vec3(0.0);
            if (p_noiseUnits == 0) { // BOUNDS
                coord.xy = c_boundsPosition.xy;
                
                if (p_noiseFit == 0) {
                    if (p_noiseScaleToSize) {
                        coord.xy *= c_boundsSize.xy * 1.0;
                    }
                } else
                
                if (p_noiseFit == 1) { // COVER
                    float mx = max(c_boundsSize.x, c_boundsSize.y);
                    float ar = min(c_boundsSize.x, c_boundsSize.y) / mx;
                    if (c_boundsSize.x == mx) {
                        coord.y = (coord.y - 0.5) * ar + 0.5;
                        if (p_noiseScaleToSize) {
                            coord *= c_boundsSize.x;
                        }
                    } else {
                        coord.x = (coord.x - 0.5) * ar + 0.5;
                        if (p_noiseScaleToSize) {
                            coord *= c_boundsSize.y;
                        }
                    }

                } else if (p_noiseFit == 2) { // CONTAIN
                    float mx = max(c_boundsSize.x, c_boundsSize.y); 
                    float ar = mx / min(c_boundsSize.x, c_boundsSize.y);
                    if (c_boundsSize.y == mx) {
                        coord.y = (coord.y - 0.5) * ar + 0.5;
                        if (p_noiseScaleToSize) {
                            coord *= c_boundsSize.x;
                        }
                    } else {
                        coord.x = (coord.x - 0.5) * ar + 0.5;
                        if (p_noiseScaleToSize) {
                            coord *= c_boundsSize.y;
                        }
                    }
                }                
            } else if (p_noiseUnits == 1) { // WORLD
                coord.xy = v_worldPosition.xy;            
            } else if (p_noiseUnits == 2) { // VIEW
                coord.xy = v_viewPosition.xy;
            } else if (p_noiseUnits == 3) { // SCREEN
                coord.xy = c_screenPosition.xy;
                coord.y = u_viewDimensions.y - coord.y;
            }
            coord.z = p_noisePhase;
            vec3 dx = dFdx(coord);
            vec3 dy = dFdy(coord);
            
            int w = p_noiseFilterWindow;
            
            vec4 filtered = vec4(0.0);
            float filterScale = 1.0 / max(float(w) - 1.0, 1.0);
            for (int y = 0; y < w; y++) {
                float fy = float(y) * filterScale - 0.5;
                for (int x = 0; x < w; x++) {
                    float fx = float(x) * filterScale - 0.5;
                    vec3 scoord = noiseDomainWarp(coord + fx * dx + fy * dy );
                    float level = noiseLevelWarp(scoord, fbm(scoord * p_noiseScale));
                    vec4 blend = noiseBlend(x_fill, level);
                    filtered += blend;
                }
            }
            filtered /= (float(w) * float(w));
            x_fill = filtered;
        """.trimIndent()
    }

}