package org.openrndr.extra.shaderphrases.noise

val permutePhrase = """#ifndef SP_PERMUTE
#define SP_PERMUTE
$mod289Phrase
float permute(const in float x) { return mod289(((x * 34.0) + 1.0) * x); }
#endif
"""

const val permuteV2Phrase = """#ifndef SP_PERMUTEV2
#define SP_PERMUTEV2    
$mod289V2Phrase
vec2 permute(const in vec2 x) { return mod289(((x * 34.0) + 1.0) * x); }
#endif
"""

const val permuteV3Phrase = """#ifndef SP_PERMUTEV3
#define SP_PERMUTEV3
$mod289V3Phrase
vec3 permute(const in vec3 x) { return mod289(((x * 34.0) + 1.0) * x); }
#endif
"""

const val permuteV4Phrase = """#ifndef SP_PERMUTEV4
#define SP_PERMUTEV4    
$mod289V4Phrase
vec4 permute(const in vec4 x) { return mod289(((x * 34.0) + 1.0) * x); }
#endif
"""