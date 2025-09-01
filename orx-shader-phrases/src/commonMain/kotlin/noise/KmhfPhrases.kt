package org.openrndr.extra.shaderphrases.noise

/**
 * Represents a shader phrase implementing Knuth's multiplicative hash function as defined for unsigned integers.
 *
 * The function takes a 32-bit unsigned integer as input and computes a hashed value using a fixed-point scaling factor
 * (2654435789u). This technique is based on the "multiplicative hash" proposed by Donald Knuth, providing an efficient
 * method for hashing without the need for additional libraries or resources.
 *
 * This phrase is wrapped in preprocessor guards to ensure it is only defined once during the shader compilation process.
 */
// knuth's multiplicative hash function (fixed point R1)
const val kmhfPhrase = """#ifndef SP_KMHF
#define SP_KMHF
uint kmhf(uint x) {
    return 0x80000000u + 2654435789u * x;
}
#endif"""

/**
 * Represents a GLSL shader phrase that defines the inverse of Knuth's
 * multiplicative hash function, commonly used in procedural noise generation
 * or random value calculations.
 *
 * The inverseKmhfPhrase` provides a utility function in GLSL to compute
 * the inverse of the multiplicative hash for unsigned integers. It is wrapped
 * within preprocessor guards to ensure the function is only defined once
 * during shader compilation.
 */
// inverse of Knuth's multiplicative hash function (fixed point R1)
const val inverseKmhfPhrase = """#ifndef SP_INVERSE_KMHF
#define SP_INVERSE_KMHF
uint inverseKmhf(uint x) {
    return (x - 0x80000000u) * 827988741u;
}
#endif
"""