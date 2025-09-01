package org.openrndr.extra.math.rbf

import org.openrndr.extra.math.matrix.Matrix
import org.openrndr.extra.math.matrix.columnMean
import org.openrndr.extra.math.matrix.invertMatrixCholesky
import org.openrndr.extra.math.matrix.minus
import org.openrndr.math.Vector2
import kotlin.math.exp
import kotlin.math.sqrt

typealias Rbf = (Double) -> Double

/**
 * Creates a Gaussian radial basis function (RBF) with the given scale parameter.
 * The resulting RBF computes the exponential decay based on the squared distance scaled by the parameter.
 *
 * @param scale The scale parameter influencing the width of the Gaussian RBF. Smaller values result in a steeper decay.
 * @return A function representing the Gaussian RBF, which takes a square of the distance as input and returns the RBF value.
 */
fun rbfGaussian(scale: Double): Rbf {
    val scale2 = scale * scale
    return { d ->
        exp(-d * scale2)
    }
}

/**
 * Radial basis function (RBF) using the inverse quadratic formula.
 *
 * Creates an RBF that calculates the inverse quadratic function based on the given scale.
 *
 * @param scale A scaling factor that determines the influence range of the RBF.
 * @return A lambda function representing the inverse quadratic RBF.
 */
fun rbfInverseQuadratic(scale: Double): Rbf {
    val scale2 = scale * scale
    return { d ->
        1.0 / (1.0 + d * scale2)
    }
}

/**
 * Generates a radial basis function (RBF) using the inverse multiquadratic kernel.
 *
 * @param scale The scaling factor that influences the spread and shape of the RBF.
 * @return A function representing the inverse multiquadratic RBF, which computes the value
 *         based on the given squared distance.
 */
fun rbfInverseMultiQuadratic(scale: Double): Rbf {
    val scale2 = scale * scale
    return { d ->
        1.0 / sqrt(1.0 + d * scale2)
    }
}

/**
 * A two-dimensional Radial Basis Function (RBF) interpolator.
 *
 * This class provides functionality to interpolate values in a 2D space
 * using Radial Basis Functions (RBFs). It computes interpolated values for
 * input points based on given data points, their corresponding values, and
 * an RBF kernel that defines the basis function.
 *
 * @constructor
 * @param points A list of 2D points representing the locations of the input data.
 * @param weights A 2D array of weights corresponding to each point for each output dimension.
 * @param values A 2D array of known function values at the given points.
 * @param rbf The radial basis function that defines how the influence of each point decreases with distance.
 *            It takes a squared distance as input and returns a scalar value.
 * @param mean The mean values for each output dimension, used to offset the interpolated results.
 */
class Rbf2DInterpolator(
    val points: List<Vector2>,
    val weights: Array<DoubleArray>,
    val values: Array<DoubleArray>,
    val rbf: (Double) -> Double,
    val mean: DoubleArray
) {
    fun interpolate(x: Vector2): DoubleArray {
        val c = DoubleArray(values[0].size)
        for (j in points.indices) {
            val r = rbf(points[j].squaredDistanceTo(x))
            for (i in 0 until c.size) {
                c[i] += weights[j][i] * r
            }
        }
        for (i in 0 until c.size) {
            c[i] += mean[i]
        }
        return c
    }
}


/**
 * Constructs a two-dimensional Radial Basis Function (RBF) interpolator using provided input points,
 * their corresponding values, a smoothing factor, and a radial basis function (RBF) kernel.
 *
 * The interpolator computes a weight matrix derived from the RBF kernel and the supplied data.
 * The resulting interpolator can be used to estimate the values at new locations in a 2D space.
 *
 * @param points A list of 2D points representing the input data locations.
 * @param values A 2D array of known function values corresponding to the input points.
 *               Each row corresponds to a point, and each column corresponds to a value in a specific dimension.
 * @param smoothing A non-negative smoothing factor to reduce interpolation sensitivity. Default is 0.0.
 *                  Larger values result in smoother interpolations.
 * @param rbf The radial basis function used for interpolation. This function takes a squared distance as input
 *            and returns a scalar value representing the influence of points at that distance.
 * @return An instance of `Rbf2DInterpolator` configured with the computed weight matrix and input data.
 */
fun Rbf2DInterpolator(
    points: List<Vector2>,
    values: Array<DoubleArray>,
    smoothing: Double = 0.0,
    rbf: Rbf
): Rbf2DInterpolator {

    val rmat = Matrix(points.size, points.size)
    for (j in points.indices) {
        for (i in points.indices) {
            rmat[i, j] = rbf(points[i].squaredDistanceTo(points[j])) + if (j == i) smoothing else 0.0
        }
    }

    val imat = invertMatrixCholesky(rmat)

    val vmat = Matrix(points.size, values[0].size)
    for (j in points.indices) {
        for (i in values[0].indices) {
            vmat[j, i] = values[j][i]
        }
    }
    val mean = vmat.columnMean()
    val vwmat = vmat - mean

    val wmat = imat * vwmat
    return Rbf2DInterpolator(points, wmat.data, values, rbf, mean.data[0])
}