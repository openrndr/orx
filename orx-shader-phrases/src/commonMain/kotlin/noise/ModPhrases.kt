package org.openrndr.extra.shaderphrases.noise

const val mod289Phrase = """#ifndef SP_MOD289
#define SP_MOD289
float mod289(const in float x) { return x - floor(x * (1. / 289.)) * 289.; }
#endif"""

const val mod289V2Phrase = """#ifndef SP_MOD289V2
#define SP_MOD289V2
vec2 mod289(const in vec2 x) { return x - floor(x * (1. / 289.)) * 289.; }
#endif"""

const val mod289V3Phrase = """#ifndef SP_MOD289V3
#define SP_MOD289V3
vec3 mod289(const in vec3 x) { return x - floor(x * (1. / 289.)) * 289.; }
#endif"""

const val mod289V4Phrase = """#ifndef SP_MOD289V4
#define SP_MOD289V4
vec4 mod289(const in vec4 x) { return x - floor(x * (1. / 289.)) * 289.; }
#endif"""