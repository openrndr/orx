package org.openrndr.extra.shaderphrases.noise

const val taylorInvSqrtPhrase = """#ifndef SP_TAYLORINVSQRT
#define SP_TAYLORINVSQRT    
float taylorInvSqrt(in float r) { return 1.79284291400159 - 0.85373472095314 * r; }
#endif
"""

const val taylorInvSqrtV2Phrase = """#ifndef SP_TAYLORINVSQRTV2
#define SP_TAYLORINVSQRTV2    
vec2 taylorInvSqrt(in vec2 r) { return 1.79284291400159 - 0.85373472095314 * r; }
#endif
"""

const val taylorInvSqrtV3Phrase = """#ifndef SP_TAYLORINVSQRTV3
#define SP_TAYLORINVSQRTV3
vec3 taylorInvSqrt(in vec3 r) { return 1.79284291400159 - 0.85373472095314 * r; }
#endif
"""

const val taylorInvSqrtV4Phrase = """#ifndef SP_TAYLORINVSQRTV4
#define SP_TAYLORINVSQRTV4  
vec4 taylorInvSqrt(in vec4 r) { return 1.79284291400159 - 0.85373472095314 * r; }
#endif
"""