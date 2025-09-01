package org.openrndr.extra.noise.phrases

/**
 * uniform hash shader phrase
 */
@Deprecated("use uhash11Phrase", ReplaceWith("uhash11Phrase", "org.openrndr.extra.shaderphrases.noise.uhash11Phrase"))
val uhash11 = """
#ifndef PHRASE_UHASH11
#define PHRASE_UHASH11
uint uhash11(uint x) {
    uint a = x;
    a = a ^ (a >> 16);
    a *= 0x7feb352du;
    a = a ^ (a >> 15);
    a *= 0x846ca68bu;
    a = a ^ (a >> 16);
    return a;        
}
#endif
"""

/**
 * uniform hash shader phrase
 */
@Deprecated("use fhash11Phrase", ReplaceWith("fhash11Phrase", "org.openrndr.extra.shaderphrases.noise.fhash11Phrase"))
val fhash11 = """
$uhash11    
#ifndef PHRASE_FHASH11
#define PHRASE_FHASH11
float fhash11(float x) {
    uint a = uhash11(floatBitsToUint(x));
    return float(a) / 4294967296.0; 
}
#endif    
"""

/**
 * uniform hash shader phrase
 */
@Deprecated("use uhash12Phrase", ReplaceWith("uhash12Phrase", "org.openrndr.extra.shaderphrases.noise.uhash12Phrase"))
val uhash12 = """
$uhash11    
#ifndef PHRASE_UHASH12
#define PHRASE_UHASH12
uint uhash12(uvec2 x) {
    uint a = uhash11(x.y + uhash11(x.x));
    return a;
}
#endif    
"""

/**
 * uniform hash shader phrase
 */
@Deprecated("use fhash12Phrase", ReplaceWith("fhash12Phrase", "org.openrndr.extra.shaderphrases.noise.fhash12Phrase"))
val fhash12 = """
$uhash12    
#ifndef PHRASE_FHASH12
#define PHRASE_FHASH12
float fhash12(vec2 x) {
    uint a = uhash12(floatBitsToUint(x));
    return float(a) / 4294967296.0; 
}
#endif
"""

/**
 * uniform hash shader phrase
 */
@Deprecated("use uhash13Phrase", ReplaceWith("uhash13Phrase", "org.openrndr.extra.shaderphrases.noise.uhash13Phrase"))
val uhash13 = """
$uhash11    
#ifndef PHRASE_UHASH13
#define PHRASE_UHASH13
uint uhash13(uvec3 x) {
    uint a = uhash11(x.z + uhash11(x.y + uhash11(x.x)));
    return a;
}
#endif    
"""

/**
 * uniform hash shader phrase
 */
@Deprecated("use fhash13Phrase", ReplaceWith("fhash13Phrase", "org.openrndr.extra.shaderphrases.noise.fhash13Phrase"))
val fhash13 = """
$uhash13  
#ifndef PHRASE_FHASH13
#define PHRASE_FHASH13
float fhash13(vec3 x) {
    uint a = uhash13(floatBitsToUint(x));
    return float(a) / 4294967296.0; 
}
#endif
"""

/**
 * uniform hash shader phrase
 */
@Deprecated("use uhash14Phrase", ReplaceWith("uhash14Phrase", "org.openrndr.extra.shaderphrases.noise.uhash14Phrase"))
val uhash14 = """
$uhash11    
#ifndef PHRASE_UHASH14
#define PHRASE_UHASH14
uint uhash14(uvec4 x) {
    uint a = uhash11(x.w + uhash11(x.z + uhash11(x.y + uhash11(x.x))));
    return a;
}
#endif    
"""

/**
 * uniform hash shader phrase
 */
@Deprecated("use fhash14Phrase", ReplaceWith("fhash14Phrase", "org.openrndr.extra.shaderphrases.noise.fhash14Phrase"))
val fhash14 = """
$uhash14  
#ifndef PHRASE_FHASH14
#define PHRASE_FHASH14
float fhash14(vec4 x) {
    uint a = uhash14(floatBitsToUint(x));
    return float(a) / 4294967296.0; 
}
#endif
"""