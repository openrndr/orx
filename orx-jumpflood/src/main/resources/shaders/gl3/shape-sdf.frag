#version 330

in vec2 v_texCoord0;
uniform float iTime;
out vec4 o_color;

uniform bool useUV;
uniform bool rectify;

uniform mat4 modelViewMatrixInverse;

uniform samplerBuffer toBuffer;
uniform samplerBuffer fromBuffer;
uniform int segmentCount;
uniform vec2 targetSize;

uniform sampler2D tex0; // uv-map

float isLeft( vec2 P0, vec2 P1, vec2 P2 ) {
    return ( (P1.x - P0.x) * (P2.y - P0.y)
    - (P2.x -  P0.x) * (P1.y - P0.y) );
}

float length_squared( vec2 v, vec2 w ) {
    return dot(w-v, w-v);
}

int winding_number( vec2 v, vec2 w, vec2 p ) {
    if (v.y <= p.y) {          // start y <= P.y
        if (w.y  > p.y)      // an upward crossing
        if (isLeft( v, w, p) > 0.0)  // P left of  edge
        return 1; // ++wn;            // have  a valid up intersect
    }
    else {                        // start y > P.y (no test needed)
        if (w.y  <= p.y)     // a downward crossing
        if (isLeft( v,w,p) < 0.0)  // P right of  edge
        return -1; //--wn;            // have  a valid down intersect
    }
    return 0;
}

float minimum_distance(vec2 v, vec2 w, vec2 p) {
    // Return minimum distance between line segment vw and point p
    float l2 = length_squared(v, w);  // i.e. |w-v|^2 -  avoid a sqrt
    if (l2 == 0.0) return distance(p, v);   // v == w case
    // Consider the line extending the segment, parameterized as v + t (w - v).
    // We find projection of point p onto the line.
    // It falls where t = [(p-v) . (w-v)] / |w-v|^2
    // We clamp t from [0,1] to handle points outside the segment vw.
    float t = max(0.0, min(1.0, dot(p - v, w - v) / l2));
    vec2 projection = v + t * (w - v);  // Projection falls on the segment
    return distance(p, projection);
}

vec3 minimum_distance_and_perpendicular(vec4 v, vec4 w, vec2 p) {
    // Return minimum distance between line segment vw and point p
    float l2 = length_squared(v.xy, w.xy);  // i.e. |w-v|^2 -  avoid a sqrt
    if (l2 == 0.0) return vec3(distance(p, v.xy), v.z, v.w);   // v == w case
    // Consider the line extending the segment, parameterized as v + t (w - v).
    // We find projection of point p onto the line.
    // It falls where t = [(p-v) . (w-v)] / |w-v|^2
    // We clamp t from [0,1] to handle points outside the segment vw.
    float t = max(0.0, min(1.0, dot(p - v.xy, w.xy - v.xy) / l2));
    vec3 projection = v.xyz + t * (w.xyz - v.xyz);  // Projection falls on the segment
    return vec3(distance(p.xy, projection.xy), projection.z, v.w);
}

float shapeDistance(vec2 uv, out float perpDistOut, out float contourLengthOut ) {
    float mindist = 10E10;
    float perpdist = 0.0;
    float contourLength = 0.0;
    int windingNr = 0;
    for (int i = 0; i < segmentCount; i++) {
        vec4 from = texelFetch(fromBuffer, i);
        vec4 to = texelFetch(toBuffer, i);
        vec3 distline_and_perp = minimum_distance_and_perpendicular(from, to, uv.xy);
        windingNr += winding_number( from.xy, to.xy, uv.xy );
        float distline = distline_and_perp.x;
        if (abs(distline) <= mindist) {
            mindist = distline;
            perpdist = distline_and_perp.y;
            contourLength = distline_and_perp.z;
        }
    }
    float signedDistance = mindist * (windingNr==0 ? 1.0 : -1.0);
    contourLengthOut = contourLength;
    perpDistOut = perpdist;
    return signedDistance;
}

void main() {
    vec2 uv = v_texCoord0;

    vec2 fixDistance = vec2(1.0);

    if (useUV) {
        vec2 o = 0.5 / textureSize(tex0, 0);
        uv = texture(tex0, v_texCoord0 + o).xy;
        if (rectify) {
            fixDistance = (fwidth(uv))*vec2(1280.0, 720.0);
        }
    }
    uv.y = 1.0 - uv.y;
    uv *= targetSize;
    uv = (modelViewMatrixInverse * vec4(uv, 0.0, 1.0)).xy;

    float perpdist;
    float contourLength;
    float signedDistance = shapeDistance(uv, perpdist, contourLength);
    o_color = vec4(signedDistance / length(fixDistance), perpdist/contourLength, contourLength, 1.0);
}