package org.openrndr.extra.color.colormatrix

import org.openrndr.color.ColorRGBa
import org.openrndr.math.Matrix55

/**
 * Creates a 5x5 matrix based on the given color values.
 *
 * @param color The color represented as an instance of ColorRGBa, where the r, g, b, and alpha
 * components will be used to modify the matrix.
 * @param ignoreAlpha A boolean flag indicating whether the alpha component of the color should be ignored.
 * If true, the alpha value in the matrix will be set to 0.0. Defaults to true.
 * @return A 5x5 matrix (Matrix55) with the r, g, b components set in the corresponding matrix columns,
 * and the alpha value determined by the ignoreAlpha parameter.
 */
fun constant(color: ColorRGBa, ignoreAlpha: Boolean = true): Matrix55 {
    return Matrix55.IDENTITY.copy(c4r0 = color.r, c4r1 = color.g, c4r2 = color.b,
        c4r3 = if (ignoreAlpha) 0.0 else color.alpha
    )
}

/**
 * Applies a color tint transformation and returns a 5x5 matrix representing the transformation.
 *
 * @param color The `ColorRGBa` instance containing the red, green, blue, and alpha values of the color tint to apply.
 * @return A 5x5 transformation matrix with the color tint applied based on the provided color.
 */
fun tint(color: ColorRGBa): Matrix55 {
    return Matrix55(c0r0 = color.r, c1r1 = color.g, c2r2 = color.b, c3r3 = color.alpha, c4r4 = 1.0)
}

/**
 * A lazily initialized 5x5 matrix (Matrix55) representing a transformation matrix.
 * The matrix is configured with specific coefficient values to perform an inversion transformation.
 */
val invert: Matrix55 by lazy {
    Matrix55(c0r0 = -1.0, c1r1 = -1.0, c2r2 = -1.0, c3r3 = 1.0, c4r0 = 1.0, c4r1 = 1.0, c4r2 = 1.0, c4r3 = 0.0, c4r4 = 1.0)
}

/**
 * Creates a grayscale transformation matrix with the specified red, green, and blue coefficients.
 *
 * @param r The coefficient for the red channel. Default is 0.33.
 * @param g The coefficient for the green channel. Default is 0.33.
 * @param b The coefficient for the blue channel. Default is 0.33.
 * @return A 5x5 matrix representing the grayscale transformation.
 */
fun grayscale(r: Double = 0.33, g: Double = 0.33, b: Double = 0.33): Matrix55 {
    return Matrix55(
            c0r0 = r, c1r0 = g, c2r0 = b,
            c0r1 = r, c1r1 = g, c2r1 = b,
            c0r2 = r, c1r2 = g, c2r2 = b,
            c3r3 = 1.0,
            c4r4 = 1.0)
}

class ColorMatrixBuilder() {
    @PublishedApi
    internal var matrix = Matrix55.IDENTITY

    /**
     * Applies a grayscale transformation to the current color matrix using the specified red, green, and blue coefficients.
     *
     * @param r The coefficient for the red channel. Default is 1.0/3.0.
     * @param g The coefficient for the green channel. Default is 1.0/3.0.
     * @param b The coefficient for the blue channel. Default is 1.0/3.0.
     */
    fun grayscale(r: Double = 1.0/3.0, g: Double = 1.0/3.0, b: Double = 1.0/3.0) {
        matrix *= org.openrndr.extra.color.colormatrix.grayscale(r, g, b)
    }

    /**
     * Adds a constant color transformation to the current color matrix.
     *
     * @param color The color to be added, represented as an instance of `ColorRGBa` with red, green, blue, and alpha components.
     * @param ignoreAlpha A boolean flag indicating whether to ignore the alpha component of the color.
     * If true, the alpha value in the matrix will be set to 0.0. Defaults to true.
     */
    fun addConstant(color: ColorRGBa, ignoreAlpha: Boolean = true) {
        matrix *= org.openrndr.extra.color.colormatrix.constant(color, ignoreAlpha)
    }


    /**
     * Inverts the specified color channels in the current color matrix.
     *
     * @param invertR A boolean indicating whether to invert the red channel. Default is true.
     * @param invertG A boolean indicating whether to invert the green channel. Default is true.
     * @param invertB A boolean indicating whether to invert the blue channel. Default is true.
     */
    fun invert(invertR: Boolean = true, invertG: Boolean = true, invertB: Boolean = true) {
        matrix *= Matrix55(
            c0r0 = if (invertR) -1.0 else 1.0,
            c1r1 = if (invertG) -1.0 else 1.0,
            c2r2 = if (invertB) -1.0 else 1.0,
            c3r3 = 1.0,
            c4r0 = if (invertR) 1.0 else 0.0,
            c4r1 = if (invertG) 1.0 else 0.0,
            c4r2 = if (invertB) 1.0 else 0.0,
            c4r3 = 0.0,
            c4r4 = 1.0
        )
    }
    /**
     * Applies a tint transformation to the color matrix using the specified color.
     *
     * @param color The `ColorRGBa` instance specifying the tint color, including its red, green, blue, and alpha components.
     */
    fun tint(color: ColorRGBa) {
        matrix *= org.openrndr.extra.color.colormatrix.tint(color)
    }

    /**
     * Multiplies the current transformation matrix with the specified 5x5 matrix.
     *
     * @param matrix A 5x5 matrix (Matrix55) to multiply with the current matrix.
     */
    fun multiply(matrix: Matrix55) {
        this.matrix *= matrix
    }

    fun build(): Matrix55 {
        return matrix
    }
}

/**
 * Constructs a 5x5 color transformation matrix using the specified transformations
 * defined within a [ColorMatrixBuilder] DSL.
 *
 * @param builder A lambda function with a receiver of type [ColorMatrixBuilder] used
 * to define the series of color matrix transformations to apply.
 * @return A [Matrix55] instance representing the resulting color transformation matrix
 * after applying all specified operations in the builder.
 */
fun colorMatrix(builder: ColorMatrixBuilder.() -> Unit): Matrix55 {
    return ColorMatrixBuilder().apply(builder).build()
}