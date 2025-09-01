package org.openrndr.extra.shaderphrases.sdf

import kotlin.math.PI

const val dot2Phrase = """#ifndef SP_DOT2
#define SP_DOT2
float dot2(vec2 v) { return dot(v,v); }
#endif"""

const val opXorPhrase = """#ifndef SP_OPXOR
#define SP_OPXOR
float opXor( float a, float b ) {
    return max( min(a,b), -max(a,b) );
}
#endif    
"""

const val opUnionPhrase = """#ifndef SP_OPUNION
#define SP_OPUNION
float opUnion( float d1, float d2 ){
    return min(d1,d2);
}
#endif"""

const val opIntersectionPhrase = """#ifndef SP_OPINTERSECTION
#define SP_OPINTERSECTION
float opIntersection( float d1, float d2 ){
    return max(d1,d2);
}"""

const val opSubtractionPhrase = """#ifndef SP_OPSUBTRACTION
#define SP_OPSUBTRACTION
float opSubtraction( float d1, float d2 ){
    return max(-d1,d2);
}
"""

const val sdBoxPhrase = """#ifndef SP_SDBOX
#define SP_SDBOX
float sdBox(in vec2 p, in vec2 b) {
    vec2 d = abs(p) - b;
    return length(max(d, 0.0)) + min(max(d.x, d.y), 0.0);
}
#endif
"""

const val sdCirclePhrase = """#ifndef SP_SDCIRCLE
#define SP_SDCIRCLE
float sdCircle(in vec2 p, in float r) {
    return length(p) - r;
}
#endif
"""

const val sdSegmentPhrase = """#ifndef SP_SDSEGMENT
#define SP_SDSEGMENT
float sdSegment(in vec2 p, in vec2 a, in vec2 b) {
    vec2 pa = p - a;
    vec2 ba = b - a;
    float h = clamp( dot(pa,ba)/dot(ba,ba), 0.0, 1.0 );
    return length( pa - ba*h );
}    
#endif
"""

const val sdHeartPhrase = """#ifndef SP_SDHEART
#define SP_SDHEART
$dot2Phrase
float sdHeart(in vec2 p) {
    p.x = abs(p.x);
    if (p.y+p.x>1.0) {
        return sqrt(dot2(p-vec2(0.25,0.75))) - sqrt(2.0)/4.0;
    }
    return sqrt(min(dot2(p-vec2(0.00,1.00)), dot2(p-0.5*max(p.x+p.y,0.0)))) * sign(p.x-p.y);    
}
#endif
"""

const val sdEllipsePhrase = """#ifndef SP_SDELLIPSE
#define SP_SDELLIPSE
float sdEllipse( in vec2 p, in vec2 ab ) {
    p = abs(p); if( p.x > p.y ) {p=p.yx;ab=ab.yx;}
    float l = ab.y*ab.y - ab.x*ab.x;
    float m = ab.x*p.x/l;      float m2 = m*m; 
    float n = ab.y*p.y/l;      float n2 = n*n; 
    float c = (m2+n2-1.0)/3.0; float c3 = c*c*c;
    float q = c3 + m2*n2*2.0;
    float d = c3 + m2*n2;
    float g = m + m*n2;
    float co;
    if( d<0.0 )
    {
        float h = acos(q/c3)/3.0;
        float s = cos(h);
        float t = sin(h)*sqrt(3.0);
        float rx = sqrt( -c*(s + t + 2.0) + m2 );
        float ry = sqrt( -c*(s - t + 2.0) + m2 );
        co = (ry+sign(l)*rx+abs(g)/(rx*ry)- m)/2.0;
    }
    else
    {
        float h = 2.0*m*n*sqrt( d );
        float s = sign(q+h)*pow(abs(q+h), 1.0/3.0);
        float u = sign(q-h)*pow(abs(q-h), 1.0/3.0);
        float rx = -s - u - c*4.0 + 2.0*m2;
        float ry = (s - u)*sqrt(3.0);
        float rm = sqrt( rx*rx + ry*ry );
        co = (ry/sqrt(rm-rx)+2.0*g/rm-m)/2.0;
    }
    vec2 r = ab * vec2(co, sqrt(1.0-co*co));
    return length(r-p) * sign(p.y-r.y);
}
#endif
"""

const val sdStarPhrase = """#ifndef SP_SDSTAR
#define SP_SDSTAR
float sdStar( in vec2 p, in float r, in int n, in float sharpness)
{
    float m = mix(2.0, float(n), sharpness);

    // next 4 lines can be precomputed for a given shape
    float an = ${PI}/float(n);
    float en = ${PI}/m;  // m is between 2 and n
    vec2  acs = vec2(cos(an),sin(an));
    vec2  ecs = vec2(cos(en),sin(en)); // ecs=vec2(0,1) for regular polygon

    float bn = mod(atan(p.x,p.y),2.0*an) - an;
    p = length(p)*vec2(cos(bn),abs(sin(bn)));
    p -= r*acs;
    p += ecs*clamp( -dot(p,ecs), 0.0, r*acs.y/ecs.y);
    return length(p)*sign(p.x);
}
#endif
"""