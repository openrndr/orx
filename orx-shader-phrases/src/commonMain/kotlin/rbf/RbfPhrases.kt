package org.openrndr.extra.shaderphrases.rbf

/**
 * A constant string defining a C-style preprocessor directive and implementation for the
 * Radial Basis Function (RBF) Gaussian formula in a shader or computational context.
 *
 * The definition includes a function `rbfGaussian` that computes the Gaussian value
 * based on the squared distance and scale factor. It utilizes the exponential function
 * for the calculation.
 */
const val rbfGaussianPhrase = """#ifndef SP_RBF_GAUSSIAN
#define SP_RBF_GAUSSIAN
float rbfGaussian(float sqrDistance, float scale) {
    return exp(-sqrDistance * scale * scale);
}
#endif
"""

/**
 * A constant string representing a shader function definition for the
 * Radial Basis Function (RBF) using the inverse quadratic formula.
 *
 * The function `rbfInverseQuadratic` calculates the RBF value based on
 * squared distance and a scale factor.
 *
 * The formula for the RBF is:
 *  1.0 / (1.0 + sqrDistance * scale^2)
 */
const val rbfInverseQuadraticPhrase = """#ifndef SP_RBF_INVERSE_QUADRATIC
#define SP_RBF_INVERSE_QUADRATIC
float rbfInverseQuadratic(float sqrDistance, float scale) {
    return 1.0 / (1.0 + sqrDistance * scale * scale);
}
#endif
"""

/**
 * Represents the implementation of the inverse multiquadratic radial basis function (RBF)
 * in shader language. This constant holds the shader source code for calculating
 * the inverse multiquadratic RBF given a squared distance and a scale factor.
 *
 * The function defined within this shader code computes the RBF as:
 * 1.0 / sqrt(1.0 + sqrDistance * scale * scale)
 */
const val rbfInverseMultiQuadraticPhrase = """#ifndef SP_RBF_INVERSE_MULTIQUADRATIC
#define SP_RBF_INVERSE_MULTIQUADRATIC
float rbfInverseMultiQuadratic(float sqrDistance, float scale) {
    return 1.0 / sqrt(1.0 + sqrDistance * scale * scale);
}
#endif
"""